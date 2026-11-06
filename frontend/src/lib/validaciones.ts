import { z } from 'zod'

import { MENSAJE_TELEFONO_INVALIDO, normalizarTelefono } from './telefono'

const texto = (max: number) =>
  z.string().trim().min(1, 'Campo obligatorio').max(max, `Máximo ${max} caracteres`)

const correo = z.string().trim().min(1, 'Campo obligatorio').email('Correo electrónico inválido')
/** El celular es el WhatsApp del cliente: obligatorio (Google no lo da) y siempre con indicativo (E.164). */
const telefono = z
  .string()
  .trim()
  .min(1, 'Escribe tu número de WhatsApp')
  .refine((v) => normalizarTelefono(v) !== null, MENSAJE_TELEFONO_INVALIDO)
  .transform((v) => normalizarTelefono(v) ?? v)

export const esquemaPerfil = z.object({
  nombres: texto(100),
  apellidos: texto(100),
  telefono,
  documento: z.string().trim().max(30, 'Máximo 30 caracteres').optional(),
})
export type DatosPerfil = z.infer<typeof esquemaPerfil>

export const esquemaNuevoAdmin = z.object({
  nombres: texto(100),
  apellidos: texto(100),
  correo,
})
export type DatosNuevoAdmin = z.infer<typeof esquemaNuevoAdmin>

export const esquemaMotivo = z.object({
  motivo: z
    .string()
    .trim()
    .min(5, 'Describe el motivo (mínimo 5 caracteres)')
    .max(500, 'Máximo 500 caracteres'),
})
