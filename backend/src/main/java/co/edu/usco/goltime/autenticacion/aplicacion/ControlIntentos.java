package co.edu.usco.goltime.autenticacion.aplicacion;

/** Puerto: bloqueo temporal tras demasiados intentos de ingreso fallidos. */
public interface ControlIntentos {

    boolean estaBloqueado(String clave);

    void registrarFallo(String clave);

    void limpiar(String clave);
}
