/**
 * Teléfonos de WhatsApp en formato internacional E.164 (+573001234567), con la misma regla del backend
 * (NumeroTelefono): así 3001234567 y +57 300 123 4567 son el mismo número para el bot.
 */

export interface Indicativo {
  codigo: string
  pais: string
  bandera: string
}

/** Colombia primero (el negocio está en Neiva); luego los países de donde más viene gente. */
export const INDICATIVOS: Indicativo[] = [
  { codigo: '57', pais: 'Colombia', bandera: '🇨🇴' },
  { codigo: '58', pais: 'Venezuela', bandera: '🇻🇪' },
  { codigo: '593', pais: 'Ecuador', bandera: '🇪🇨' },
  { codigo: '51', pais: 'Perú', bandera: '🇵🇪' },
  { codigo: '507', pais: 'Panamá', bandera: '🇵🇦' },
  { codigo: '52', pais: 'México', bandera: '🇲🇽' },
  { codigo: '54', pais: 'Argentina', bandera: '🇦🇷' },
  { codigo: '56', pais: 'Chile', bandera: '🇨🇱' },
  { codigo: '55', pais: 'Brasil', bandera: '🇧🇷' },
  { codigo: '34', pais: 'España', bandera: '🇪🇸' },
  { codigo: '1', pais: 'Estados Unidos / Canadá', bandera: '🇺🇸' },
]

export const INDICATIVO_COLOMBIA = '57'

const E164 = /^\+[1-9]\d{7,14}$/
const CELULAR_COLOMBIA = /^3\d{9}$/

/** Convierte lo escrito a E.164 o devuelve null si no es un celular válido. */
export function normalizarTelefono(texto: string): string | null {
  let limpio = texto.trim().replace(/[\s().-]/g, '')
  if (limpio.startsWith('00')) limpio = '+' + limpio.slice(2)
  if (!limpio.startsWith('+')) {
    if (CELULAR_COLOMBIA.test(limpio)) limpio = '+57' + limpio
    else if (/^573\d{9}$/.test(limpio)) limpio = '+' + limpio
    else return null
  }
  if (!E164.test(limpio)) return null
  if (limpio.startsWith('+57') && !CELULAR_COLOMBIA.test(limpio.slice(3))) return null
  return limpio
}

/** Separa un número en indicativo y número nacional (el indicativo conocido más largo que coincida). */
export function separarTelefono(valor: string | null | undefined): {
  indicativo: string
  numero: string
} {
  const limpio = (valor ?? '').trim().replace(/[\s().-]/g, '')
  if (limpio.startsWith('+')) {
    const digitos = limpio.slice(1)
    const conocido = [...INDICATIVOS]
      .sort((a, b) => b.codigo.length - a.codigo.length)
      .find((i) => digitos.startsWith(i.codigo))
    if (conocido)
      return { indicativo: conocido.codigo, numero: digitos.slice(conocido.codigo.length) }
  }
  return { indicativo: INDICATIVO_COLOMBIA, numero: limpio.replace(/^\+/, '') }
}

/** +573001234567 -> "+57 300 123 4567"; otros países y valores no normalizados quedan tal cual. */
export function formatearTelefono(valor: string | null | undefined): string {
  if (!valor) return ''
  const m = /^\+57(3\d{2})(\d{3})(\d{4})$/.exec(valor)
  return m ? `+57 ${m[1]} ${m[2]} ${m[3]}` : valor
}

export const MENSAJE_TELEFONO_INVALIDO =
  'Escribe un celular válido. En Colombia son 10 dígitos y empiezan por 3.'
