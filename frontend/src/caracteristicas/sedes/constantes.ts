/** Nombres de los días según ISO-8601 (1 = lunes). */
export const NOMBRES_DIAS = [
  'Lunes',
  'Martes',
  'Miércoles',
  'Jueves',
  'Viernes',
  'Sábado',
  'Domingo',
]
export const DIAS_CORTOS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

/** Modalidades por defecto (jugadores por equipo). La API valida contra la configuración del negocio. */
export const MODALIDADES = [5, 6, 7, 8, 9, 11]

export const ETIQUETA_ESTADO = {
  ACTIVA: 'Activa',
  MANTENIMIENTO: 'En mantenimiento',
  INACTIVA: 'Inactiva',
} as const

/** Horas cada 30 minutos de 00:00 a 24:00 para los selectores de horario. */
export const HORAS: string[] = Array.from({ length: 49 }, (_, i) => {
  const minutos = i * 30
  return `${String(Math.floor(minutos / 60)).padStart(2, '0')}:${String(minutos % 60).padStart(2, '0')}`
})
