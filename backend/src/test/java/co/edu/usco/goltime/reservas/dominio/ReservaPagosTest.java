package co.edu.usco.goltime.reservas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.Rol;

class ReservaPagosTest {

    private static final Instant AHORA = Instant.parse("2026-11-10T14:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-11-14T00:00:00Z");
    private static final Instant FIN = Instant.parse("2026-11-14T01:00:00Z");
    private static final PoliticaReservas POLITICA = PoliticaReservas.porDefecto();
    private static final Actor ADMIN = new Actor(1L, Rol.ADMIN);

    private Reserva confirmada(int precio) {
        return Reserva.crearPorAdmin("GT-2026-000009", 10L, null, 5L, INICIO, FIN, Precio.sinDescuento(precio), "Equipo", null, 1L, AHORA);
    }

    private static CodigoError codigo(Runnable accion) {
        try {
            accion.run();
        } catch (ExcepcionNegocio e) {
            return e.codigo();
        }
        return null;
    }

    // ----- Creación por el admin -----

    @Test
    void unaReservaCreadaPorElAdminNaceConfirmadaConOrigenAdmin() {
        Reserva r = confirmada(80_000);
        assertThat(r.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(r.origen()).isEqualTo(OrigenReserva.ADMIN);
        assertThat(r.aprobadaPor()).isEqualTo(1L);
        assertThat(r.expiraEn()).isNull();
        assertThat(r.estadoPago()).isEqualTo(EstadoPago.PENDIENTE);
    }

    @Test
    void puedeSerDeUnClienteRegistradoODeMostradorPeroNuncaDeAmbosNiDeNinguno() {
        Reserva mostrador = Reserva.crearPorAdmin("GT-1", null, 3L, 5L, INICIO, FIN, Precio.sinDescuento(1), null, null, 1L, AHORA);
        assertThat(mostrador.clienteMostradorId()).isEqualTo(3L);
        assertThat(mostrador.clienteId()).isNull();
        assertThat(mostrador.esDe(10L)).isFalse();
        assertThatThrownBy(() -> Reserva.crearPorAdmin("GT-2", 10L, 3L, 5L, INICIO, FIN, Precio.sinDescuento(1), null, null, 1L, AHORA))
                .isInstanceOf(ExcepcionNegocio.class);
        assertThatThrownBy(() -> Reserva.crearPorAdmin("GT-3", null, null, 5L, INICIO, FIN, Precio.sinDescuento(1), null, null, 1L, AHORA))
                .isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void laPoliticaDelAdminNoTieneAntelacionMinimaNiLimiteDeReservas() {
        PoliticaReservas admin = POLITICA.paraAdministrador();
        assertThat(admin.antelacionMinimaHoras()).isNegative();
        assertThat(admin.maxReservasActivasCliente()).isEqualTo(Integer.MAX_VALUE);
        assertThat(admin.antelacionMaximaDias()).isGreaterThan(POLITICA.antelacionMaximaDias());
        assertThat(admin.maxReprogramaciones()).isEqualTo(POLITICA.maxReprogramaciones());
    }

    // ----- Pagos -----

    @Test
    void unAbonoDejaLaReservaAbonadaYElPagoQueCompletaElValorLaDejaPagada() {
        Reserva r = confirmada(100_000);
        assertThat(r.aplicarPago(40_000)).isEqualTo(TipoPago.ABONO);
        assertThat(r.estadoPago()).isEqualTo(EstadoPago.ABONADO);
        assertThat(r.montoPagado()).isEqualTo(40_000);
        assertThat(r.saldoPendiente()).isEqualTo(60_000);

        assertThat(r.aplicarPago(60_000)).isEqualTo(TipoPago.PAGO);
        assertThat(r.estadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(r.saldoPendiente()).isZero();
    }

    @Test
    void noSePagaMasDelSaldoNiMontosInvalidosNiEnReservasInactivas() {
        Reserva r = confirmada(100_000);
        r.aplicarPago(90_000);
        assertThat(codigo(() -> r.aplicarPago(10_001))).isEqualTo(CodigoError.DATOS_INVALIDOS);
        assertThat(codigo(() -> r.aplicarPago(0))).isEqualTo(CodigoError.DATOS_INVALIDOS);
        assertThat(codigo(() -> r.aplicarPago(-5))).isEqualTo(CodigoError.DATOS_INVALIDOS);
        assertThat(r.montoPagado()).isEqualTo(90_000);

        Reserva pendiente = Reserva.solicitar("GT-4", 10L, 5L, INICIO, FIN, Precio.sinDescuento(100_000), null, null, INICIO, null);
        assertThat(codigo(() -> pendiente.aplicarPago(1_000))).isEqualTo(CodigoError.ESTADO_INVALIDO);
    }

    @Test
    void sePuedePagarUnaReservaYaJugada() {
        Reserva r = confirmada(100_000);
        r.completar(FIN);
        assertThat(r.aplicarPago(100_000)).isEqualTo(TipoPago.PAGO);
        assertThat(r.estadoPago()).isEqualTo(EstadoPago.PAGADO);
    }

    // ----- Reembolsos -----

    @Test
    void cancelarUnaReservaConDineroPagadoDejaUnReembolsoPendiente() {
        Reserva r = confirmada(100_000);
        r.aplicarPago(50_000);
        r.cancelar(ADMIN, "Bloqueo de horario: mantenimiento", AHORA, POLITICA);
        assertThat(r.estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(r.estadoPago()).isEqualTo(EstadoPago.REEMBOLSO_PENDIENTE);
        assertThat(r.montoPagado()).isEqualTo(50_000);
    }

    @Test
    void cancelarUnaReservaSinPagosNoGeneraReembolso() {
        Reserva r = confirmada(100_000);
        r.cancelar(ADMIN, "Motivo", AHORA, POLITICA);
        assertThat(r.estadoPago()).isEqualTo(EstadoPago.PENDIENTE);
    }

    @Test
    void elReembolsoTotalLaDejaReembolsadaYElParcialSigueAbierto() {
        Reserva r = confirmada(100_000);
        r.aplicarPago(100_000);
        r.cancelar(ADMIN, "Motivo", AHORA, POLITICA);

        r.aplicarReembolso(30_000, false);
        assertThat(r.estadoPago()).isEqualTo(EstadoPago.REEMBOLSO_PENDIENTE);
        assertThat(r.montoPagado()).isEqualTo(70_000);

        r.aplicarReembolso(70_000, false);
        assertThat(r.estadoPago()).isEqualTo(EstadoPago.REEMBOLSADO);
        assertThat(r.montoPagado()).isZero();
    }

    @Test
    void conCerrarSeSaldaElReembolsoRetenidoUnaParte() {
        Reserva r = confirmada(100_000);
        r.aplicarPago(100_000);
        r.cancelar(new Actor(10L, Rol.CLIENTE), null, INICIO.minusSeconds(3600), POLITICA);
        assertThat(r.cancelacionTardia()).isTrue();

        r.aplicarReembolso(60_000, true); // se retienen 40.000 por cancelación tardía
        assertThat(r.estadoPago()).isEqualTo(EstadoPago.REEMBOLSADO);
        assertThat(r.montoPagado()).isEqualTo(40_000);
    }

    @Test
    void soloSeReembolsaSiHayReembolsoPendienteYSinPasarseDeLoPagado() {
        Reserva r = confirmada(100_000);
        r.aplicarPago(50_000);
        assertThat(codigo(() -> r.aplicarReembolso(10_000, false))).isEqualTo(CodigoError.ESTADO_INVALIDO);
        r.cancelar(ADMIN, "Motivo", AHORA, POLITICA);
        assertThat(codigo(() -> r.aplicarReembolso(50_001, false))).isEqualTo(CodigoError.DATOS_INVALIDOS);
        assertThat(codigo(() -> r.aplicarReembolso(0, false))).isEqualTo(CodigoError.DATOS_INVALIDOS);
        assertThat(r.montoPagado()).isEqualTo(50_000);
    }

    @Test
    void rechazarOExpirarUnaSolicitudConAbonoTambienDejaReembolsoPendiente() {
        Reserva r = Reserva.reconstruir(1L, "GT-5", 10L, null, 5L, INICIO, FIN, EstadoReserva.PENDIENTE, OrigenReserva.WEB,
                Precio.sinDescuento(100_000), EstadoPago.ABONADO, 20_000, null, null, null, null, null, null, false, 0, null, null,
                null, AHORA, null, null, AHORA);
        r.expirar(AHORA);
        assertThat(r.estadoPago()).isEqualTo(EstadoPago.REEMBOLSO_PENDIENTE);
    }

    // ----- Reprogramación con pagos -----

    @Test
    void alReprogramarAUnPrecioMayorUnaReservaPagadaPasaAAbonada() {
        Reserva r = confirmada(80_000);
        r.aplicarPago(80_000);
        assertThat(r.estadoPago()).isEqualTo(EstadoPago.PAGADO);
        r.reprogramar(ADMIN, 5L, INICIO.plusSeconds(86_400), FIN.plusSeconds(86_400), Precio.sinDescuento(100_000), AHORA, POLITICA, null);
        assertThat(r.estadoPago()).isEqualTo(EstadoPago.ABONADO);
        assertThat(r.saldoPendiente()).isEqualTo(20_000);
    }
}
