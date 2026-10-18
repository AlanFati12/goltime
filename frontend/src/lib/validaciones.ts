import { z } from 'zod'

const texto = (max: number) =>
  z.string().trim().min(1, 'Campo obligatorio').max(max, `Máximo ${max} caracteres`)

/** Misma política que el backend: mínimo 8, al menos una letra y un número. */
export const esquemaClave = z
  .string()
  .min(8, 'Mínimo 8 caracteres')
  .max(72, 'Máximo 72 caracteres')
  .regex(/[A-Za-zÁÉÍÓÚáéíóúÑñ]/, 'Incluye al menos una letra')
  .regex(/\d/, 'Incluye al menos un número')

const correo = z.string().trim().min(1, 'Campo obligatorio').email('Correo electrónico inválido')
const telefono = z
  .string()
  .trim()
  .regex(/^[0-9+\s-]{7,20}$/, 'Teléfono inválido (solo números, mínimo 7 dígitos)')

export const esquemaRegistro = z
  .object({
    nombres: texto(100),
    apellidos: texto(100),
    correo,
    telefono,
    documento: z.string().trim().max(30, 'Máximo 30 caracteres').optional(),
    clave: esquemaClave,
    confirmacionClave: z.string().min(1, 'Confirma tu contraseña'),
    aceptaTerminos: z
      .boolean()
      .refine((v) => v, 'Debes aceptar los términos y el tratamiento de datos'),
  })
  .refine((d) => d.clave === d.confirmacionClave, {
    path: ['confirmacionClave'],
    message: 'Las contraseñas no coinciden',
  })
export type DatosRegistro = z.infer<typeof esquemaRegistro>

export const esquemaIngreso = z.object({
  correo,
  clave: z.string().min(1, 'Ingresa tu contraseña'),
  recordarme: z.boolean(),
})
export type DatosIngreso = z.infer<typeof esquemaIngreso>

export const esquemaOlvideClave = z.object({ correo })
export type DatosOlvideClave = z.infer<typeof esquemaOlvideClave>

export const esquemaRestablecer = z
  .object({ claveNueva: esquemaClave, confirmacion: z.string().min(1, 'Confirma tu contraseña') })
  .refine((d) => d.claveNueva === d.confirmacion, {
    path: ['confirmacion'],
    message: 'Las contraseñas no coinciden',
  })
export type DatosRestablecer = z.infer<typeof esquemaRestablecer>

export const esquemaPerfil = z.object({
  nombres: texto(100),
  apellidos: texto(100),
  telefono,
  documento: z.string().trim().max(30, 'Máximo 30 caracteres').optional(),
})
export type DatosPerfil = z.infer<typeof esquemaPerfil>

export const esquemaCambioClave = z
  .object({
    claveActual: z.string().min(1, 'Ingresa tu contraseña actual'),
    claveNueva: esquemaClave,
    confirmacion: z.string().min(1, 'Confirma tu contraseña'),
  })
  .refine((d) => d.claveNueva === d.confirmacion, {
    path: ['confirmacion'],
    message: 'Las contraseñas no coinciden',
  })
export type DatosCambioClave = z.infer<typeof esquemaCambioClave>

export const esquemaNuevoAdmin = z.object({
  nombres: texto(100),
  apellidos: texto(100),
  correo,
  clave: esquemaClave,
})
export type DatosNuevoAdmin = z.infer<typeof esquemaNuevoAdmin>

export const esquemaMotivo = z.object({
  motivo: z
    .string()
    .trim()
    .min(5, 'Describe el motivo (mínimo 5 caracteres)')
    .max(500, 'Máximo 500 caracteres'),
})

export type NivelClave = { puntaje: 0 | 1 | 2 | 3 | 4; etiqueta: string }

/** Indicador de fortaleza de la contraseña (solo orientativo; la política real la valida el servidor). */
export function fortalezaClave(clave: string): NivelClave {
  if (!clave) return { puntaje: 0, etiqueta: '' }
  let puntos = 0
  if (clave.length >= 8) puntos++
  if (/[A-Za-z]/.test(clave) && /\d/.test(clave)) puntos++
  if (/[a-z]/.test(clave) && /[A-Z]/.test(clave)) puntos++
  if (/[^A-Za-z0-9]/.test(clave) || clave.length >= 12) puntos++
  const puntaje = Math.min(puntos, 4) as 0 | 1 | 2 | 3 | 4
  return { puntaje, etiqueta: ['Muy débil', 'Débil', 'Aceptable', 'Buena', 'Excelente'][puntaje] }
}
