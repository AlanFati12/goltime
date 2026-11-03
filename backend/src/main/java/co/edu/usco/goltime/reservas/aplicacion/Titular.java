package co.edu.usco.goltime.reservas.aplicacion;

import co.edu.usco.goltime.compartido.dominio.Actor;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.reservas.dominio.Reserva;

/**
 * Dueño de las reservas cuando actúa por WhatsApp (ADR 0018): un cliente registrado con su número verificado
 * o un cliente sin cuenta (de mostrador) identificado por su número. Solo puede tocar sus propias reservas.
 */
public record Titular(Long clienteId, Long clienteMostradorId) {

    public Titular {
        if ((clienteId == null) == (clienteMostradorId == null)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica un cliente registrado o uno sin cuenta.");
        }
    }

    public static Titular cliente(long clienteId) {
        return new Titular(clienteId, null);
    }

    public static Titular sinCuenta(long clienteMostradorId) {
        return new Titular(null, clienteMostradorId);
    }

    public boolean tieneCuenta() {
        return clienteId != null;
    }

    /** Actúa con las reglas del cliente; sin cuenta no hay usuario que registrar como actor. */
    public Actor actor() {
        return new Actor(clienteId, Rol.CLIENTE);
    }

    public boolean esDuenoDe(Reserva r) {
        return tieneCuenta() ? r.esDe(clienteId) : r.esDeClienteSinCuenta(clienteMostradorId);
    }
}
