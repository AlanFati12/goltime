import { formatearFechaHora, formatearHora } from '@/lib/formato'

import type { DisponibilidadCancha, EntradaHistorial, EstadoReserva, Slot } from './tipos'

export const ETIQUETA_ESTADO_RESERVA: Record<EstadoReserva, string> = {
  PENDIENTE: 'Pendiente de aprobación',
  CONFIRMADA: 'Confirmada',
  RECHAZADA: 'Rechazada',
  EXPIRADA: 'Expirada',
  CANCELADA: 'Cancelada',
  COMPLETADA: 'Completada',
  NO_ASISTIO: 'No asistió',
}

export const TONO_ESTADO_RESERVA: Record<EstadoReserva, 'verde' | 'ambar' | 'rojo' | 'gris'> = {
  PENDIENTE: 'ambar',
  CONFIRMADA: 'verde',
  RECHAZADA: 'rojo',
  EXPIRADA: 'gris',
  CANCELADA: 'rojo',
  COMPLETADA: 'verde',
  NO_ASISTIO: 'rojo',
}

export const ETIQUETA_PAGO: Record<string, string> = {
  PENDIENTE: 'Pago pendiente',
  ABONADO: 'Abonado',
  PAGADO: 'Pagado',
  REEMBOLSO_PENDIENTE: 'Reembolso pendiente',
  REEMBOLSADO: 'Reembolsado',
}

const ROL: Record<EntradaHistorial['actorRol'], string> = {
  ADMIN: 'el administrador',
  CLIENTE: 'el cliente',
  SISTEMA: 'el sistema',
}

/** Quién hizo la acción: "el administrador Juan Pérez", "el cliente Ana Gómez" o "el sistema". */
export function describirActor(h: Pick<EntradaHistorial, 'actorRol' | 'actorNombre'>): string {
  return h.actorRol === 'SISTEMA' || !h.actorNombre
    ? ROL[h.actorRol]
    : `${ROL[h.actorRol]} ${h.actorNombre}`
}

/**
 * Frase de la línea de tiempo de una reserva: quién hizo qué, cuándo y por qué. Ej.:
 * "Reprogramada por el cliente Ana Gómez el jue 12/11/2026 15:40 — de vie 13/11/2026 19:00 a sáb 14/11/2026 20:00."
 */
export function describirEntrada(h: EntradaHistorial): string {
  const actor = describirActor(h)
  const cuando = formatearFechaHora(h.fecha)
  const motivo = h.motivo ? ` Motivo: ${h.motivo}.` : ''
  switch (h.accion) {
    case 'CREADA':
      return `Solicitada por ${actor} el ${cuando}.`
    case 'APROBADA':
      return `Aprobada por ${actor} el ${cuando}.`
    case 'RECHAZADA':
      return `Rechazada por ${actor} el ${cuando}.${motivo}`
    case 'EXPIRADA':
      return `Expiró el ${cuando} porque nadie respondió a tiempo.`
    case 'REPROGRAMADA': {
      const antes = h.datosAnteriores?.inicio as string | undefined
      const despues = h.datosNuevos?.inicio as string | undefined
      const cambio =
        antes && despues
          ? ` — de ${formatearFechaHora(antes)} a ${formatearFechaHora(despues)}`
          : ''
      return `Reprogramada por ${actor} el ${cuando}${cambio}.${motivo}`
    }
    case 'CANCELADA':
      return `Cancelada por ${actor} el ${cuando}.${motivo}`
    case 'COMPLETADA':
      return `Marcada como completada el ${cuando}.`
    case 'NO_ASISTIO':
      return `Marcada como no asistió por ${actor} el ${cuando}.`
    case 'PAGO_REGISTRADO':
      return `Pago registrado por ${actor} el ${cuando}.${motivo}`
    default:
      return `${h.accion} por ${actor} el ${cuando}.${motivo}`
  }
}

/** Hasta cuántos bloques seguidos libres hay desde el slot elegido (respetando el máximo de la cancha). */
export function duracionesPosibles(cancha: DisponibilidadCancha, desde: Slot): number[] {
  const indice = cancha.slots.findIndex((s) => s.inicio === desde.inicio)
  if (indice < 0) return []
  let libres = 0
  while (
    indice + libres < cancha.slots.length &&
    cancha.slots[indice + libres].estado === 'DISPONIBLE'
  )
    libres++
  const duraciones: number[] = []
  for (let bloques = 1; bloques <= libres; bloques++) {
    const minutos = bloques * cancha.duracionBloqueMin
    if (minutos > cancha.duracionMaxMin) break
    if (minutos >= cancha.duracionMinMin) duraciones.push(minutos)
  }
  return duraciones
}

export function sumarMinutos(iso: string, minutos: number): string {
  return new Date(new Date(iso).getTime() + minutos * 60_000).toISOString()
}

/** "19:00 – 20:00" en hora de Bogotá. */
export function rangoHorario(inicio: string, fin: string): string {
  return `${formatearHora(inicio)} – ${formatearHora(fin)}`
}

export function textoDuracion(minutos: number): string {
  const h = Math.floor(minutos / 60)
  const m = minutos % 60
  if (h === 0) return `${m} min`
  return m === 0 ? `${h} h` : `${h} h ${m} min`
}
