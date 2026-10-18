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
        return Usuario.nuevoCliente(" Ana ", "Pérez", "  Ana.Perez@Correo.COM ", "300", null, "hash", AHORA);
    }

    @Test
    void elRegistroNormalizaElCorreoYCreaUnClienteActivo() {
        Usuario u = cliente();
        assertThat(u.correo()).isEqualTo("ana.perez@correo.com");
        assertThat(u.nombres()).isEqualTo("Ana");
        assertThat(u.rol()).isEqualTo(Rol.CLIENTE);
        assertThat(u.activo()).isTrue();
        assertThat(u.puedeInscribirEquipos()).isFalse();
    }

    @Test
    void exigeAceptarLosTerminos() {
        assertThatThrownBy(() -> Usuario.nuevoCliente("Ana", "Pérez", "a@b.co", "300", null, "hash", null))
                .isInstanceOf(ExcepcionNegocio.class);
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

        Usuario admin = Usuario.nuevoAdministrador("Root", "Admin", "root@goltime.co", null, "hash");
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
    void cambiarClaveIncrementaLaVersionDelToken() {
        Usuario u = cliente();
        u.cambiarClave("nuevoHash");
        assertThat(u.claveHash()).isEqualTo("nuevoHash");
        assertThat(u.versionToken()).isEqualTo(1);
    }

    @Test
    void soloLosClientesPuedenInscribirEquipos() {
        Usuario admin = Usuario.nuevoAdministrador("Root", "Admin", "root@goltime.co", null, "hash");
        assertThatThrownBy(() -> admin.habilitarInscripcionEquipos(true)).isInstanceOf(ExcepcionNegocio.class);

        Usuario u = cliente();
        u.habilitarInscripcionEquipos(true);
        assertThat(u.puedeInscribirEquipos()).isTrue();
    }

    @Test
    void elAdministradorNaceConCorreoVerificado() {
        assertThat(Usuario.nuevoAdministrador("Root", "Admin", "root@goltime.co", null, "hash").correoVerificado()).isTrue();
    }
}
