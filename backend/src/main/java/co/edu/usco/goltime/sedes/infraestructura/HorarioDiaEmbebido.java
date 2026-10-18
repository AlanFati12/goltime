package co.edu.usco.goltime.sedes.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Fila de {@code sede_horarios} / {@code cancha_horarios}. */
@Embeddable
class HorarioDiaEmbebido {

    @Column(name = "dia_semana", nullable = false)
    int diaSemana;

    @Column(nullable = false)
    boolean cerrado;

    @Column(name = "minuto_apertura")
    Integer minutoApertura;

    @Column(name = "minuto_cierre")
    Integer minutoCierre;

    protected HorarioDiaEmbebido() {
    }

    HorarioDiaEmbebido(int diaSemana, boolean cerrado, Integer minutoApertura, Integer minutoCierre) {
        this.diaSemana = diaSemana;
        this.cerrado = cerrado;
        this.minutoApertura = minutoApertura;
        this.minutoCierre = minutoCierre;
    }
}
