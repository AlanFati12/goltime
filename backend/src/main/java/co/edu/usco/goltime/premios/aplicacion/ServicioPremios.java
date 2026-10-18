package co.edu.usco.goltime.premios.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.FormatoTexto;
import co.edu.usco.goltime.notificaciones.aplicacion.Notificador;
import co.edu.usco.goltime.premios.aplicacion.RepositorioPremios.PremioOtorgado;
import co.edu.usco.goltime.premios.dominio.CalculadoraPremios;
import co.edu.usco.goltime.premios.dominio.EstadoPremio;
import co.edu.usco.goltime.premios.dominio.ReglaPremio;
import co.edu.usco.goltime.reservas.aplicacion.PoliticaDescuentos.Fidelidad;
import co.edu.usco.goltime.reservas.dominio.CompletadaRevertidaEvento;
import co.edu.usco.goltime.reservas.dominio.ReservaCompletadaEvento;
import co.edu.usco.goltime.reservas.dominio.ReservaLiberadaEvento;

/**
 * Premios de fidelidad. El progreso sale siempre de contar las reservas COMPLETADA del cliente (sin torneos): cada vez que una reserva se
 * completa o se corrige, {@link #evaluarCliente} compara lo ganado con lo otorgado y otorga o revoca la diferencia. Un premio ya usado
 * nunca se revoca.
 */
@Service
public class ServicioPremios {

    private static final String ENTIDAD = "premios";

    private final RepositorioPremios premios;
    private final Fidelidad fidelidad;
    private final Notificador notificador;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;

    public ServicioPremios(RepositorioPremios premios, Fidelidad fidelidad, Notificador notificador, RegistroAuditoria auditoria, Clock reloj) {
        this.premios = premios;
        this.fidelidad = fidelidad;
        this.notificador = notificador;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    public record ProgresoPremio(ReglaPremio regla, int completadas, int enCiclo, int faltan) {
    }

    public record MisPremios(List<ProgresoPremio> progreso, List<PremioOtorgado> premios) {
    }

    // ----- Reglas (administración) -----

    @Transactional
    public ReglaPremio crearRegla(ReglaPremio datos) {
        ReglaPremio guardada = premios.guardarRegla(datos);
        auditoria.registrar("CREAR_REGLA_PREMIO", ENTIDAD, guardada.id(), Map.of("nombre", guardada.nombre(), "cada", guardada.reservasRequeridas()), null);
        return guardada;
    }

    @Transactional
    public ReglaPremio actualizarRegla(long id, ReglaPremio datos) {
        premios.regla(id).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "La regla de premio no existe."));
        ReglaPremio guardada = premios.guardarRegla(new ReglaPremio(id, datos.nombre(), datos.descripcion(), datos.reservasRequeridas(), datos.tipo(),
                datos.valor(), datos.vigenciaDias(), datos.activa()));
        auditoria.registrar("ACTUALIZAR_REGLA_PREMIO", ENTIDAD, id, Map.of("nombre", guardada.nombre()), null);
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<ReglaPremio> reglas(boolean soloActivas) {
        return premios.reglas(soloActivas);
    }

    // ----- Premios otorgados -----

    @Transactional
    public List<PremioOtorgado> listar(Long clienteId, EstadoPremio estado) {
        premios.vencer(reloj.instant());
        return premios.listar(clienteId, estado, 200);
    }

    @Transactional
    public void revocar(long id, String motivo) {
        PremioOtorgado p = premios.buscarBloqueando(id).orElseThrow(() -> new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "El premio no existe."));
        if (p.estado() != EstadoPremio.DISPONIBLE) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Solo se puede revocar un premio disponible.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica el motivo de la revocación.");
        }
        premios.revocar(id, motivo.trim());
        auditoria.registrar("REVOCAR_PREMIO", ENTIDAD, id, Map.of("clienteId", p.clienteId()), motivo.trim());
    }

    // ----- Cliente -----

    @Transactional
    public MisPremios misPremios(long clienteId) {
        premios.vencer(reloj.instant());
        int completadas = fidelidad.reservasCompletadas(clienteId);
        List<ProgresoPremio> progreso = premios.reglas(true).stream().map(r -> new ProgresoPremio(r, completadas,
                CalculadoraPremios.enCiclo(completadas, r.reservasRequeridas()), CalculadoraPremios.faltan(completadas, r.reservasRequeridas()))).toList();
        return new MisPremios(progreso, premios.deCliente(clienteId));
    }

    /** Premio que el cliente puede aplicar ahora: suyo, disponible y sin vencer. */
    @Transactional(readOnly = true)
    public PremioOtorgado validarParaUso(long premioId, long clienteId) {
        PremioOtorgado p = premios.buscar(premioId).filter(x -> x.clienteId() == clienteId).orElseThrow(
                () -> new ExcepcionNegocio(CodigoError.PROMOCION_INVALIDA, "El premio no existe o no es tuyo."));
        if (p.estado() != EstadoPremio.DISPONIBLE || !p.venceEn().isAfter(reloj.instant())) {
            throw new ExcepcionNegocio(CodigoError.PROMOCION_INVALIDA, "Ese premio ya no está disponible.");
        }
        return p;
    }

    @Transactional
    public void consumir(long premioId, long clienteId, long reservaId) {
        validarParaUso(premioId, clienteId);
        if (!premios.marcarUsado(premioId, reservaId)) {
            throw new ExcepcionNegocio(CodigoError.PROMOCION_INVALIDA, "Ese premio ya fue usado.");
        }
    }

    // ----- Fidelidad -----

    /** Otorga o revoca lo que haga falta para que los premios coincidan con las reservas completadas del cliente. */
    @Transactional
    public void evaluarCliente(long clienteId) {
        int completadas = fidelidad.reservasCompletadas(clienteId);
        Instant ahora = reloj.instant();
        for (ReglaPremio regla : premios.reglas(true)) {
            int diferencia = CalculadoraPremios.diferencia(completadas, regla.reservasRequeridas(), premios.contarOtorgados(clienteId, regla.id()));
            for (int i = 0; i < diferencia; i++) {
                PremioOtorgado nuevo = premios.otorgar(clienteId, regla, ahora.plus(Duration.ofDays(regla.vigenciaDias())));
                notificador.notificar(clienteId, "PREMIO_GANADO", "¡Ganaste un premio!",
                        "Por tus reservas completadas ganaste %s (%s de descuento). Úsalo antes del %s.".formatted(regla.nombre(),
                                regla.tipo().texto(regla.valor()), FormatoTexto.fechaHora(nuevo.venceEn())),
                        "premio", nuevo.id());
                auditoria.registrar("OTORGAR_PREMIO", ENTIDAD, nuevo.id(), Map.of("clienteId", clienteId, "regla", regla.nombre()), null);
            }
            if (diferencia < 0) {
                List<PremioOtorgado> disponibles = premios.disponibles(clienteId, regla.id());
                for (int i = 0; i < Math.min(-diferencia, disponibles.size()); i++) {
                    premios.revocar(disponibles.get(i).id(), "Una reserva completada se corrigió y ya no alcanzan las reservas necesarias");
                    auditoria.registrar("REVOCAR_PREMIO", ENTIDAD, disponibles.get(i).id(), Map.of("clienteId", clienteId), "Reserva completada corregida");
                }
            }
        }
    }

    @EventListener
    public void alCompletarse(ReservaCompletadaEvento evento) {
        if (evento.clienteId() != null) {
            evaluarCliente(evento.clienteId());
        }
    }

    @EventListener
    public void alCorregirseUnaCompletada(CompletadaRevertidaEvento evento) {
        if (evento.clienteId() != null) {
            evaluarCliente(evento.clienteId());
        }
    }

    @EventListener
    public void alLiberarseUnaReserva(ReservaLiberadaEvento evento) {
        premios.liberarDeReserva(evento.reservaId(), reloj.instant());
    }
}
