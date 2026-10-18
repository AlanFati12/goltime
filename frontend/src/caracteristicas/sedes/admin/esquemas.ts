import { z } from 'zod'

/** Lee un campo numérico opcional: vacío = undefined. */
export const numeroOpcional = (v: unknown) =>
  v === '' || v === null || v === undefined || Number.isNaN(Number(v)) ? undefined : Number(v)

const texto = (max: number, mensaje = 'Campo obligatorio') =>
  z.string().trim().min(1, mensaje).max(max, `Máximo ${max} caracteres`)

export const esquemaSede = z.object({
  nombre: texto(120),
  direccion: texto(200),
  ciudad: texto(80),
  telefono: z.string().trim().max(30, 'Máximo 30 caracteres').optional(),
  whatsapp: z.string().trim().max(30, 'Máximo 30 caracteres').optional(),
  descripcion: z.string().trim().max(2000, 'Máximo 2000 caracteres').optional(),
  /** Separados por coma, ej. "Parqueadero, Camerinos". */
  servicios: z.string().max(1000, 'Demasiado largo'),
  latitud: z.number().min(-90, 'Entre -90 y 90').max(90, 'Entre -90 y 90').optional(),
  longitud: z.number().min(-180, 'Entre -180 y 180').max(180, 'Entre -180 y 180').optional(),
})
export type DatosSedeFormulario = z.infer<typeof esquemaSede>

export const esquemaCancha = z
  .object({
    nombre: texto(80),
    jugadoresPorEquipo: z.number('Elige la modalidad').int().min(2).max(11),
    capacidadTotal: z.number().int().min(4, 'Mínimo 4').max(40, 'Máximo 40').optional(),
    superficie: z.string().trim().max(60, 'Máximo 60 caracteres').optional(),
    largoM: z.number().min(1, 'Debe ser positivo').max(200, 'Máximo 200').optional(),
    anchoM: z.number().min(1, 'Debe ser positivo').max(200, 'Máximo 200').optional(),
    techada: z.boolean(),
    iluminacion: z.boolean(),
    precioBaseHora: z
      .number('Ingresa el precio')
      .int('Sin decimales')
      .min(0, 'No puede ser negativo')
      .max(100_000_000, 'Valor demasiado alto'),
    duracionBloqueMin: z.number().int().min(15).max(240),
    duracionMinMin: z.number().int().min(15),
    duracionMaxMin: z.number().int().min(15),
    descripcion: z.string().trim().max(2000, 'Máximo 2000 caracteres').optional(),
    orden: z.number().int().optional(),
  })
  .refine((d) => d.duracionMinMin % d.duracionBloqueMin === 0, {
    path: ['duracionMinMin'],
    message: 'Debe ser múltiplo del bloque',
  })
  .refine((d) => d.duracionMaxMin % d.duracionBloqueMin === 0, {
    path: ['duracionMaxMin'],
    message: 'Debe ser múltiplo del bloque',
  })
  .refine((d) => d.duracionMaxMin >= d.duracionMinMin, {
    path: ['duracionMaxMin'],
    message: 'Debe ser mayor o igual a la mínima',
  })
export type DatosCanchaFormulario = z.infer<typeof esquemaCancha>

export const esquemaTarifa = z
  .object({
    diasSemana: z.array(z.number()),
    aplicaFestivo: z.boolean(),
    horaInicio: z.string(),
    horaFin: z.string(),
    precioHora: z
      .number('Ingresa el precio')
      .int('Sin decimales')
      .min(0, 'No puede ser negativo')
      .max(100_000_000, 'Valor demasiado alto'),
    vigenteDesde: z.string().optional(),
    vigenteHasta: z.string().optional(),
  })
  .refine((d) => d.aplicaFestivo || d.diasSemana.length > 0, {
    path: ['diasSemana'],
    message: 'Elige al menos un día o marca "Festivos"',
  })
  .refine((d) => d.horaFin > d.horaInicio, {
    path: ['horaFin'],
    message: 'La hora final debe ser posterior',
  })
  .refine((d) => !d.vigenteDesde || !d.vigenteHasta || d.vigenteHasta >= d.vigenteDesde, {
    path: ['vigenteHasta'],
    message: 'No puede ser anterior al inicio',
  })
export type DatosTarifaFormulario = z.infer<typeof esquemaTarifa>

export const esquemaFestivo = z.object({
  fecha: z.string().min(1, 'Elige la fecha'),
  nombre: texto(100),
})
