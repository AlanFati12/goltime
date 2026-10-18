package co.edu.usco.goltime.sedes.dominio;

import java.time.LocalDate;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

public record Festivo(Long id, LocalDate fecha, String nombre) {

    public Festivo {
        if (fecha == null || nombre == null || nombre.isBlank()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El festivo necesita fecha y nombre.");
        }
        nombre = nombre.trim();
    }
}
