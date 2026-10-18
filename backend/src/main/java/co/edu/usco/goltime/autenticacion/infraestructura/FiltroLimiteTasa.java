package co.edu.usco.goltime.autenticacion.infraestructura;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Aplica límite de tasa por IP a ingreso, registro y recuperación de contraseña. */
@Component
class FiltroLimiteTasa extends OncePerRequestFilter {

    private static final String BASE = "/api/v1/autenticacion";

    private final Map<String, LimitadorTasa> limitadores;
    private final ObjectMapper mapeador;

    FiltroLimiteTasa(PropiedadesLimiteTasa limites, ObjectMapper mapeador) {
        this.mapeador = mapeador;
        this.limitadores = Map.of(
                BASE + "/ingresar", new LimitadorTasa(limites.ingresoPorMinuto(), Duration.ofMinutes(1)),
                BASE + "/registrar", new LimitadorTasa(limites.registroPorHora(), Duration.ofHours(1)),
                BASE + "/olvide-clave", new LimitadorTasa(limites.recuperacionPorHora(), Duration.ofHours(1)),
                BASE + "/restablecer-clave", new LimitadorTasa(limites.recuperacionPorHora(), Duration.ofHours(1)));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return !"POST".equals(peticion.getMethod()) || !limitadores.containsKey(peticion.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String ruta = peticion.getRequestURI();
        if (limitadores.get(ruta).permitir(peticion.getRemoteAddr())) {
            cadena.doFilter(peticion, respuesta);
            return;
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "Demasiadas solicitudes. Intenta de nuevo más tarde.");
        problema.setTitle(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        problema.setProperty("codigo", CodigoError.DEMASIADOS_INTENTOS.name());
        respuesta.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        mapeador.writeValue(respuesta.getOutputStream(), problema);
    }
}
