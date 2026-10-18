package co.edu.usco.goltime.autenticacion.infraestructura;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")
class TokenRefrescoEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "usuario_id", nullable = false)
    long usuarioId;

    @Column(name = "familia_id", nullable = false)
    UUID familiaId;

    @Column(name = "token_hash", nullable = false)
    String tokenHash;

    @Column(name = "expira_en", nullable = false)
    Instant expiraEn;

    @Column(nullable = false)
    boolean revocado;

    @Column(name = "reemplazado_por")
    Long reemplazadoPor;

    @Column(name = "user_agent")
    String userAgent;

    String ip;

    @Column(nullable = false)
    boolean recordar;

    @Column(name = "creado_en", nullable = false, updatable = false)
    Instant creadoEn = Instant.now();
}
