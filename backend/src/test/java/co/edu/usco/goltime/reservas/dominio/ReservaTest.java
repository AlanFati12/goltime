package co.edu.usco.goltime.reservas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.Rol;

class ReservaTest {

    private static final Instant AHORA = Instant.parse("2026-11-10T14:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-11-14T00:00:00Z");
    private static final Instant FIN = Instant.parse("2026-11-14T01:00:00Z");
    private static final PoliticaReservas POLITICA = PoliticaReservas.porDefecto();
    private static final Actor CLIENTE = new Actor(10L, Rol.CLIENTE);
    private static final Actor ADMIN = new Actor(1L, Rol.ADMIN);

    private Reserva pendiente() {
        return Reserva.solicitar("GT-2026-000001", 10L, 5L, INICIO, FIN, Precio.sinDescuento(100_000), "  Los Cracks ", " ",
                POLITICA.calcularExpiracion(AHORA, INICIO), "clave-1");
    }

    private Reserva confirmada() {
        Reserva r = pendiente();
        r.aprobar(1L, AHORA);
        return r;
    }

    private static CodigoError codigo(Runnable accion) {
        try {
            accion.run();
        } catch (ExcepcionNegocio e) {
            return e.codigo();
        }
        return null;
    }

    // ----- Creación -----

    @Test
    void laSolicitudNacePendienteConLosDatosLimpiosYElPrecioCongelado() {
        Reserva r = pendiente();
        assertThat(r.estado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(r.origen()).isEqualTo(OrigenReserva.WEB);
        assertThat(r.estadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(r.nombreEquipo()).isEqualTo("Los Cracks");
        assertThat(r.notas()).isNull();
        assertThat(r.precio().finalAPagar()).isEqualTo(100_000);
        assertThat(r.duracionMin()).isEqualTo(60);
        assertThat(r.expiraEn()).isEqualTo(AHORA.plusSeconds(12 * 3600));
        assertThat(r.esDe(10L)).isTrue();
        assertThat(r.esDe(11L)).isFalse();
    }

    // ----- Aprobación -----

    @Test
    void elAdminApruebaUnaPendienteYQuedaConfirmada() {
        Reserva r = confirmada();
        assertThat(r.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(r.aprobadaPor()).isEqualTo(1L);
        assertThat(r.expiraEn()).isNull();
    }

    @Test
    void noSeApruebaUnaSolicitudExpiradaNiYaAprobada() {
        Reserva r = pendiente();
        assertThat(codigo(() -> r.aprobar(1L, r.expiraEn().plusSeconds(1)))).isEqualTo(CodigoError.ESTADO_INVALIDO);
        Reserva c = confirmada();
        assertThat(codigo(() -> c.aprobar(1L, AHORA))).isEqualTo(CodigoError.ESTADO_INVALIDO);
    }

    @Test
    void rechazarExigeMotivo() {
        Reserva r = pendiente();
        assertThatThrownBy(() -> r.rechazar(1L, "  ", AHORA)).isInstanceOf(ExcepcionNegocio.class);
        r.rechazar(1L, "Cancha en mantenimiento ese día", AHORA);
        assertThat(r.estado()).isEqualTo(EstadoReserva.RECHAZADA);
        assertThat(r.motivoRechazo()).isEqualTo("Cancha en mantenimiento ese día");
        assertThat(r.estado().liberaHorario()).isTrue();
    }

    // ----- Expiración -----

    @Test
    void soloExpiraUnaPendienteCuyoPlazoYaVencio() {
        Reserva r = pendiente();
        assertThat(codigo(() -> r.expirar(AHORA))).isEqualTo(CodigoError.ESTADO_INVALIDO);
        r.expirar(r.expiraEn());
        assertThat(r.estado()).isEqualTo(EstadoReserva.EXPIRADA);
        assertThat(codigo(() -> confirmada().expirar(AHORA.plusSeconds(999_999)))).isEqualTo(CodigoError.ESTADO_INVALIDO);
    }

    // ----- Cancelación -----

    @Test
    void elClienteCancelaUnaPendienteSinPenalidadYSinMotivo() {
        Reserva r = pendiente();
        r.cancelar(CLIENTE, null, AHORA, POLITICA);
        assertThat(r.estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(r.cancelacionTardia()).isFalse();
        assertThat(r.canceladaPor()).isEqualTo(10L);
        assertThat(r.rolCancelador()).isEqualTo(Rol.CLIENTE);
        assertThat(r.fechaCancelacion()).isEqualTo(AHORA);
    }

    @Test
    void cancelarUnaConfirmadaConAntelacionNoEsTardia() {
        Reserva r = confirmada();
        r.cancelar(CLIENTE, "Se lesionó un jugador", AHORA, POLITICA);
        assertThat(r.cancelacionTardia()).isFalse();
        assertThat(r.motivoCancelacion()).isEqualTo("Se lesionó un jugador");
    }

    @Test
    void cancelarUnaConfirmadaConMenosDeLasHorasMinimasEsTardia() {
        Reserva r = confirmada();
        Instant casiEmpieza = INICIO.minusSeconds(3 * 3600);
        assertThat(r.seriaCancelacionTardia(casiEmpieza, POLITICA)).isTrue();
        r.cancelar(CLIENTE, null, casiEmpieza, POLITICA);
        assertThat(r.estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(r.cancelacionTardia()).isTrue();
    }

    @Test
    void unaPendienteCancelaSinPenalidadAunqueFaltePoco() {
        Reserva r = pendiente();
        r.cancelar(CLIENTE, null, INICIO.minusSeconds(3600), POLITICA);
        assertThat(r.cancelacionTardia()).isFalse();
    }

    @Test
    void elAdminCancelaSiempreConMotivoYNuncaEsTardia() {
        Reserva r = confirmada();
        assertThatThrownBy(() -> r.cancelar(ADMIN, "", AHORA, POLITICA)).isInstanceOf(ExcepcionNegocio.class);
        r.cancelar(ADMIN, "Bloqueo de horario: mantenimiento", INICIO.minusSeconds(1800), POLITICA);
        assertThat(r.cancelacionTardia()).isFalse();
        assertThat(r.rolCancelador()).isEqualTo(Rol.ADMIN);
        assertThat(r.canceladaPor()).isEqualTo(1L);
    }

    @Test
    void noSeCancelaLoQueYaComenzoNiLoQueYaNoEstaActivo() {
        assertThat(codigo(() -> confirmada().cancelar(CLIENTE, null, INICIO.plusSeconds(60), POLITICA))).isEqualTo(CodigoError.ESTADO_INVALIDO);
        Reserva cancelada = pendiente();
        cancelada.cancelar(CLIENTE, null, AHORA, POLITICA);
        assertThat(codigo(() -> cancelada.cancelar(CLIENTE, null, AHORA, POLITICA))).isEqualTo(CodigoError.ESTADO_INVALIDO);
    }

    // ----- Reprogramación -----

    private void reprogramar(Reserva r, Actor actor, Instant ahora) {
        r.reprogramar(actor, 6L, INICIO.plusSeconds(86_400), FIN.plusSeconds(86_400), Precio.sinDescuento(120_000), ahora, POLITICA,
                POLITICA.calcularExpiracion(ahora, INICIO.plusSeconds(86_400)));
    }

    @Test
    void reprogramarCambiaCanchaHorarioYPrecioYCuentaLaReprogramacion() {
        Reserva r = confirmada();
        reprogramar(r, CLIENTE, AHORA);
        assertThat(r.canchaId()).isEqualTo(6L);
        assertThat(r.inicio()).isEqualTo(INICIO.plusSeconds(86_400));
        assertThat(r.precio().finalAPagar()).isEqualTo(120_000);
        assertThat(r.vecesReprogramada()).isEqualTo(1);
        assertThat(r.estado()).as("una confirmada sigue confirmada").isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void reprogramarUnaPendienteRenuevaSuExpiracion() {
        Reserva r = pendiente();
        Instant despues = AHORA.plusSeconds(3600);
        reprogramar(r, CLIENTE, despues);
        assertThat(r.estado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(r.expiraEn()).isEqualTo(despues.plusSeconds(12 * 3600));
    }

    @Test
    void elClienteTieneUnMaximoDeReprogramaciones() {
        Reserva r = confirmada();
        reprogramar(r, CLIENTE, AHORA);
        reprogramar(r, CLIENTE, AHORA);
        assertThat(r.puedeReprogramar(AHORA, POLITICA)).isFalse();
        assertThat(codigo(() -> reprogramar(r, CLIENTE, AHORA))).isEqualTo(CodigoError.REPROGRAMACION_NO_PERMITIDA);
        // el administrador no tiene ese límite
        reprogramar(r, ADMIN, AHORA);
        assertThat(r.vecesReprogramada()).isEqualTo(3);
    }

    @Test
    void elClienteSoloReprogramaConLaAntelacionMinima() {
        Reserva r = confirmada();
        Instant tarde = INICIO.minusSeconds(3 * 3600);
        assertThat(r.puedeReprogramar(tarde, POLITICA)).isFalse();
        assertThat(codigo(() -> reprogramar(r, CLIENTE, tarde))).isEqualTo(CodigoError.REPROGRAMACION_NO_PERMITIDA);
        reprogramar(r, ADMIN, tarde);
        assertThat(r.vecesReprogramada()).isEqualTo(1);
    }

    @Test
    void noSeReprogramaUnaReservaCanceladaNiIniciada() {
        Reserva r = pendiente();
        r.cancelar(CLIENTE, null, AHORA, POLITICA);
        assertThat(codigo(() -> reprogramar(r, CLIENTE, AHORA))).isEqualTo(CodigoError.ESTADO_INVALIDO);
        assertThat(codigo(() -> reprogramar(confirmada(), ADMIN, INICIO.plusSeconds(1)))).isEqualTo(CodigoError.ESTADO_INVALIDO);
    }

    // ----- Cierre del ciclo -----

    @Test
    void completarYMarcarNoAsistio() {
        Reserva r = confirmada();
        assertThat(codigo(() -> r.completar(AHORA))).isEqualTo(CodigoError.ESTADO_INVALIDO);
        assertThat(codigo(() -> r.marcarNoAsistio(AHORA))).isEqualTo(CodigoError.ESTADO_INVALIDO);

        r.completar(FIN);
        assertThat(r.estado()).isEqualTo(EstadoReserva.COMPLETADA);
        // el admin puede corregir una completada por error
        r.marcarNoAsistio(FIN);
        assertThat(r.estado()).isEqualTo(EstadoReserva.NO_ASISTIO);
        assertThat(codigo(() -> pendiente().completar(FIN))).isEqualTo(CodigoError.ESTADO_INVALIDO);
    }

    @Test
    void soloLasPendientesYConfirmadasOcupanElHorario() {
        assertThat(EstadoReserva.PENDIENTE.ocupaHorario()).isTrue();
        assertThat(EstadoReserva.CONFIRMADA.ocupaHorario()).isTrue();
        for (EstadoReserva e : new EstadoReserva[] {EstadoReserva.CANCELADA, EstadoReserva.RECHAZADA, EstadoReserva.EXPIRADA,
                EstadoReserva.COMPLETADA, EstadoReserva.NO_ASISTIO}) {
            assertThat(e.ocupaHorario()).isFalse();
        }
    }
}
