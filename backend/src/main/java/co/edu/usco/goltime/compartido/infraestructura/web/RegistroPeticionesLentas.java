package co.edu.usco.goltime.compartido.infraestructura.web;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Deja una advertencia en el registro por cada petición que tarda más del umbral
 * ({@code goltime.observabilidad.umbral-peticion-lenta-ms}). Es la forma más barata de enterarse de qué rutas se
 * sienten lentas, sin montar nada más; con las trazas de OpenTelemetry activadas se puede ver además dónde se fue el
 * tiempo. Registra solo método, ruta y estado (nunca la cadena de consulta, que puede traer datos personales).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RegistroPeticionesLentas extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RegistroPeticionesLentas.class);

    private final long umbralMs;

    public RegistroPeticionesLentas(@Value("${goltime.observabilidad.umbral-peticion-lenta-ms:800}") long umbralMs) {
        this.umbralMs = umbralMs;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        long inicio = System.nanoTime();
        try {
            cadena.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            if (ms >= umbralMs) {
                log.warn("Petición lenta: {} {} -> {} en {} ms", request.getMethod(), request.getRequestURI(), response.getStatus(), ms);
            }
        }
    }
}
