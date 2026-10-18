package co.edu.usco.goltime.autenticacion.infraestructura;

import java.time.Instant;

import co.edu.usco.goltime.autenticacion.dominio.TipoTokenVerificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tokens_verificacion")
class TokenVerificacionEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "usuario_id", nullable = false)
    long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TipoTokenVerificacion tipo;

    @Column(name = "token_hash", nullable = false)
    String tokenHash;

    @Column(name = "expira_en", nullable = false)
    Instant expiraEn;

    @Column(name = "usado_en")
    Instant usadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    Instant creadoEn = Instant.now();
}
