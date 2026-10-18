package co.edu.usco.goltime.torneos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class ProgramacionPartidosIT extends BaseTorneosIT {

    private Map<String, Object> programacion(long cancha, LocalDate dia, String hora, String huella) {
        Map<String, Object> m = new HashMap<>();
        m.put("canchaId", cancha);
        m.put("inicio", a(dia, hora).toString());
        m.put("duracionMin", 60);
        m.put("arbitro", "Carlos Gómez");
        if (huella != null) {
            m.put("huellaConfirmada", huella);
        }
        return m;
    }

    private long reservaDe(long partidoId) {
        return jdbc.queryForObject("SELECT reserva_id FROM partidos WHERE id = ?", Long.class, partidoId);
    }

    /** Liga de 3 equipos con el fixture generado; devuelve [torneoId, ...partidos]. */
    private List<Long> ligaConPartidos() throws Exception {
        return ligaConPartidos(3);
    }

    /** Con 4 equipos, los dos primeros partidos (misma jornada) no comparten equipos. */
    private List<Long> ligaConPartidos(int equipos) throws Exception {
        List<Long> ids = torneoConEquipos(cuerpoTorneo("LIGA"), equipos);
        generarFixture(ids.get(0));
        List<Long> resultado = new java.util.ArrayList<>();
        resultado.add(ids.get(0));
        partidos(ids.get(0)).forEach(p -> resultado.add(idPartido(p)));
        return resultado;
    }

    @Test
    void programarReservaLaCanchaYOcupaElHorario() throws Exception {
        List<Long> liga = ligaConPartidos(4);
        long partido = liga.get(1);
        LocalDate dia = diaHabil(8);

        MvcResult r = poner("/api/v1/admin/partidos/" + partido + "/programacion", admin, programacion(canchaId, dia, "18:00", null));
        esperarEstado(r, 200);
        assertThat((String) leer(r, "$.partido.estado")).isEqualTo("PROGRAMADO");
        assertThat((String) leer(r, "$.partido.cancha")).isEqualTo("Cancha 1");

        long reserva = reservaDe(partido);
        assertThat(estadoDe(reserva)).isEqualTo("CONFIRMADA");
        assertThat(jdbc.queryForObject("SELECT origen FROM reservas WHERE id = ?", String.class, reserva)).isEqualTo("TORNEO");
        assertThat(ocupaciones(reserva)).isEqualTo(1);

        // el horario ya no está disponible para un cliente
        Sesion cliente = nuevoClienteConSesion();
        esperarEstado(solicitar(cliente.token(), canchaId, dia, "18:00", "19:00"), 409);

        // otro partido en la misma cancha y hora: no se cancela, es un error
        MvcResult choque = poner("/api/v1/admin/partidos/" + liga.get(2) + "/programacion", admin, programacion(canchaId, dia, "18:00", null));
        esperarEstado(choque, 409);
        assertThat((String) leer(choque, "$.codigo")).isEqualTo("RESERVA_SOLAPADA");

        // reprogramar libera el horario anterior
        esperarEstado(poner("/api/v1/admin/partidos/" + partido + "/programacion", admin, programacion(canchaId, dia, "20:00", null)), 200);
        assertThat(estadoDe(reserva)).isEqualTo("CANCELADA");
        assertThat(ocupaciones(reserva)).isZero();
        esperarEstado(poner("/api/v1/admin/partidos/" + liga.get(2) + "/programacion", admin, programacion(canchaId, dia, "18:00", null)), 200);

        // quitar la programación libera la cancha
        long nueva = reservaDe(partido);
        esperarEstado(borrar("/api/v1/admin/partidos/" + partido + "/programacion?aplazar=true", admin), 200);
        assertThat(estadoDe(nueva)).isEqualTo("CANCELADA");
        MvcResult aplazado = obtener("/api/v1/admin/partidos/" + partido, admin);
        assertThat((String) leer(aplazado, "$.partido.estado")).isEqualTo("APLAZADO");
    }

    @Test
    void chocarConUnaReservaDeClienteExigeConfirmarLaHuella() throws Exception {
        List<Long> liga = ligaConPartidos();
        long partido = liga.get(1);
        LocalDate dia = diaHabil(9);
        Sesion cliente = nuevoClienteConSesion();
        long reservaCliente = solicitarOk(cliente.token(), canchaId, dia, "18:00", "19:00");

        // sin confirmar: 409 y la reserva del cliente sigue viva
        MvcResult sin = poner("/api/v1/admin/partidos/" + partido + "/programacion", admin, programacion(canchaId, dia, "18:00", null));
        esperarEstado(sin, 409);
        assertThat((String) leer(sin, "$.codigo")).isEqualTo("CONFLICTOS_SIN_CONFIRMAR");
        assertThat(estadoDe(reservaCliente)).isEqualTo("PENDIENTE");

        // la previsualización lista lo que se cancelaría
        MvcResult previa = postear("/api/v1/admin/partidos/" + partido + "/programacion/previsualizar", admin, programacion(canchaId, dia, "18:00", null));
        esperarEstado(previa, 200);
        assertThat((Boolean) leer(previa, "$.requiereConfirmacion")).isTrue();
        assertThat((List<?>) leer(previa, "$.conflictos")).hasSize(1);
        String huella = leer(previa, "$.huella");

        // una huella ajena no sirve
        esperarEstado(poner("/api/v1/admin/partidos/" + partido + "/programacion", admin, programacion(canchaId, dia, "18:00", "huella-falsa")), 409);
        assertThat(estadoDe(reservaCliente)).isEqualTo("PENDIENTE");

        esperarEstado(poner("/api/v1/admin/partidos/" + partido + "/programacion", admin, programacion(canchaId, dia, "18:00", huella)), 200);
        assertThat(estadoDe(reservaCliente)).isEqualTo("CANCELADA");
        assertThat(notificaciones(cliente.id(), "RESERVA_CANCELADA_ADMIN")).isEqualTo(1);
        assertThat(estadoDe(reservaDe(partido))).isEqualTo("CONFIRMADA");
    }

    @Test
    void unEquipoNoJuegaDosPartidosAlMismoTiempoYLaCanchaDebeSerDelTorneo() throws Exception {
        List<Long> liga = ligaConPartidos();
        long otraCancha = crearCancha(admin, sedeId, "Cancha 2");
        LocalDate dia = diaHabil(10);
        esperarEstado(poner("/api/v1/admin/partidos/" + liga.get(1) + "/programacion", admin, programacion(canchaId, dia, "18:00", null)), 200);

        // en una liga de 3 todos los partidos comparten algún equipo con el primero
        MvcResult r = poner("/api/v1/admin/partidos/" + liga.get(2) + "/programacion", admin, programacion(otraCancha, dia, "18:00", null));
        esperarEstado(r, 409);
        assertThat((String) leer(r, "$.codigo")).isEqualTo("EQUIPO_OCUPADO");
        // en un horario distinto sí
        esperarEstado(poner("/api/v1/admin/partidos/" + liga.get(2) + "/programacion", admin, programacion(otraCancha, dia, "19:00", null)), 200);

        // una cancha de otra sede no sirve
        long otraSede = crearSede(admin);
        long ajena = crearCancha(admin, otraSede, "Ajena");
        esperarEstado(poner("/api/v1/admin/partidos/" + liga.get(3) + "/programacion", admin, programacion(ajena, dia, "21:00", null)), 400);
    }

    @Test
    void cancelarElTorneoLiberaLasCanchas() throws Exception {
        List<Long> liga = ligaConPartidos();
        LocalDate dia = diaHabil(11);
        esperarEstado(poner("/api/v1/admin/partidos/" + liga.get(1) + "/programacion", admin, programacion(canchaId, dia, "18:00", null)), 200);
        long reserva = reservaDe(liga.get(1));

        esperarEstado(estado(liga.get(0), "CANCELADO", "Sin patrocinio"), 200);

        assertThat(estadoDe(reserva)).isEqualTo("CANCELADA");
        assertThat(ocupaciones(reserva)).isZero();
        // el horario volvió a estar libre
        Sesion cliente = nuevoClienteConSesion();
        solicitarOk(cliente.token(), canchaId, dia, "18:00", "19:00");
    }

    @Test
    void laAgendaDelAdminMuestraElPartidoComoReservaDeTorneo() throws Exception {
        List<Long> liga = ligaConPartidos();
        LocalDate dia = diaHabil(12);
        esperarEstado(poner("/api/v1/admin/partidos/" + liga.get(1) + "/programacion", admin, programacion(canchaId, dia, "18:00", null)), 200);

        MvcResult r = obtener("/api/v1/admin/agenda?sedeId=" + sedeId + "&desde=" + dia + "&hasta=" + dia, admin);
        esperarEstado(r, 200);
        List<String> origenes = leer(r, "$.elementos[*].origen");
        assertThat(origenes).contains("TORNEO");
    }
}
