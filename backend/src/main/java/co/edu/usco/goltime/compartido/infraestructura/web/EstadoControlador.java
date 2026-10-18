package co.edu.usco.goltime.compartido.infraestructura.web;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/publico/estado")
@Tag(name = "Estado", description = "Verificación de que la API está viva")
public class EstadoControlador {

    private final String nombre;
    private final String zonaHoraria;

    public EstadoControlador(@Value("${spring.application.name}") String nombre,
            @Value("${goltime.zona-horaria}") String zonaHoraria) {
        this.nombre = nombre;
        this.zonaHoraria = zonaHoraria;
    }

    @GetMapping
    @Operation(summary = "Estado de la API")
    public EstadoRespuesta estado() {
        return new EstadoRespuesta(nombre, "OK", zonaHoraria, Instant.now());
    }

    public record EstadoRespuesta(String aplicacion, String estado, String zonaHoraria, Instant fechaServidor) {
    }
}
