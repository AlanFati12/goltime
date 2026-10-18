package co.edu.usco.goltime.torneos.dominio;

import java.time.Instant;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/**
 * Partido de un torneo. Un lado puede estar por definir (referencia a un clasificado) hasta que se conozca el equipo.
 * Los rivales son inscripciones al torneo, no equipos sueltos.
 */
public class Partido {

    private Long id;
    private long torneoId;
    private long faseId;
    private TipoFase tipoFase;
    private Long grupoId;
    private String grupoNombre;
    private int ronda;
    private int numeroLlave;
    private String nombreRonda;
    private Long localId;
    private Long visitanteId;
    private String etiquetaLocal;
    private String etiquetaVisitante;
    private String refLocal;
    private String refVisitante;
    private Long siguienteId;
    private Lado ladoSiguiente;
    private Long siguientePerdedorId;
    private Lado ladoPerdedor;
    private Long canchaId;
    private Long reservaId;
    private Instant inicio;
    private Integer duracionMin;
    private String arbitro;
    private EstadoPartido estado;
    private Integer golesLocal;
    private Integer golesVisitante;
    private Integer penalesLocal;
    private Integer penalesVisitante;
    private Long ganadorId;
    private boolean bye;
    private boolean wo;
    private Long figuraJugadorId;

    private Partido() {
    }

    /** Partido nuevo a partir del plan del generador; {@code id} lo asigna el almacenamiento. */
    public static Partido desdePlan(long torneoId, long faseId, TipoFase tipoFase, Long grupoId, String grupoNombre, PartidoPlan plan) {
        Partido p = new Partido();
        p.torneoId = torneoId;
        p.faseId = faseId;
        p.tipoFase = tipoFase;
        p.grupoId = grupoId;
        p.grupoNombre = grupoNombre;
        p.ronda = plan.ronda();
        p.numeroLlave = plan.numeroLlave();
        p.nombreRonda = plan.nombreRonda();
        p.estado = plan.bye() ? EstadoPartido.BYE : EstadoPartido.PENDIENTE;
        p.bye = plan.bye();
        if (plan.local() != null) {
            p.localId = plan.local().inscripcionId();
            p.etiquetaLocal = plan.local().etiqueta();
            p.refLocal = plan.local().referencia();
        }
        if (plan.visitante() != null) {
            p.visitanteId = plan.visitante().inscripcionId();
            p.etiquetaVisitante = plan.visitante().etiqueta();
            p.refVisitante = plan.visitante().referencia();
        }
        p.ladoSiguiente = plan.ladoEnSiguiente();
        p.ladoPerdedor = plan.ladoPerdedor();
        return p;
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static Partido reconstruir(Long id, long torneoId, long faseId, TipoFase tipoFase, Long grupoId, String grupoNombre, int ronda,
            int numeroLlave, String nombreRonda, Long localId, Long visitanteId, String etiquetaLocal, String etiquetaVisitante, String refLocal,
            String refVisitante, Long siguienteId, Lado ladoSiguiente, Long siguientePerdedorId, Lado ladoPerdedor, Long canchaId, Long reservaId,
            Instant inicio, Integer duracionMin, String arbitro, EstadoPartido estado, Integer golesLocal, Integer golesVisitante,
            Integer penalesLocal, Integer penalesVisitante, Long ganadorId, boolean bye, boolean wo, Long figuraJugadorId) {
        Partido p = new Partido();
        p.id = id;
        p.torneoId = torneoId;
        p.faseId = faseId;
        p.tipoFase = tipoFase;
        p.grupoId = grupoId;
        p.grupoNombre = grupoNombre;
        p.ronda = ronda;
        p.numeroLlave = numeroLlave;
        p.nombreRonda = nombreRonda;
        p.localId = localId;
        p.visitanteId = visitanteId;
        p.etiquetaLocal = etiquetaLocal;
        p.etiquetaVisitante = etiquetaVisitante;
        p.refLocal = refLocal;
        p.refVisitante = refVisitante;
        p.siguienteId = siguienteId;
        p.ladoSiguiente = ladoSiguiente;
        p.siguientePerdedorId = siguientePerdedorId;
        p.ladoPerdedor = ladoPerdedor;
        p.canchaId = canchaId;
        p.reservaId = reservaId;
        p.inicio = inicio;
        p.duracionMin = duracionMin;
        p.arbitro = arbitro;
        p.estado = estado;
        p.golesLocal = golesLocal;
        p.golesVisitante = golesVisitante;
        p.penalesLocal = penalesLocal;
        p.penalesVisitante = penalesVisitante;
        p.ganadorId = ganadorId;
        p.bye = bye;
        p.wo = wo;
        p.figuraJugadorId = figuraJugadorId;
        return p;
    }

    // ----- Enlaces del cuadro -----

    public void enlazar(Long siguienteId, Long siguientePerdedorId) {
        this.siguienteId = siguienteId;
        this.siguientePerdedorId = siguientePerdedorId;
    }

    /** Pone (o quita, con {@code null}) a la inscripción en un lado del partido. */
    public void colocar(Lado lado, Long inscripcionId) {
        if (lado == Lado.LOCAL) {
            localId = inscripcionId;
        } else {
            visitanteId = inscripcionId;
        }
    }

    public Long equipoEn(Lado lado) {
        return lado == Lado.LOCAL ? localId : visitanteId;
    }

    public String referenciaEn(Lado lado) {
        return lado == Lado.LOCAL ? refLocal : refVisitante;
    }

    /** Los dos rivales ya se conocen. */
    public boolean listoParaJugar() {
        return localId != null && visitanteId != null && !bye;
    }

    public boolean jugado() {
        return estado.jugado();
    }

    public boolean esEliminatoria() {
        return tipoFase == TipoFase.ELIMINATORIA;
    }

    public Long perdedorId() {
        if (ganadorId == null || localId == null || visitanteId == null) {
            return null;
        }
        return ganadorId.equals(localId) ? visitanteId : localId;
    }

    // ----- Programación -----

    public void programar(long canchaId, long reservaId, Instant inicio, int duracionMin, String arbitro) {
        exigirNoJugado("programar");
        if (bye) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "Un partido con descanso no se programa.");
        }
        if (duracionMin < 20 || duracionMin > 240) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La duración del partido debe estar entre 20 y 240 minutos.");
        }
        this.canchaId = canchaId;
        this.reservaId = reservaId;
        this.inicio = inicio;
        this.duracionMin = duracionMin;
        this.arbitro = arbitro == null || arbitro.isBlank() ? null : arbitro.trim();
        this.estado = EstadoPartido.PROGRAMADO;
    }

    /** Quita fecha y cancha. {@code destino} es PENDIENTE o APLAZADO. */
    public void desprogramar(EstadoPartido destino) {
        exigirNoJugado("desprogramar");
        if (destino != EstadoPartido.PENDIENTE && destino != EstadoPartido.APLAZADO) {
            throw new IllegalArgumentException("Destino no válido: " + destino);
        }
        canchaId = null;
        reservaId = null;
        inicio = null;
        duracionMin = null;
        estado = destino;
    }

    public void iniciar() {
        exigirNoJugado("iniciar");
        if (!listoParaJugar()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El partido aún tiene rivales por definir.");
        }
        estado = EstadoPartido.EN_JUEGO;
    }

    // ----- Resultado -----

    /** Registra o corrige el marcador (y penales si es eliminatoria y hay empate). */
    public void registrarResultado(int golesLocal, int golesVisitante, Integer penalesLocal, Integer penalesVisitante, Long figuraJugadorId) {
        if (!listoParaJugar()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El partido aún tiene rivales por definir.");
        }
        Lado ganador = ResolucionPartido.ganador(golesLocal, golesVisitante, penalesLocal, penalesVisitante, esEliminatoria());
        this.golesLocal = golesLocal;
        this.golesVisitante = golesVisitante;
        this.penalesLocal = penalesLocal;
        this.penalesVisitante = penalesVisitante;
        this.ganadorId = ganador == null ? null : equipoEn(ganador);
        this.figuraJugadorId = figuraJugadorId;
        this.wo = false;
        this.estado = EstadoPartido.FINALIZADO;
    }

    /** Gana el rival por no presentación del otro, con el marcador configurado del torneo. */
    public void registrarWO(Lado ganador, int marcadorGoles) {
        if (!listoParaJugar()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El partido aún tiene rivales por definir.");
        }
        this.golesLocal = ganador == Lado.LOCAL ? marcadorGoles : 0;
        this.golesVisitante = ganador == Lado.VISITANTE ? marcadorGoles : 0;
        this.penalesLocal = null;
        this.penalesVisitante = null;
        this.ganadorId = equipoEn(ganador);
        this.figuraJugadorId = null;
        this.wo = true;
        this.estado = EstadoPartido.WO;
    }

    /** Deshace el resultado para poder corregirlo. */
    public void reabrir() {
        if (!jugado()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "El partido no tiene resultado.");
        }
        golesLocal = null;
        golesVisitante = null;
        penalesLocal = null;
        penalesVisitante = null;
        ganadorId = null;
        figuraJugadorId = null;
        wo = false;
        estado = reservaId != null ? EstadoPartido.PROGRAMADO : EstadoPartido.PENDIENTE;
    }

    /** El descanso (bye) da por ganador a quien avanza solo. */
    public void marcarBye() {
        if (!bye || localId == null) {
            return;
        }
        ganadorId = localId;
        estado = EstadoPartido.BYE;
    }

    /** Vuelve el descanso a su estado inicial (cuando el equipo que lo ocupaba deja de ser conocido). */
    public void reabrirBye() {
        if (bye) {
            ganadorId = null;
        }
    }

    private void exigirNoJugado(String accion) {
        if (jugado()) {
            throw new ExcepcionNegocio(CodigoError.ESTADO_INVALIDO, "No se puede " + accion + " un partido que ya tiene resultado.");
        }
    }

    // ----- Accesores -----

    public Long id() { return id; }
    public long torneoId() { return torneoId; }
    public long faseId() { return faseId; }
    public TipoFase tipoFase() { return tipoFase; }
    public Long grupoId() { return grupoId; }
    public String grupoNombre() { return grupoNombre; }
    public int ronda() { return ronda; }
    public int numeroLlave() { return numeroLlave; }
    public String nombreRonda() { return nombreRonda; }
    public Long localId() { return localId; }
    public Long visitanteId() { return visitanteId; }
    public String etiquetaLocal() { return etiquetaLocal; }
    public String etiquetaVisitante() { return etiquetaVisitante; }
    public String refLocal() { return refLocal; }
    public String refVisitante() { return refVisitante; }
    public Long siguienteId() { return siguienteId; }
    public Lado ladoSiguiente() { return ladoSiguiente; }
    public Long siguientePerdedorId() { return siguientePerdedorId; }
    public Lado ladoPerdedor() { return ladoPerdedor; }
    public Long canchaId() { return canchaId; }
    public Long reservaId() { return reservaId; }
    public Instant inicio() { return inicio; }
    public Integer duracionMin() { return duracionMin; }
    public Instant fin() { return inicio == null || duracionMin == null ? null : inicio.plusSeconds(duracionMin * 60L); }
    public String arbitro() { return arbitro; }
    public EstadoPartido estado() { return estado; }
    public Integer golesLocal() { return golesLocal; }
    public Integer golesVisitante() { return golesVisitante; }
    public Integer penalesLocal() { return penalesLocal; }
    public Integer penalesVisitante() { return penalesVisitante; }
    public Long ganadorId() { return ganadorId; }
    public boolean bye() { return bye; }
    public boolean wo() { return wo; }
    public Long figuraJugadorId() { return figuraJugadorId; }

    /** Solo para el almacenamiento tras insertar. */
    public void asignarId(long id) {
        this.id = id;
    }
}
