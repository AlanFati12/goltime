package co.edu.usco.goltime.reservas.infraestructura;

import co.edu.usco.goltime.reservas.aplicacion.Relacionados;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.DisponibilidadCancha;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.DisponibilidadDia;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.PrevisualizacionCambioEstado;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.PrevisualizacionReprogramacion;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.ResumenDia;
import co.edu.usco.goltime.reservas.aplicacion.Vistas.VistaReserva;
import co.edu.usco.goltime.reservas.dominio.Reserva;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaCanchaReserva;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaClienteReserva;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaDisponibilidad;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaDisponibilidadCancha;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaHistorial;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaPrevisualizacion;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaPrevisualizacionEstado;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaReserva;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaResumenDia;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaSedeReserva;
import co.edu.usco.goltime.reservas.infraestructura.DtosReservas.RespuestaSlot;

final class MapeadorReservas {

    private MapeadorReservas() {
    }

    static RespuestaReserva aRespuesta(VistaReserva v) {
        Reserva r = v.reserva();
        Relacionados rel = v.relacionados();
        return new RespuestaReserva(r.id(), r.codigo(), r.estado(), r.origen(),
                new RespuestaCanchaReserva(rel.canchaId(), rel.canchaNombre(), rel.modalidad()),
                new RespuestaSedeReserva(rel.sedeId(), rel.sedeNombre()),
                new RespuestaClienteReserva(r.clienteId(), rel.clienteNombre(), rel.clienteCorreo(), rel.clienteTelefono()),
                r.inicio(), r.fin(), r.duracionMin(), r.precio().base(), r.precio().descuento(), r.precio().finalAPagar(),
                r.estadoPago(), r.montoPagado(), r.nombreEquipo(), r.notas(), r.expiraEn(), r.vecesReprogramada(),
                r.cancelacionTardia(), r.rolCancelador(), r.motivoCancelacion(), r.fechaCancelacion(), r.motivoRechazo(),
                v.puedeCancelar(), v.puedeReprogramar(), v.cancelacionSeriaTardia(), r.creadoEn(),
                v.historial() == null ? null : v.historial().stream().map(h -> new RespuestaHistorial(h.id(), h.accion(), h.actorId(),
                        h.actorNombre(), h.actorRol(), h.motivo(), h.datosAnteriores(), h.datosNuevos(), h.fecha())).toList());
    }

    static RespuestaPrevisualizacion aRespuesta(PrevisualizacionReprogramacion p) {
        return new RespuestaPrevisualizacion(p.disponible(), p.precioActual(), p.precioNuevo(), p.diferencia());
    }

    static RespuestaPrevisualizacionEstado aRespuesta(PrevisualizacionCambioEstado p) {
        return new RespuestaPrevisualizacionEstado(p.actual(), p.nuevo(), p.ocuparaHorario(), p.horarioLibre(), p.liberaraHorario(),
                p.cuentaParaPremios(), p.dejaDeContarParaPremios(), p.quedaReembolsoPendiente(), p.seCompletaraSola(), p.expiraraSola(),
                p.avisaAlCliente());
    }

    static RespuestaDisponibilidad aRespuesta(DisponibilidadDia d) {
        return new RespuestaDisponibilidad(d.fecha(), d.canchas().stream().map(MapeadorReservas::aRespuesta).toList());
    }

    private static RespuestaDisponibilidadCancha aRespuesta(DisponibilidadCancha c) {
        return new RespuestaDisponibilidadCancha(c.canchaId(), c.nombre(), c.modalidad(), c.sedeId(), c.sedeNombre(),
                c.duracionBloqueMin(), c.duracionMinMin(), c.duracionMaxMin(), c.cerrado(),
                c.slots().stream().map(s -> new RespuestaSlot(s.inicio(), s.fin(), s.estado().name(), s.precio())).toList());
    }

    static RespuestaResumenDia aRespuesta(ResumenDia r) {
        return new RespuestaResumenDia(r.fecha(), r.horariosLibres());
    }
}
