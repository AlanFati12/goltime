package co.edu.usco.goltime.autenticacion.aplicacion;

import java.time.Instant;
import java.util.Optional;

/** Puerto de salida: códigos para vincular el WhatsApp y los intentos fallidos de usarlos. */
public interface RepositorioCodigosVinculacion {

    /** Guarda el código (solo su hash); {@code false} si ese hash ya existe (se genera otro). */
    boolean guardar(long usuarioId, String codigoHash, Instant expiraEn);

    /** Dueño de un código sin usar y sin vencer. */
    Optional<Long> usuarioDeVigente(String codigoHash, Instant ahora);

    void marcarUsado(String codigoHash, Instant ahora);

    /** Anula los códigos sin usar de la cuenta, para que solo sirva el último. */
    void anularDeUsuario(long usuarioId, Instant ahora);

    int contarGeneradosDesde(long usuarioId, Instant desde);

    void registrarIntentoFallido(String remitente);

    int contarIntentosFallidosDesde(String remitente, Instant desde);
}
