import { es } from 'date-fns/locale'
import { formatInTimeZone } from 'date-fns-tz'

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
