package co.edu.usco.goltime.sedes.infraestructura;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import co.edu.usco.goltime.compartido.infraestructura.persistencia.EntidadAuditable;
import co.edu.usco.goltime.sedes.dominio.EstadoCancha;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "canchas")
class CanchaEntidad extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "sede_id", nullable = false, updatable = false)
    long sedeId;

    @Column(nullable = false)
    String nombre;

    @Column(nullable = false)
    String modalidad;

    @Column(name = "jugadores_por_equipo", nullable = false)
    int jugadoresPorEquipo;

    @Column(name = "capacidad_total", nullable = false)
    int capacidadTotal;

    @Column(nullable = false)
    String superficie;

    @Column(name = "largo_m")
    BigDecimal largoM;

    @Column(name = "ancho_m")
    BigDecimal anchoM;

    @Column(nullable = false)
    boolean techada;

    @Column(nullable = false)
    boolean iluminacion;

    @Column(name = "precio_base_hora", nullable = false)
    int precioBaseHora;

    @Column(name = "duracion_bloque_min", nullable = false)
    int duracionBloqueMin;

    @Column(name = "duracion_min_min", nullable = false)
    int duracionMinMin;

    @Column(name = "duracion_max_min", nullable = false)
    int duracionMaxMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    EstadoCancha estado = EstadoCancha.ACTIVA;

    @Column(columnDefinition = "text")
    String descripcion;

    @Column(nullable = false)
    int orden;

    @Version
    long version;

    @ElementCollection
    @CollectionTable(name = "cancha_horarios", joinColumns = @JoinColumn(name = "cancha_id"))
    List<HorarioDiaEmbebido> horarioPropio = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "cancha_composicion", joinColumns = @JoinColumn(name = "cancha_compuesta_id"))
    @Column(name = "cancha_componente_id")
    Set<Long> componentes = new HashSet<>();
}
