package co.edu.usco.goltime.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.Rol;

class UsuarioTest {

    private static final Instant AHORA = Instant.parse("2026-10-18T10:00:00Z");

    private Usuario cliente() {
        Usuario u = Usuario.nuevoClienteGoogle(" Ana ", "Pérez", "  Ana.Perez@Correo.COM ", "sub-123", AHORA);
        u.actualizarPerfil("Ana", "Pérez", "300 123 4567", null);
        return u;
    }

    @Test
    void laPrimeraEntradaConGoogleNormalizaElCorreoYCreaUnClienteActivoYVerificado() {
        Usuario u = cliente();
        assertThat(u.correo()).isEqualTo("ana.perez@correo.com");
        assertThat(u.nombres()).isEqualTo("Ana");
        assertThat(u.rol()).isEqualTo(Rol.CLIENTE);
        assertThat(u.activo()).isTrue();
        assertThat(u.puedeInscribirEquipos()).isFalse();
        assertThat(u.googleSub()).isEqualTo("sub-123");
        assertThat(u.correoVerificado()).as("Google ya confirmó el correo").isTrue();
        assertThat(u.aceptoTerminosEn()).isEqualTo(AHORA);
    }

    @Test
    void entrarConGoogleNoPideWhatsappPeroSePuedeAgregarDespues() {
        Usuario u = Usuario.nuevoClienteGoogle("Ana", "", "a@b.co", "sub-1", AHORA);
        assertThat(u.telefono()).isNull();
        assertThat(u.apellidos()).as("Google no siempre da apellido").isEmpty();
        u.actualizarPerfil("Ana", "Pérez", "3001234567", null);
        assertThat(u.telefono()).isEqualTo("+573001234567");
    }

    @Test
    void vincularGoogleConfirmaElCorreoYRechazaOtraCuentaDeGoogle() {
        Usuario admin = Usuario.nuevoAdministrador("Root", "Admin", "root@goltime.co", null);
        assertThat(admin.googleSub()).isNull();
        admin.vincularGoogle("sub-root");
        assertThat(admin.googleSub()).isEqualTo("sub-root");
        admin.vincularGoogle("sub-root"); // repetir con la misma cuenta es inofensivo
        assertThatThrownBy(() -> admin.vincularGoogle("sub-otro"))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.codigo()).isEqualTo(CodigoError.CREDENCIALES_INVALIDAS));
    }

    @Test
    void unaCuentaCreadaConElIngresoDePruebaPasaAUnaCuentaRealDeGoogleSinPerderse() {
        Usuario u = Usuario.nuevoClienteGoogle("Ana", "Pérez", "a@b.co", "prueba-a@b.co", AHORA);
        u.vincularGoogle("109876543210987654321");
        assertThat(u.googleSub()).isEqualTo("109876543210987654321");
        // ya vinculada a una cuenta real, no se puede pasar a otra
        assertThatThrownBy(() -> u.vincularGoogle("100000000000000000001")).isInstanceOf(ExcepcionNegocio.class);
    }

    @Test
    void desactivarExigeMotivoEInvalidaLosTokens() {
        Usuario u = cliente();
        assertThatThrownBy(() -> u.desactivar("  ", 1L, AHORA)).isInstanceOf(ExcepcionNegocio.class);

        int antes = u.versionToken();
        u.desactivar("Conducta indebida", 1L, AHORA);

        assertThat(u.activo()).isFalse();
        assertThat(u.motivoDesactivacion()).isEqualTo("Conducta indebida");
        assertThat(u.desactivadoPor()).isEqualTo(1L);
        assertThat(u.fechaDesactivacion()).isEqualTo(AHORA);
        assertThat(u.versionToken()).isEqualTo(antes + 1);
    }

    @Test
    void noSePuedeDesactivarDosVecesNiUnAdministrador() {
        Usuario u = cliente();
        u.desactivar("Motivo", 1L, AHORA);
        assertThatThrownBy(() -> u.desactivar("Otra vez", 1L, AHORA)).isInstanceOf(ExcepcionNegocio.class);

        Usuario admin = Usuario.nuevoAdministrador("Root", "Admin", "root@goltime.co", null);
        assertThatThrownBy(() -> admin.desactivar("No", 1L, AHORA))
                .isInstanceOfSatisfying(ExcepcionNegocio.class, e -> assertThat(e.codigo()).isEqualTo(CodigoError.DATOS_INVALIDOS));
    }

    @Test
    void activarLimpiaLosDatosDeDesactivacion() {
        Usuario u = cliente();
        u.desactivar("Motivo", 1L, AHORA);
        u.activar();
        assertThat(u.activo()).isTrue();
        assertThat(u.motivoDesactivacion()).isNull();
        assertThat(u.desactivadoPor()).isNull();
    }

    @Test
    void soloLosClientesPuedenInscribirEquipos() {
        Usuario admin = Usuario.nuevoAdministrador("Root", "Admin", "root@goltime.co", null);
        assertThatThrownBy(() -> admin.habilitarInscripcionEquipos(true)).isInstanceOf(ExcepcionNegocio.class);

        Usuario u = cliente();
        u.habilitarInscripcionEquipos(true);
        assertThat(u.puedeInscribirEquipos()).isTrue();
    }

    @Test
    void elAdministradorNaceConCorreoVerificado() {
        assertThat(Usuario.nuevoAdministrador("Root", "Admin", "root@goltime.co", null).correoVerificado()).isTrue();
    }

    @Test
    void elClienteGuardaSuWhatsappEnFormatoInternacionalYSinVerificar() {
        Usuario u = cliente();
        assertThat(u.telefono()).isEqualTo("+573001234567");
        assertThat(u.telefonoVerificado()).isFalse();
    }

    @Test
    void elWhatsappEsOpcionalParaTodos() {
        assertThat(Usuario.nuevoClienteGoogle("Ana", "Pérez", "a@b.co", "sub-1", AHORA).telefono()).isNull();
        assertThat(Usuario.nuevoAdministrador("Root", "Admin", "root@goltime.co", " ").telefono()).isNull();
    }

    @Test
    void cambiarElNumeroObligaAVerificarloDeNuevo() {
        Usuario u = cliente();
        u.vincularWhatsappTelefono("+573001234567");

        u.actualizarPerfil("Ana", "Pérez", "+57 300 123 4567", null);
        assertThat(u.telefonoVerificado()).as("mismo número escrito distinto").isTrue();

        u.actualizarPerfil("Ana", "Pérez", "3200000000", null);
        assertThat(u.telefono()).isEqualTo("+573200000000");
        assertThat(u.telefonoVerificado()).isFalse();
    }

    @Test
    void desvincularQuitaElTelefonoVerificadoYElLid() {
        Usuario u = cliente();
        u.vincularWhatsappTelefono("3009998877");
        u.vincularWhatsappLid("164295400784036");
        assertThat(u.telefono()).isEqualTo("+573009998877");
        assertThat(u.whatsappVinculado()).isTrue();

        u.desvincularWhatsapp();
        assertThat(u.telefonoVerificado()).isFalse();
        assertThat(u.whatsappLid()).isNull();
        assertThat(u.whatsappVinculado()).isFalse();
    }
}
