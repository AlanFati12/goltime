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

const DIAS = ['dom', 'lun', 'mar', 'mié', 'jue', 'vie', 'sáb']
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']
const SEMANA_EN = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
/** Colombia no tiene horario de verano: Bogotá es siempre UTC-5. */
const DESFASE_BOGOTA = '-05:00'

// Un solo formateador reutilizado: crear uno nuevo en cada llamada es lo más caro de Intl.
const formateadorBogota = new Intl.DateTimeFormat('en-US', {
  timeZone: ZONA_HORARIA,
  hourCycle: 'h23',
  weekday: 'short',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

interface PartesFecha {
  anio: string
  mes: string
  dia: string
  hora: string
  minuto: string
  /** 0 = domingo. */
  diaSemana: number
}

/** Descompone un instante en sus partes según la hora de Bogotá. */
function partesBogota(instante: string | Date): PartesFecha {
  const p: Record<string, string> = {}
  for (const parte of formateadorBogota.formatToParts(new Date(instante)))
    p[parte.type] = parte.value
  return {
    anio: p.year,
    mes: p.month,
    dia: p.day,
    hora: p.hour === '24' ? '00' : p.hour,
    minuto: p.minute,
    diaSemana: SEMANA_EN.indexOf(p.weekday),
  }
}

/** Instante ISO (UTC) -> "jue 13/11/2026 19:00" en hora de Bogotá. */
export function formatearFechaHora(iso: string | Date): string {
  const f = partesBogota(iso)
  return `${DIAS[f.diaSemana]} ${f.dia}/${f.mes}/${f.anio} ${f.hora}:${f.minuto}`
}

/** Instante ISO (UTC) -> "19:00" en hora de Bogotá. */
export function formatearHora(iso: string | Date): string {
  const f = partesBogota(iso)
  return `${f.hora}:${f.minuto}`
}

/** Instante ISO (UTC) -> "jue", día de la semana abreviado en hora de Bogotá. */
export function diaSemanaCorto(iso: string | Date): string {
  return DIAS[partesBogota(iso).diaSemana]
}

/** Instante ISO (UTC) -> "13", día del mes sin cero a la izquierda, en hora de Bogotá. */
export function diaDelMes(iso: string | Date): string {
  return String(Number(partesBogota(iso).dia))
}

/** Fecha de hoy en Bogotá como "2026-11-17" (para los campos de fecha). */
export function hoyEnBogota(): string {
  return fechaEnBogota(new Date())
}

/** "2026-11-17" -> "mar 17 nov". */
export function formatearDia(fechaISO: string): string {
  const [a, m, d] = fechaISO.split('-').map(Number)
  const dia = new Date(Date.UTC(a, m - 1, d, 12))
  return `${DIAS[dia.getUTCDay()]} ${dia.getUTCDate()} ${MESES[dia.getUTCMonth()]}`
}

/** Instante -> "2026-11-17" en hora de Bogotá. */
export function fechaEnBogota(iso: string | Date): string {
  const f = partesBogota(iso)
  return `${f.anio}-${f.mes}-${f.dia}`
}

/** Fecha y hora locales de Bogotá ("2026-11-17T19:00:00") -> instante en UTC. */
export function desdeBogota(fechaHoraLocal: string): Date {
  return new Date(`${fechaHoraLocal}${DESFASE_BOGOTA}`)
}

/**
 * Fecha ("2026-11-17") y hora ("19:00" o "24:00") de Bogotá -> instante ISO en UTC.
 * "24:00" es la medianoche al final del día (el día siguiente a las 00:00).
 */
export function aIsoBogota(fecha: string, hora: string): string {
  if (hora === '24:00') return desdeBogota(`${sumarDias(fecha, 1)}T00:00:00`).toISOString()
  return desdeBogota(`${fecha}T${hora}:00`).toISOString()
}

/** Minuto del día (0..1439) de un instante, en hora de Bogotá. */
export function minutoDelDia(iso: string | Date): number {
  const f = partesBogota(iso)
  return Number(f.hora) * 60 + Number(f.minuto)
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
