package co.edu.usco.goltime.compartido.infraestructura.web;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import jakarta.validation.ConstraintViolationException;

/**
 * Traduce excepciones a {@link ProblemDetail} (RFC 7807). Todas las respuestas de error llevan la
 * propiedad {@code codigo} (ver {@link CodigoError}) para que el frontend muestre mensajes amigables.
 * Hereda de {@link ResponseEntityExceptionHandler}, que ya cubre las excepciones estándar de Spring MVC.
 */
@RestControllerAdvice
public class ManejadorErroresGlobal extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresGlobal.class);

    @ExceptionHandler(ExcepcionNegocio.class)
    ResponseEntity<ProblemDetail> alManejarNegocio(ExcepcionNegocio ex) {
        return responder(ex.codigo(), ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> alValidarParametros(ConstraintViolationException ex) {
        List<CampoInvalido> campos = ex.getConstraintViolations().stream()
                .map(v -> new CampoInvalido(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        ResponseEntity<ProblemDetail> respuesta = responder(CodigoError.DATOS_INVALIDOS,
                "Hay datos inválidos en la solicitud.");
        respuesta.getBody().setProperty("campos", campos);
        return respuesta;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> alHaberConflictoDeVersion(OptimisticLockingFailureException ex) {
        return responder(CodigoError.CONFLICTO_DE_VERSION,
                "El registro fue modificado por otra persona. Recarga e intenta de nuevo.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> alDenegarAcceso(AccessDeniedException ex) {
        return responder(CodigoError.ACCESO_DENEGADO, "No tienes permisos para realizar esta acción.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> alFallarInesperadamente(Exception ex) {
        log.error("Error inesperado", ex);
        return responder(CodigoError.ERROR_INTERNO, "Ocurrió un error inesperado. Intenta más tarde.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new CampoInvalido(e.getField(), e.getDefaultMessage()))
                .toList();
        ProblemDetail problema = problema(CodigoError.DATOS_INVALIDOS, "Hay datos inválidos en la solicitud.");
        problema.setProperty("campos", campos);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problema(CodigoError.DATOS_INVALIDOS,
                "El cuerpo de la solicitud no tiene un formato válido."));
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problema(CodigoError.RECURSO_NO_ENCONTRADO,
                "El recurso solicitado no existe."));
    }

    private ResponseEntity<ProblemDetail> responder(CodigoError codigo, String detalle) {
        return ResponseEntity.status(codigo.estadoHttp()).body(problema(codigo, detalle));
    }

    private ProblemDetail problema(CodigoError codigo, String detalle) {
        HttpStatus estado = HttpStatus.valueOf(codigo.estadoHttp());
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(estado.getReasonPhrase());
        problema.setProperty("codigo", codigo.name());
        return problema;
    }

    /** Campo con error de validación. */
    public record CampoInvalido(String campo, String mensaje) {
    }
}
