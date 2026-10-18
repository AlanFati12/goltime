export type FormatoTorneo = 'LIGA' | 'ELIMINACION_DIRECTA' | 'GRUPOS_Y_ELIMINACION'
export type EstadoTorneo = 'BORRADOR' | 'INSCRIPCIONES' | 'EN_CURSO' | 'FINALIZADO' | 'CANCELADO'
export type ModoEmparejamiento = 'MANUAL' | 'ALEATORIO' | 'SEMBRADO'
export type EstadoPartido =
  'PENDIENTE' | 'PROGRAMADO' | 'EN_JUEGO' | 'FINALIZADO' | 'APLAZADO' | 'WO' | 'BYE'
export type TipoEvento =
  'GOL' | 'GOL_PENAL' | 'AUTOGOL' | 'ASISTENCIA' | 'AMARILLA' | 'ROJA' | 'FALTA'
export type CriterioDesempate =
  | 'PUNTOS'
  | 'DIFERENCIA_GOL'
  | 'GOLES_A_FAVOR'
  | 'ENFRENTAMIENTO_DIRECTO'
  | 'JUEGO_LIMPIO'
  | 'SORTEO'

export interface Torneo {
  id: number
  nombre: string
  descripcion: string | null
  sedeId: number
  sedeNombre: string | null
  jugadoresPorEquipo: number
  formato: FormatoTorneo
  estado: EstadoTorneo
  fechaInicio: string | null
  fechaFin: string | null
  cupoEquipos: number | null
  valorInscripcion: number
  premiosTexto: string | null
  reglamento: string | null
  modoEmparejamiento: ModoEmparejamiento
  idaYVuelta: boolean
  tercerPuesto: boolean
  numGrupos: number | null
  clasificadosPorGrupo: number | null
  puntosVictoria: number
  puntosEmpate: number
  puntosDerrota: number
  criteriosDesempate: CriterioDesempate[]
  marcadorWoGoles: number
  campeonId: number | null
  subcampeonId: number | null
  terceroId: number | null
  campeon: string | null
  subcampeon: string | null
  tercero: string | null
  equiposInscritos: number
}

export interface Inscripcion {
  id: number
  equipoId: number
  equipo: string
  color: string | null
  semilla: number | null
  jugadores: number
}

export interface DetalleTorneo {
  torneo: Torneo
  canchaIds: number[]
  inscripciones: Inscripcion[]
  solicitudes: Inscripcion[]
  fases: { id: number; tipo: 'GRUPOS' | 'LIGA' | 'ELIMINATORIA'; orden: number; nombre: string }[]
  partidosTotales: number
  partidosJugados: number
}

export interface Lado {
  inscripcionId: number | null
  equipoId: number | null
  nombre: string | null
  color: string | null
  etiqueta: string | null
}

export interface Partido {
  id: number
  torneoId: number
  torneo: string
  faseId: number
  tipoFase: 'GRUPOS' | 'LIGA' | 'ELIMINATORIA'
  grupo: string | null
  ronda: number
  numeroLlave: number
  nombreRonda: string | null
  local: Lado | null
  visitante: Lado | null
  canchaId: number | null
  cancha: string | null
  inicio: string | null
  duracionMin: number | null
  arbitro: string | null
  estado: EstadoPartido
  golesLocal: number | null
  golesVisitante: number | null
  penalesLocal: number | null
  penalesVisitante: number | null
  ganadorInscripcionId: number | null
  descanso: boolean
  porWO: boolean
  figuraJugadorId: number | null
  figura: string | null
  siguientePartidoId: number | null
}

export interface EventoPartido {
  id: number
  partidoId: number
  inscripcionId: number
  equipo: string
  jugadorId: number
  jugador: string
  dorsal: number | null
  tipo: TipoEvento
  minuto: number
  eventoRelacionadoId: number | null
  observacion: string | null
}

export interface DetallePartido {
  partido: Partido
  eventos: EventoPartido[]
}

export interface FilaPosicion {
  inscripcionId: number
  equipoId: number | null
  equipo: string | null
  color: string | null
  posicion: number
  jugados: number
  ganados: number
  empatados: number
  perdidos: number
  golesFavor: number
  golesContra: number
  diferencia: number
  puntos: number
  puntosDisciplina: number
  forma: string[]
}

export interface Tabla {
  faseId: number
  fase: string
  grupoId: number | null
  grupo: string | null
  filas: FilaPosicion[]
}

export interface Estadisticas {
  goleadores: {
    jugadorId: number
    jugador: string
    equipoId: number
    equipo: string
    goles: number
    penales: number
    partidos: number
  }[]
  asistidores: {
    jugadorId: number
    jugador: string
    equipoId: number
    equipo: string
    asistencias: number
  }[]
  tarjetas: {
    jugadorId: number
    jugador: string
    equipoId: number
    equipo: string
    amarillas: number
    rojas: number
  }[]
}

export interface PerfilEquipo {
  equipo: { id: number; nombre: string; color: string | null }
  plantilla: { id: number; nombre: string; posicion: string | null; dorsal: number | null }[]
  torneos: { torneoId: number; nombre: string; estado: EstadoTorneo }[]
  partidos: Partido[]
}

export interface PerfilJugador {
  id: number
  nombre: string
  posicion: string | null
  torneos: {
    torneoId: number
    torneo: string
    equipo: string
    goles: number
    asistencias: number
    amarillas: number
    rojas: number
  }[]
}

// ----- Administración -----

export interface Equipo {
  id: number
  nombre: string
  color: string | null
  delegadoNombre: string | null
  delegadoTelefono: string | null
  usuarioResponsableId: number | null
  activo: boolean
}

export interface JugadorEquipo {
  id: number
  nombres: string
  apellidos: string
  nombreCompleto: string
  documento: string | null
  posicion: string | null
  dorsal: number | null
}

export interface ResumenEquipo {
  equipo: Equipo
  jugadores: number
  torneos: number
}

export interface DetalleEquipo {
  equipo: Equipo
  plantilla: JugadorEquipo[]
}

export interface ConflictoHorario {
  reservaId: number
  codigo: string
  cliente: string | null
  cancha: string
  inicio: string
  fin: string
  precioFinal: number
  conPago: boolean
  estadoPago: string
  estado: string
}

export interface PrevisualizacionPartido {
  conflictos: ConflictoHorario[]
  huella: string
  requiereConfirmacion: boolean
}

export interface ResumenFixture {
  fases: number
  grupos: number
  partidos: number
  descansos: number
}
