package co.edu.usco.goltime.premios.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import co.edu.usco.goltime.compartido.dominio.TipoDescuento;
import co.edu.usco.goltime.premios.dominio.EstadoPremio;
import co.edu.usco.goltime.premios.dominio.ReglaPremio;

/** Reglas de premio y premios ganados por los clientes. */
public interface RepositorioPremios {

    /** Premio ganado: copia el beneficio de la regla en el momento de otorgarlo. */
    record PremioOtorgado(long id, long clienteId, long reglaId, String nombre, TipoDescuento tipo, int valor, EstadoPremio estado, Instant otorgadoEn,
            Instant venceEn, Long reservaUsoId, String motivoRevocacion) {
    }

    ReglaPremio guardarRegla(ReglaPremio regla);

    Optional<ReglaPremio> regla(long id);

    List<ReglaPremio> reglas(boolean soloActivas);

    PremioOtorgado otorgar(long clienteId, ReglaPremio regla, Instant venceEn);

    /** Premios que cuentan contra la regla: todos menos los revocados. */
    int contarOtorgados(long clienteId, long reglaId);

    /** Premios DISPONIBLE del cliente para esa regla, del más reciente al más antiguo (los primeros en revocarse). */
    List<PremioOtorgado> disponibles(long clienteId, long reglaId);

    Optional<PremioOtorgado> buscar(long id);

    Optional<PremioOtorgado> buscarBloqueando(long id);

    void revocar(long id, String motivo);

    List<PremioOtorgado> deCliente(long clienteId);

    List<PremioOtorgado> listar(Long clienteId, EstadoPremio estado, int limite);

    /** Pasa a USADO un premio DISPONIBLE; devuelve false si ya no lo estaba. */
    boolean marcarUsado(long id, long reservaId);

    /** Devuelve a DISPONIBLE (o VENCIDO si ya pasó su fecha) el premio que usaba una reserva que se liberó. */
    void liberarDeReserva(long reservaId, Instant ahora);

    /** Marca VENCIDO los DISPONIBLE cuya fecha ya pasó. */
    int vencer(Instant ahora);
}
