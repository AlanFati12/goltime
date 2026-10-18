package co.edu.usco.goltime.compartido.dominio;

/**
 * Códigos de error de negocio expuestos en la API. El valor numérico es el estado HTTP asociado.
 * Se amplía a medida que se implementan los módulos.
 */
public enum CodigoError {

    RECURSO_NO_ENCONTRADO(404),
    ACCESO_DENEGADO(403),
    NO_AUTENTICADO(401),
    DATOS_INVALIDOS(400),
    CONFLICTO_DE_VERSION(409),
    RESERVA_SOLAPADA(409),
    FUERA_DE_HORARIO(422),
    POLITICA_CANCELACION(422),
    CONFLICTOS_SIN_CONFIRMAR(409),
    CREDENCIALES_INVALIDAS(401),
    USUARIO_DESACTIVADO(403),
    DEMASIADOS_INTENTOS(429),
    ERROR_INTERNO(500);

    private final int estadoHttp;

    CodigoError(int estadoHttp) {
        this.estadoHttp = estadoHttp;
    }

    public int estadoHttp() {
        return estadoHttp;
    }
}
