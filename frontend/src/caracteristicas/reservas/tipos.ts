import type { Pagina } from '@/lib/api'

export type EstadoReserva =
  'PENDIENTE' | 'CONFIRMADA' | 'RECHAZADA' | 'EXPIRADA' | 'CANCELADA' | 'COMPLETADA' | 'NO_ASISTIO'
export type PestanaReservas = 'PROXIMAS' | 'HISTORIAL' | 'CANCELADAS'
export type EstadoSlot = 'DISPONIBLE' | 'OCUPADO' | 'BLOQUEADO' | 'NO_RESERVABLE'

export interface EntradaHistorial {
  id: number
  accion: string
  actorId: number | null
  actorNombre: string | null
  actorRol: 'ADMIN' | 'CLIENTE' | 'SISTEMA'
  motivo: string | null
  datosAnteriores: Record<string, unknown> | null
  datosNuevos: Record<string, unknown> | null
  fecha: string
}

export interface Reserva {
  id: number
  codigo: string
  estado: EstadoReserva
  origen: string
  cancha: { id: number; nombre: string; modalidad: string }
  sede: { id: number; nombre: string }
  cliente: {
    id: number | null
    nombre: string | null
    correo: string | null
    telefono: string | null
  }
  inicio: string
  fin: string
  duracionMin: number
  precioBase: number
  descuento: number
  precioFinal: number
  estadoPago: string
  montoPagado: number
  nombreEquipo: string | null
  notas: string | null
  expiraEn: string | null
  vecesReprogramada: number
  cancelacionTardia: boolean
  canceladaPorRol: 'ADMIN' | 'CLIENTE' | 'SISTEMA' | null
  motivoCancelacion: string | null
  fechaCancelacion: string | null
  motivoRechazo: string | null
  puedeCancelar: boolean
  puedeReprogramar: boolean
  cancelacionSeriaTardia: boolean
  creadoEn: string
  historial: EntradaHistorial[] | null
}

export type PaginaReservas = Pagina<Reserva>

export interface Slot {
  inicio: string
  fin: string
  estado: EstadoSlot
  precio: number
}

export interface DisponibilidadCancha {
  canchaId: number
  nombre: string
  modalidad: string
  sedeId: number
  sedeNombre: string
  duracionBloqueMin: number
  duracionMinMin: number
  duracionMaxMin: number
  cerrado: boolean
  slots: Slot[]
}

export interface DisponibilidadDia {
  fecha: string
  canchas: DisponibilidadCancha[]
}

export interface ResumenDia {
  fecha: string
  horariosLibres: number
}

export interface PrevisualizacionReprogramacion {
  disponible: boolean
  precioActual: number
  precioNuevo: number
  diferencia: number
}

export interface Notificacion {
  id: number
  tipo: string
  titulo: string
  mensaje: string
  leida: boolean
  entidad: string | null
  entidadId: number | null
  creadoEn: string
}
