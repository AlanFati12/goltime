package co.edu.usco.goltime.sedes.infraestructura;

import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaCancha;
import co.edu.usco.goltime.sedes.aplicacion.Vistas.VistaSede;
import co.edu.usco.goltime.sedes.dominio.Cancha;
import co.edu.usco.goltime.sedes.dominio.DesglosePrecio;
import co.edu.usco.goltime.sedes.dominio.Festivo;
import co.edu.usco.goltime.sedes.dominio.Foto;
import co.edu.usco.goltime.sedes.dominio.HorarioDia;
import co.edu.usco.goltime.sedes.dominio.HorarioSemanal;
import co.edu.usco.goltime.sedes.dominio.MinutoDelDia;
import co.edu.usco.goltime.sedes.dominio.Tarifa;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaCancha;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaComponente;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaCotizacion;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaFestivo;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaFoto;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaHorarioDia;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaSede;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaTarifa;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.RespuestaTramo;
import co.edu.usco.goltime.sedes.infraestructura.DtosSedes.SolicitudHorarioDia;

/** Conversión entre el dominio y los contratos JSON. */
final class MapeadorSedes {

    static final String RUTA_ARCHIVOS = "/api/v1/publico/archivos/";

    private MapeadorSedes() {
    }

    static RespuestaSede aRespuesta(VistaSede v) {
        return new RespuestaSede(v.sede().id(), v.sede().nombre(), v.sede().direccion(), v.sede().ciudad(),
                v.sede().telefono(), v.sede().whatsapp(), v.sede().descripcion(), v.sede().servicios(),
                v.sede().latitud(), v.sede().longitud(), v.sede().activa(), horario(v.sede().horario().dias()),
                fotos(v.fotos()), v.canchas().stream().map(MapeadorSedes::aRespuesta).toList());
    }

    static RespuestaCancha aRespuesta(VistaCancha v) {
        Cancha c = v.cancha();
        List<RespuestaComponente> componentes = v.componentes().stream()
                .map(x -> new RespuestaComponente(x.id(), x.nombre(), x.modalidad())).toList();
        return new RespuestaCancha(c.id(), c.sedeId(), v.sede().nombre(), c.nombre(), c.modalidad(), c.jugadoresPorEquipo(),
                c.capacidadTotal(), c.superficie(), c.largoM(), c.anchoM(), c.techada(), c.iluminacion(), c.precioBaseHora(),
                c.duracionBloqueMin(), c.duracionMinMin(), c.duracionMaxMin(), c.estado(), c.descripcion(), c.orden(),
                c.esCompuesta(), componentes, fotos(v.fotos()), v.tarifas().stream().map(MapeadorSedes::aRespuesta).toList(),
                horario(v.horarioEfectivo()), c.horarioPropio() == null ? List.of() : horario(c.horarioPropio().dias()));
    }

    static RespuestaTarifa aRespuesta(Tarifa t) {
        List<Integer> dias = t.diasSemana().stream().map(DayOfWeek::getValue).sorted().toList();
        return new RespuestaTarifa(t.id(), t.canchaId(), dias, MinutoDelDia.formatear(t.minutoInicio()),
                MinutoDelDia.formatear(t.minutoFin()), t.aplicaFestivo(), t.precioHora(), t.vigenteDesde(), t.vigenteHasta());
    }

    static RespuestaCotizacion aRespuesta(DesglosePrecio d) {
        return new RespuestaCotizacion(d.total(), d.tramos().stream()
                .map(t -> new RespuestaTramo(t.inicio(), t.fin(), t.minutos(), t.precioHora(), t.origen().name())).toList());
    }

    static RespuestaFestivo aRespuesta(Festivo f) {
        return new RespuestaFestivo(f.id(), f.fecha(), f.nombre());
    }

    static List<RespuestaFoto> fotos(List<Foto> fotos) {
        return fotos.stream().map(f -> new RespuestaFoto(f.id(), RUTA_ARCHIVOS + f.archivo(), f.orden())).toList();
    }

    static List<RespuestaHorarioDia> horario(List<HorarioDia> dias) {
        return dias.stream().sorted(Comparator.comparingInt(HorarioDia::diaSemana))
                .map(d -> new RespuestaHorarioDia(d.diaSemana(), d.cerrado(),
                        d.cerrado() ? null : MinutoDelDia.formatear(d.minutoApertura()),
                        d.cerrado() ? null : MinutoDelDia.formatear(d.minutoCierre()))).toList();
    }

    static HorarioSemanal aHorario(List<SolicitudHorarioDia> dias) {
        return new HorarioSemanal(dias.stream().map(d -> d.cerrado() ? HorarioDia.cerradoEl(d.diaSemana())
                : HorarioDia.abierto(d.diaSemana(), d.apertura(), d.cierre())).toList());
    }

    static Set<DayOfWeek> aDias(Set<Integer> dias) {
        return dias == null ? Set.of() : dias.stream().map(DayOfWeek::of).collect(Collectors.toSet());
    }
}
