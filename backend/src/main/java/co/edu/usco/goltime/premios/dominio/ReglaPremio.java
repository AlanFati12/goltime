package co.edu.usco.goltime.premios.dominio;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.TipoDescuento;

/** "Cada N reservas completadas, un beneficio": un porcentaje o un monto fijo que el cliente usa en una reserva posterior. */
public record ReglaPremio(Long id, String nombre, String descripcion, int reservasRequeridas, TipoDescuento tipo, int valor, int vigenciaDias, boolean activa) {

    public ReglaPremio {
        nombre = nombre == null ? null : nombre.trim();
        if (nombre == null || nombre.isEmpty() || nombre.length() > 100) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El nombre del premio es obligatorio (máximo 100 caracteres).");
        }
        descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        if (reservasRequeridas < 1) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Se necesita al menos 1 reserva completada para ganar el premio.");
        }
        if (tipo == null) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Indica si el premio es un porcentaje o un monto fijo.");
        }
        tipo.validarValor(valor);
        if (vigenciaDias < 1) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El premio debe durar al menos 1 día.");
        }
    }
}
