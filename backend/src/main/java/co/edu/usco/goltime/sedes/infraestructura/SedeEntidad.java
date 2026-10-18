package co.edu.usco.goltime.sedes.infraestructura;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import co.edu.usco.goltime.compartido.infraestructura.persistencia.EntidadAuditable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "sedes")
class SedeEntidad extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String nombre;

    @Column(nullable = false)
    String direccion;

    @Column(nullable = false)
    String ciudad;

    String telefono;

    String whatsapp;

    @Column(columnDefinition = "text")
    String descripcion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    List<String> servicios = new ArrayList<>();

    BigDecimal latitud;

    BigDecimal longitud;

    @Column(nullable = false)
    boolean activa = true;

    @Version
    long version;

    @ElementCollection
    @CollectionTable(name = "sede_horarios", joinColumns = @JoinColumn(name = "sede_id"))
    List<HorarioDiaEmbebido> horario = new ArrayList<>();
}
