package co.edu.usco.goltime.compartido.dominio;

/** Forma de un beneficio: porcentaje del precio o monto fijo en COP. Nunca supera el precio ni es negativo. */
public enum TipoDescuento {
    PORCENTAJE,
    MONTO_FIJO;

    public int calcular(int valor, int precioBase) {
        if (precioBase <= 0 || valor <= 0) {
            return 0;
        }
        long descuento = this == PORCENTAJE ? Math.round(precioBase * Math.min(valor, 100) / 100.0) : valor;
        return (int) Math.min(descuento, precioBase);
    }

    /** Valida que el valor tenga sentido para el tipo (un porcentaje va de 1 a 100). */
    public void validarValor(int valor) {
        if (valor < 1 || (this == PORCENTAJE && valor > 100)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS,
                    this == PORCENTAJE ? "El porcentaje debe estar entre 1 y 100." : "El monto del descuento debe ser mayor que cero.");
        }
    }

    public String texto(int valor) {
        return this == PORCENTAJE ? valor + "%" : FormatoTexto.cop(valor);
    }
}
