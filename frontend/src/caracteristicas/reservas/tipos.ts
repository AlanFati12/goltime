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

/** Efecto de un cambio de estado del administrador, calculado en el backend (ADR 0019). */
export interface PrevisualizacionEstado {
  actual: EstadoReserva
  nuevo: EstadoReserva
  ocuparaHorario: boolean
  horarioLibre: boolean
  liberaraHorario: boolean
  cuentaParaPremios: boolean
  dejaDeContarParaPremios: boolean
  quedaReembolsoPendiente: boolean
  seCompletaraSola: boolean
  expiraraSola: boolean
  avisaAlCliente: boolean
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

export type MetodoPago = 'EFECTIVO' | 'TRANSFERENCIA' | 'NEQUI' | 'DAVIPLATA' | 'TARJETA'
export type TipoPago = 'PAGO' | 'ABONO' | 'REEMBOLSO'

export interface Pago {
  id: number
  monto: number
  metodo: MetodoPago
  referencia: string | null
  tipo: TipoPago
  registradoPor: string | null
  fecha: string
}

export interface ClienteMostrador {
  id: number
  nombre: string
  telefono: string | null
}

export type MotivoBloqueo = 'MANTENIMIENTO' | 'EVENTO_PRIVADO' | 'TORNEO' | 'CLIMA' | 'OTRO'

export interface Bloqueo {
  id: number
  sedeId: number
  canchaIds: number[]
  tipo: 'PUNTUAL' | 'RECURRENTE'
  inicio: string | null
  fin: string | null
  todoElDia: boolean
  diasSemana: number[]
  horaInicio: string | null
  horaFin: string | null
  fechaDesde: string | null
  fechaHasta: string | null
  motivo: MotivoBloqueo
  detalle: string | null
  motivoTexto: string
  activo: boolean
}

export interface ConflictoReserva {
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

export interface PrevisualizacionBloqueo {
  conflictos: ConflictoReserva[]
  intervalos: number
  canchasAfectadas: number
  huella: string
  requiereConfirmacion: boolean
}

export interface ElementoAgenda {
  tipo: 'RESERVA' | 'BLOQUEO'
  canchaId: number
  inicio: string
  fin: string
  reservaId: number | null
  codigo: string | null
  estado: EstadoReserva | null
  cliente: string | null
  equipo: string | null
  precioFinal: number | null
  estadoPago: string | null
  origen: string | null
  canchaReservaNombre: string | null
  bloqueoId: number | null
  motivoBloqueo: string | null
}

export interface Agenda {
  canchas: { id: number; nombre: string; modalidad: string; estado: string; esCompuesta: boolean }[]
  elementos: ElementoAgenda[]
}

export interface RegistroAuditado {
  id: number
  fecha: string
  actorId: number | null
  actorNombre: string | null
  actorRol: 'ADMIN' | 'CLIENTE' | 'SISTEMA'
  accion: string
  entidad: string
  entidadId: number | null
  cambios: Record<string, unknown> | null
  motivo: string | null
  ip: string | null
}
