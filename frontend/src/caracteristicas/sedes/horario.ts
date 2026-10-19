import { hoyEnBogota, minutoDelDia } from '@/lib/formato'

import type { HorarioDia, Sede } from './tipos'

/** Día ISO (1 = lunes … 7 = domingo) de hoy en Bogotá. */
export function diaIsoDeHoy(): number {
  const [a, m, d] = hoyEnBogota().split('-').map(Number)
  const dia = new Date(Date.UTC(a, m - 1, d)).getUTCDay()
  return dia === 0 ? 7 : dia
}

const aMinutos = (hhmm: string) => {
  const [h, m] = hhmm.split(':').map(Number)
  return h * 60 + m
}

/**
 * Lectura del horario publicado de hoy: si la sede está abierta en este momento y hasta qué hora.
 * Solo describe el horario de atención; la disponibilidad de canchas la decide el backend.
 */
export function horarioDeHoy(horario: HorarioDia[]): {
  abiertaAhora: boolean
  texto: string
} {
  const hoy = horario.find((h) => h.diaSemana === diaIsoDeHoy())
  if (!hoy || hoy.cerrado || !hoy.apertura || !hoy.cierre) {
    return { abiertaAhora: false, texto: 'Hoy cerrada' }
  }
  const ahora = minutoDelDia(new Date())
  const abiertaAhora = ahora >= aMinutos(hoy.apertura) && ahora < aMinutos(hoy.cierre)
  return {
    abiertaAhora,
    texto: abiertaAhora
      ? `Abierta hasta las ${hoy.cierre}`
      : `Hoy de ${hoy.apertura} a ${hoy.cierre}`,
  }
}

/** Precio base por hora más bajo entre las canchas activas de la sede (lo publica el backend). */
export function precioDesde(sede: Sede): number | null {
  const precios = sede.canchas.filter((c) => c.estado === 'ACTIVA').map((c) => c.precioBaseHora)
  return precios.length ? Math.min(...precios) : null
}

/** Modalidades distintas que ofrece la sede, en orden de aparición. */
export function modalidadesDe(sede: Sede): string[] {
  return [...new Set(sede.canchas.filter((c) => c.estado === 'ACTIVA').map((c) => c.modalidad))]
}
