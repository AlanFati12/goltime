package co.edu.usco.goltime.reservas.infraestructura;

import java.time.Instant;

import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.compartido.infraestructura.persistencia.EntidadAuditable;
import co.edu.usco.goltime.reservas.dominio.EstadoPago;
import co.edu.usco.goltime.reservas.dominio.EstadoReserva;
import co.edu.usco.goltime.reservas.dominio.OrigenReserva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "reservas")
class ReservaEntidad extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, updatable = false)
    String codigo;

    @Column(name = "cliente_id", updatable = false)
    Long clienteId;

    @Column(name = "cliente_mostrador_id", updatable = false)
    Long clienteMostradorId;

    @Column(name = "cancha_id", nullable = false)
    long canchaId;

    @Column(nullable = false)
    Instant inicio;

    @Column(nullable = false)
    Instant fin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    EstadoReserva estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    OrigenReserva origen;

    @Column(name = "precio_base", nullable = false)
    int precioBase;

    @Column(nullable = false)
    int descuento;

    @Column(name = "precio_final", nullable = false)
    int precioFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false)
    EstadoPago estadoPago;

    @Column(name = "monto_pagado", nullable = false)
    int montoPagado;

    @Column(name = "nombre_equipo")
    String nombreEquipo;

    String notas;

    @Column(name = "cancelada_por")
    Long canceladaPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_cancelador")
    Rol rolCancelador;

    @Column(name = "motivo_cancelacion")
    String motivoCancelacion;

    @Column(name = "fecha_cancelacion")
    Instant fechaCancelacion;

    @Column(name = "cancelacion_tardia", nullable = false)
    boolean cancelacionTardia;

    @Column(name = "veces_reprogramada", nullable = false)
    int vecesReprogramada;

    @Column(name = "aprobada_por")
    Long aprobadaPor;

    @Column(name = "fecha_aprobacion")
    Instant fechaAprobacion;

    @Column(name = "motivo_rechazo")
    String motivoRechazo;

    @Column(name = "expira_en")
    Instant expiraEn;

    @Column(name = "clave_idempotencia", updatable = false)
    String claveIdempotencia;

    @Column(name = "recordatorio_enviado_en")
    Instant recordatorioEnviadoEn;

    @Version
    long version;
}
