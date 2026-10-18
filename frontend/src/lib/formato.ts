import { es } from 'date-fns/locale'
import { formatInTimeZone, fromZonedTime } from 'date-fns-tz'

/** Zona horaria del negocio: el backend guarda UTC y el front siempre muestra hora de Bogotá. */
export const ZONA_HORARIA = 'America/Bogota'

const formateadorCOP = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
})

/** 80000 -> "$ 80.000" (COP sin decimales). */
export function formatearCOP(valor: number): string {
  return formateadorCOP.format(valor).replace(/ /g, ' ')
}

/** Instante ISO (UTC) -> "jue 13/11/2026 19:00" en hora de Bogotá. */
export function formatearFechaHora(iso: string | Date): string {
  return formatInTimeZone(iso, ZONA_HORARIA, 'EEE dd/MM/yyyy HH:mm', { locale: es })
}

/** Instante ISO (UTC) -> "19:00" en hora de Bogotá. */
export function formatearHora(iso: string | Date): string {
  return formatInTimeZone(iso, ZONA_HORARIA, 'HH:mm')
}

/** Fecha de hoy en Bogotá como "2026-11-17" (para los campos de fecha). */
export function hoyEnBogota(): string {
  return formatInTimeZone(new Date(), ZONA_HORARIA, 'yyyy-MM-dd')
}

/** "2026-11-17" -> "mar 17 nov". */
export function formatearDia(fechaISO: string): string {
  const [a, m, d] = fechaISO.split('-').map(Number)
  return formatInTimeZone(new Date(Date.UTC(a, m - 1, d, 12)), 'UTC', 'EEE d MMM', { locale: es })
}

/** Instante -> "2026-11-17" en hora de Bogotá. */
export function fechaEnBogota(iso: string | Date): string {
  return formatInTimeZone(iso, ZONA_HORARIA, 'yyyy-MM-dd')
}

/**
 * Fecha ("2026-11-17") y hora ("19:00" o "24:00") de Bogotá -> instante ISO en UTC.
 * "24:00" es la medianoche al final del día (el día siguiente a las 00:00).
 */
export function aIsoBogota(fecha: string, hora: string): string {
  if (hora === '24:00') {
    const [a, m, d] = fecha.split('-').map(Number)
    const siguiente = new Date(Date.UTC(a, m - 1, d + 1)).toISOString().slice(0, 10)
    return fromZonedTime(`${siguiente}T00:00:00`, ZONA_HORARIA).toISOString()
  }
  return fromZonedTime(`${fecha}T${hora}:00`, ZONA_HORARIA).toISOString()
}

/** Minuto del día (0..1439) de un instante, en hora de Bogotá. */
export function minutoDelDia(iso: string | Date): number {
  const [h, m] = formatInTimeZone(iso, ZONA_HORARIA, 'HH:mm').split(':').map(Number)
  return h * 60 + m
}

/** "2026-11-17" + n días. */
export function sumarDias(fechaISO: string, dias: number): string {
  const [a, m, d] = fechaISO.split('-').map(Number)
  return new Date(Date.UTC(a, m - 1, d + dias)).toISOString().slice(0, 10)
}

/** Lunes de la semana de una fecha ("2026-11-17" -> "2026-11-16"). */
export function lunesDeLaSemana(fechaISO: string): string {
  const [a, m, d] = fechaISO.split('-').map(Number)
  const diaSemana = new Date(Date.UTC(a, m - 1, d)).getUTCDay() // 0 = domingo
  return sumarDias(fechaISO, -((diaSemana + 6) % 7))
}
