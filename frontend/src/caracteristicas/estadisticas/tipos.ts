export type Agrupacion = 'DIA' | 'SEMANA' | 'MES'

export interface Resumen {
  ingresosReales: number
  recaudado: number
  porCobrar: number
  reservas: number
  porEstado: Record<string, number>
  completadas: number
  canceladas: number
  noAsistio: number
  tasaCancelacion: number
  tasaNoAsistencia: number
  horasJugadas: number
  ticketPromedio: number
  clientesUnicos: number
  ocupacionPct: number
  partidosTorneo: number
}

export interface Periodo {
  desde: string
  hasta: string
}

export interface ResumenConComparacion {
  periodo: Periodo
  actual: Resumen
  periodoAnterior: Periodo
  anterior: Resumen
  variaciones: Partial<Record<string, number>>
}

export interface PuntoSerie {
  fecha: string
  ingresos: number
  completadas: number
  canceladas: number
}

export interface FilaCancha {
  canchaId: number
  cancha: string
  sede: string
  completadas: number
  horas: number
  ingresos: number
  ocupacionPct: number
}

export interface HorasPico {
  celdas: { diaSemana: number; hora: number; reservas: number }[]
  maximo: number
}

export interface ClienteFrecuente {
  nombre: string
  contacto: string | null
  mostrador: boolean
  completadas: number
  canceladas: number
  noAsistio: number
  gasto: number
  ultimaReserva: string
}

export interface FiltroEstadisticas {
  desde: string
  hasta: string
  sedeId: number | null
  canchaId: number | null
}
