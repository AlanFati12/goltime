package co.edu.usco.goltime.compartido.infraestructura.seguridad;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Responde 401 y 403 como {@link ProblemDetail} (RFC 7807) con un código de negocio estable. */
@Component
public class RespuestasSeguridadJson implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapeador;

    public RespuestasSeguridadJson(ObjectMapper mapeador) {
        this.mapeador = mapeador;
    }

    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta,
            AuthenticationException ex) throws IOException {
        escribir(respuesta, HttpStatus.UNAUTHORIZED, CodigoError.NO_AUTENTICADO,
                "Debes iniciar sesión para continuar.");
    }

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta,
            AccessDeniedException ex) throws IOException {
        escribir(respuesta, HttpStatus.FORBIDDEN, CodigoError.ACCESO_DENEGADO,
                "No tienes permisos para realizar esta acción.");
    }

    private void escribir(HttpServletResponse respuesta, HttpStatus estado, CodigoError codigo,
            String detalle) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(estado.getReasonPhrase());
        problema.setProperty("codigo", codigo.name());
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        mapeador.writeValue(respuesta.getOutputStream(), problema);
    }
}
