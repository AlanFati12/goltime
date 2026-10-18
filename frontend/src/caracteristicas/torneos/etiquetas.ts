import type {
  CriterioDesempate,
  EstadoPartido,
  EstadoTorneo,
  FormatoTorneo,
  ModoEmparejamiento,
  TipoEvento,
} from './tipos'

export const ETIQUETA_FORMATO: Record<FormatoTorneo, string> = {
  LIGA: 'Liga (todos contra todos)',
  ELIMINACION_DIRECTA: 'Eliminación directa',
  GRUPOS_Y_ELIMINACION: 'Grupos y eliminación',
}

export const ETIQUETA_ESTADO_TORNEO: Record<EstadoTorneo, string> = {
  BORRADOR: 'Borrador',
  INSCRIPCIONES: 'Inscripciones abiertas',
  EN_CURSO: 'En curso',
  FINALIZADO: 'Finalizado',
  CANCELADO: 'Cancelado',
}

export const TONO_ESTADO_TORNEO: Record<EstadoTorneo, 'verde' | 'ambar' | 'gris' | 'rojo'> = {
  BORRADOR: 'gris',
  INSCRIPCIONES: 'ambar',
  EN_CURSO: 'verde',
  FINALIZADO: 'gris',
  CANCELADO: 'rojo',
}

export const ETIQUETA_ESTADO_PARTIDO: Record<EstadoPartido, string> = {
  PENDIENTE: 'Sin programar',
  PROGRAMADO: 'Programado',
  EN_JUEGO: 'En juego',
  FINALIZADO: 'Finalizado',
  APLAZADO: 'Aplazado',
  WO: 'W.O.',
  BYE: 'Descanso',
}

export const ETIQUETA_MODO: Record<ModoEmparejamiento, string> = {
  MANUAL: 'Manual (orden de inscripción)',
  ALEATORIO: 'Aleatorio (sorteo)',
  SEMBRADO: 'Sembrado (por cabezas de serie)',
}

export const ETIQUETA_CRITERIO: Record<CriterioDesempate, string> = {
  PUNTOS: 'Puntos',
  DIFERENCIA_GOL: 'Diferencia de gol',
  GOLES_A_FAVOR: 'Goles a favor',
  ENFRENTAMIENTO_DIRECTO: 'Enfrentamiento directo',
  JUEGO_LIMPIO: 'Juego limpio',
  SORTEO: 'Sorteo',
}

export const ETIQUETA_EVENTO: Record<TipoEvento, string> = {
  GOL: 'Gol',
  GOL_PENAL: 'Gol de penal',
  AUTOGOL: 'Autogol',
  ASISTENCIA: 'Asistencia',
  AMARILLA: 'Tarjeta amarilla',
  ROJA: 'Tarjeta roja',
  FALTA: 'Falta',
}

export const ICONO_EVENTO: Record<TipoEvento, string> = {
  GOL: '⚽',
  GOL_PENAL: '⚽',
  AUTOGOL: '⚽',
  ASISTENCIA: '👟',
  AMARILLA: '🟨',
  ROJA: '🟥',
  FALTA: '✋',
}
