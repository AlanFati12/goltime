export type TipoDescuento = 'PORCENTAJE' | 'MONTO_FIJO'
export type EstadoPremio = 'DISPONIBLE' | 'USADO' | 'VENCIDO' | 'REVOCADO'

export interface Promocion {
  id: number
  nombre: string
  descripcion: string | null
  codigo: string | null
  tipo: TipoDescuento
  valor: number
  descuentoTexto: string
  fechaDesde: string
  fechaHasta: string
  diasSemana: number[]
  minutoInicio: number | null
  minutoFin: number | null
  sedeId: number | null
  canchaId: number | null
  usosMaximos: number | null
  usosPorCliente: number | null
  soloPrimeraReserva: boolean
  publica: boolean
  activa: boolean
  automatica: boolean
}

export interface DetallePromocion {
  promocion: Promocion
  usosActivos: number
  montoDescontado: number
  ultimosUsos: {
    reservaId: number
    clienteId: number
    monto: number
    estado: string
    creadoEn: string
  }[]
}

export interface PrevisualizacionDescuento {
  precioBase: number
  descuento: number
  precioFinal: number
  descripcion: string | null
  origen: 'PROMOCION' | 'PREMIO' | null
}

export interface ReglaPremio {
  id: number
  nombre: string
  descripcion: string | null
  reservasRequeridas: number
  tipo: TipoDescuento
  valor: number
  beneficio: string
  vigenciaDias: number
  activa: boolean
}

export interface Premio {
  id: number
  clienteId: number
  nombre: string
  tipo: TipoDescuento
  valor: number
  beneficio: string
  estado: EstadoPremio
  otorgadoEn: string
  venceEn: string
  reservaUsoId: number | null
  motivoRevocacion: string | null
}

export interface MisPremios {
  progreso: { regla: ReglaPremio; completadas: number; enCiclo: number; faltan: number }[]
  premios: Premio[]
}
