package co.edu.usco.goltime.autenticacion.aplicacion;

import java.time.Instant;
import java.util.List;

import co.edu.usco.goltime.autenticacion.dominio.VerificacionTelefono;

/** Puerto de salida: códigos de verificación del número de WhatsApp. */
public interface RepositorioVerificacionesTelefono {

    VerificacionTelefono guardar(VerificacionTelefono verificacion);

    /** Códigos sin usar de ese número (los vencidos o agotados los descarta el dominio). */
    List<VerificacionTelefono> pendientesDe(String telefono);

    /** Cuántos códigos se pidieron para ese número desde {@code desde} (límite contra abusos). */
    int contarCreadasDesde(String telefono, Instant desde);

    /** Anula los códigos sin usar de esa cuenta y ese número, para que solo sirva el último. */
    void anularPendientes(long usuarioId, String telefono, Instant ahora);

    List<VerificacionTelefono> pendientesDeLid(String whatsappLid);

    int contarCreadasDesdeLid(String whatsappLid, Instant desde);

    /** Cuántos códigos se enviaron al correo de esa cuenta (para no llenarle la bandeja). */
    int contarCreadasDesdeUsuario(long usuarioId, Instant desde);

    void anularPendientesDeLid(long usuarioId, String whatsappLid, Instant ahora);
}
