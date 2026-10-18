package co.edu.usco.goltime.sedes.infraestructura;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import co.edu.usco.goltime.compartido.infraestructura.persistencia.EntidadAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tarifas")
class TarifaEntidad extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "cancha_id", nullable = false, updatable = false)
    long canchaId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dias_semana", nullable = false, columnDefinition = "smallint[]")
    short[] diasSemana = new short[0];

    @Column(name = "minuto_inicio", nullable = false)
    int minutoInicio;

    @Column(name = "minuto_fin", nullable = false)
    int minutoFin;

    @Column(name = "aplica_festivo", nullable = false)
    boolean aplicaFestivo;

    @Column(name = "precio_hora", nullable = false)
    int precioHora;

    @Column(name = "vigente_desde")
    LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    LocalDate vigenteHasta;
}
