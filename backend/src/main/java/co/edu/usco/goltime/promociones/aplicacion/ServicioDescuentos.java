package co.edu.usco.goltime.promociones.aplicacion;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.ZonaHoraria;
import co.edu.usco.goltime.premios.aplicacion.RepositorioPremios.PremioOtorgado;
import co.edu.usco.goltime.premios.aplicacion.ServicioPremios;
import co.edu.usco.goltime.promociones.dominio.Promocion;
import co.edu.usco.goltime.promociones.dominio.Promocion.ContextoReserva;
import co.edu.usco.goltime.reservas.aplicacion.PoliticaDescuentos;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCanchas;
import co.edu.usco.goltime.sedes.aplicacion.ServicioCotizacion;
import co.edu.usco.goltime.reservas.dominio.ReservaLiberadaEvento;

/**
 * Decide el descuento de una reserva. Se consideran las promociones automáticas vigentes, la promoción cuyo código escribió el cliente
 * y el premio de fidelidad que eligió; se aplica <b>solo el de mayor valor</b>. Un código o premio inválido es un error (el cliente
 * espera el descuento); una promoción automática que no aplica simplemente se ignora.
 */
@Service
public class ServicioDescuentos implements PoliticaDescuentos {

    private final RepositorioPromociones promociones;
    private final ServicioPremios premios;
    private final ServicioCotizacion cotizacion;
    private final ServicioCanchas canchas;
    private final Fidelidad fidelidad;

    public ServicioDescuentos(RepositorioPromociones promociones, ServicioPremios premios, ServicioCotizacion cotizacion, ServicioCanchas canchas,
            Fidelidad fidelidad) {
        this.promociones = promociones;
        this.premios = premios;
        this.cotizacion = cotizacion;
        this.canchas = canchas;
        this.fidelidad = fidelidad;
    }

    /** Cómo quedaría el precio de una reserva con el código o premio indicados (o con la mejor promoción automática). */
    public record Previsualizacion(int precioBase, int descuento, int precioFinal, String descripcion, Origen origen) {
    }

    @Transactional(readOnly = true)
    public Previsualizacion previsualizar(long clienteId, long canchaId, Instant inicio, Instant fin, String codigo, Long premioId) {
        int base = (int) cotizacion.cotizar(canchaId, inicio, fin).total();
        long sedeId = canchas.detallePublico(canchaId).cancha().sedeId();
        Optional<Descuento> d = evaluar(new Consulta(clienteId, sedeId, canchaId, inicio, fin, base, codigo, premioId, fidelidad.reservasNoCanceladas(clienteId) == 0));
        return new Previsualizacion(base, d.map(Descuento::monto).orElse(0), base - d.map(Descuento::monto).orElse(0), d.map(Descuento::descripcion).orElse(null),
                d.map(Descuento::origen).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Descuento> evaluar(Consulta c) {
        ContextoReserva contexto = contexto(c);
        List<Descuento> candidatos = new ArrayList<>();

        if (c.codigo() != null && !c.codigo().isBlank()) {
            Promocion porCodigo = promociones.buscarPorCodigo(c.codigo().trim())
                    .orElseThrow(() -> new ExcepcionNegocio(CodigoError.PROMOCION_INVALIDA, "El código de promoción no existe."));
            candidatos.add(comoDescuento(porCodigo, c, contexto));
        }
        for (Promocion automatica : promociones.automaticasVigentes(contexto.fecha())) {
            Optional<Descuento> d = intentar(automatica, c, contexto);
            d.ifPresent(candidatos::add);
        }
        if (c.premioId() != null) {
            PremioOtorgado premio = premios.validarParaUso(c.premioId(), c.clienteId());
            int monto = premio.tipo().calcular(premio.valor(), c.precioBase());
            if (monto <= 0) {
                throw new ExcepcionNegocio(CodigoError.PROMOCION_INVALIDA, "El premio no genera descuento para esta reserva.");
            }
            candidatos.add(new Descuento(monto, Origen.PREMIO, premio.id(), "Premio: " + premio.nombre()));
        }
        return candidatos.stream().max(Comparator.comparingInt(Descuento::monto));
    }

    @Override
    @Transactional
    public void consumir(Descuento d, long reservaId, long clienteId) {
        if (d.origen() == Origen.PREMIO) {
            premios.consumir(d.referenciaId(), clienteId, reservaId);
            return;
        }
        // se bloquea la promoción y se cuentan de nuevo los usos: dos reservas simultáneas no pueden pasarse del cupo
        promociones.bloquear(d.referenciaId());
        Promocion p = promociones.buscar(d.referenciaId()).orElseThrow();
        verificarCupos(p, clienteId);
        promociones.registrarUso(p.id(), reservaId, clienteId, d.monto());
    }

    @EventListener
    @Transactional
    public void alLiberarseUnaReserva(ReservaLiberadaEvento evento) {
        promociones.liberarUsosDeReserva(evento.reservaId());
    }

    // ----- Interno -----

    private Optional<Descuento> intentar(Promocion p, Consulta c, ContextoReserva contexto) {
        if (p.motivoNoAplica(contexto).isPresent() || sinCupo(p, c.clienteId())) {
            return Optional.empty();
        }
        int monto = p.descuentoPara(c.precioBase());
        return monto > 0 ? Optional.of(new Descuento(monto, Origen.PROMOCION, p.id(), "Promoción: " + p.nombre())) : Optional.empty();
    }

    /** Para el código del cliente: cualquier razón por la que no aplica se le explica. */
    private Descuento comoDescuento(Promocion p, Consulta c, ContextoReserva contexto) {
        p.motivoNoAplica(contexto).ifPresent(motivo -> {
            throw new ExcepcionNegocio(CodigoError.PROMOCION_INVALIDA, motivo);
        });
        verificarCupos(p, c.clienteId());
        int monto = p.descuentoPara(c.precioBase());
        if (monto <= 0) {
            throw new ExcepcionNegocio(CodigoError.PROMOCION_INVALIDA, "La promoción no genera descuento para esta reserva.");
        }
        return new Descuento(monto, Origen.PROMOCION, p.id(), "Promoción: " + p.nombre() + (p.codigo() != null ? " (" + p.codigo() + ")" : ""));
    }

    private boolean sinCupo(Promocion p, long clienteId) {
        return (p.usosMaximos() != null && promociones.usosActivos(p.id()) >= p.usosMaximos())
                || (p.usosPorCliente() != null && promociones.usosActivosDeCliente(p.id(), clienteId) >= p.usosPorCliente());
    }

    private void verificarCupos(Promocion p, long clienteId) {
        if (p.usosMaximos() != null && promociones.usosActivos(p.id()) >= p.usosMaximos()) {
            throw new ExcepcionNegocio(CodigoError.PROMOCION_INVALIDA, "La promoción ya agotó sus cupos.");
        }
        if (p.usosPorCliente() != null && promociones.usosActivosDeCliente(p.id(), clienteId) >= p.usosPorCliente()) {
            throw new ExcepcionNegocio(CodigoError.PROMOCION_INVALIDA, "Ya usaste esta promoción el máximo de veces permitido.");
        }
    }

    private ContextoReserva contexto(Consulta c) {
        LocalDateTime inicio = ZonaHoraria.aHoraLocal(c.inicio());
        int minutoInicio = inicio.getHour() * 60 + inicio.getMinute();
        int duracion = (int) ChronoUnit.MINUTES.between(c.inicio(), c.fin());
        return new ContextoReserva(inicio.toLocalDate(), inicio.getDayOfWeek().getValue(), minutoInicio, minutoInicio + duracion, c.sedeId(), c.canchaId(),
                c.primeraReserva());
    }
}
