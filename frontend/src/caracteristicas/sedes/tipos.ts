export type EstadoCancha = 'ACTIVA' | 'MANTENIMIENTO' | 'INACTIVA'

export interface Foto {
  id: number
  url: string
  orden: number
}

/** Horario de un día (ISO: 1 = lunes ... 7 = domingo). Las horas son "HH:mm" y el cierre puede ser "24:00". */
export interface HorarioDia {
  diaSemana: number
  cerrado: boolean
  apertura: string | null
  cierre: string | null
}

export interface Tarifa {
  id: number
  canchaId: number
  diasSemana: number[]
  horaInicio: string
  horaFin: string
  aplicaFestivo: boolean
  precioHora: number
  vigenteDesde: string | null
  vigenteHasta: string | null
}

export interface Cancha {
  id: number
  sedeId: number
  sedeNombre: string
  nombre: string
  modalidad: string
  jugadoresPorEquipo: number
  capacidadTotal: number
  superficie: string
  largoM: number | null
  anchoM: number | null
  techada: boolean
  iluminacion: boolean
  precioBaseHora: number
  duracionBloqueMin: number
  duracionMinMin: number
  duracionMaxMin: number
  estado: EstadoCancha
  descripcion: string | null
  orden: number
  esCompuesta: boolean
  componentes: { id: number; nombre: string; modalidad: string }[]
  fotos: Foto[]
  tarifas: Tarifa[]
  horarioEfectivo: HorarioDia[]
  horarioPropio: HorarioDia[]
}

export interface Sede {
  id: number
  nombre: string
  direccion: string
  ciudad: string
  telefono: string | null
  whatsapp: string | null
  descripcion: string | null
  servicios: string[]
  latitud: number | null
  longitud: number | null
  activa: boolean
  horario: HorarioDia[]
  fotos: Foto[]
  canchas: Cancha[]
}

export interface Festivo {
  id: number
  fecha: string
  nombre: string
}

export interface Cotizacion {
  total: number
  tramos: {
    inicio: string
    fin: string
    minutos: number
    precioHora: number
    origen: 'BASE' | 'TARIFA' | 'FESTIVO'
  }[]
}

export interface PrevisualizacionEstado {
  reservasFuturasActivas: number
  requiereConfirmacion: boolean
}
