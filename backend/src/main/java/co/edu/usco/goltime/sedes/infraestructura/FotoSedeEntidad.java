package co.edu.usco.goltime.sedes.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sede_fotos")
class FotoSedeEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "sede_id", nullable = false)
    long sedeId;

    @Column(nullable = false)
    String archivo;

    @Column(nullable = false)
    int orden;
}
