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
    BLOQUEO_SOLAPADO(409),
    FUERA_DE_HORARIO(422),
    POLITICA_CANCELACION(422),
    REPROGRAMACION_NO_PERMITIDA(422),
    FUERA_DE_VENTANA(422),
    LIMITE_DE_RESERVAS(422),
    ESTADO_INVALIDO(409),
    EQUIPO_OCUPADO(409),
    YA_EXISTE(409),
    PROMOCION_INVALIDA(422),
    CONFLICTOS_SIN_CONFIRMAR(409),
    CREDENCIALES_INVALIDAS(401),
    USUARIO_DESACTIVADO(403),
    CORREO_NO_VERIFICADO(403),
    CORREO_YA_REGISTRADO(409),
    TELEFONO_YA_REGISTRADO(409),
    TOKEN_INVALIDO(400),
    SESION_INVALIDA(401),
    DEMASIADOS_INTENTOS(429),
    /** El número escribió al bot pero pertenece a una cuenta que aún no lo ha vinculado (ADR 0018). */
    WHATSAPP_SIN_VINCULAR(409),
    /** El administrador apagó el bot de WhatsApp. */
    SERVICIO_NO_DISPONIBLE(503),
    ERROR_INTERNO(500);

    private final int estadoHttp;

    CodigoError(int estadoHttp) {
        this.estadoHttp = estadoHttp;
    }

    public int estadoHttp() {
        return estadoHttp;
    }
}
