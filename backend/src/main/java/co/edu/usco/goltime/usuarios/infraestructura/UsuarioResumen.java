package co.edu.usco.goltime.usuarios.infraestructura;

import java.time.Instant;

import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/** Representación pública de un usuario en la API. Sin datos sensibles (no hay contraseñas ni se expone el identificador de Google). `perfilCompleto` dice si falta pedirle datos. */
public record UsuarioResumen(
        Long id,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        boolean telefonoVerificado,
        boolean whatsappVinculado,
        String documento,
        Rol rol,
        boolean activo,
        boolean correoVerificado,
        String motivoDesactivacion,
        Instant fechaDesactivacion,
        boolean puedeInscribirEquipos,
        boolean perfilCompleto,
        Instant ultimoLogin,
        Instant creadoEn) {

    public static UsuarioResumen de(Usuario u) {
        return new UsuarioResumen(u.id(), u.nombres(), u.apellidos(), u.correo(), u.telefono(), u.telefonoVerificado(), u.whatsappVinculado(), u.documento(),
                u.rol(), u.activo(), u.correoVerificado(), u.motivoDesactivacion(), u.fechaDesactivacion(),
                u.puedeInscribirEquipos(), u.perfilCompleto(), u.ultimoLogin(), u.creadoEn());
    }
}
