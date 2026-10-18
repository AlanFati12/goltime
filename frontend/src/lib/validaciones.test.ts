import { describe, expect, it } from 'vitest'

import { esquemaCambioClave, esquemaClave, esquemaRegistro, fortalezaClave } from './validaciones'

const registroValido = {
  nombres: 'Ana',
  apellidos: 'Pérez',
  correo: 'ana@correo.com',
  telefono: '3001234567',
  clave: 'Clave1234',
  confirmacionClave: 'Clave1234',
  aceptaTerminos: true,
}

describe('esquemaClave', () => {
  it('exige 8 caracteres, una letra y un número', () => {
    expect(esquemaClave.safeParse('Clave1234').success).toBe(true)
    expect(esquemaClave.safeParse('corta1').success).toBe(false)
    expect(esquemaClave.safeParse('sololetrasaqui').success).toBe(false)
    expect(esquemaClave.safeParse('123456789').success).toBe(false)
  })
})

describe('esquemaRegistro', () => {
  it('acepta un registro válido', () => {
    expect(esquemaRegistro.safeParse(registroValido).success).toBe(true)
  })

  it('rechaza contraseñas que no coinciden', () => {
    const r = esquemaRegistro.safeParse({ ...registroValido, confirmacionClave: 'Otra12345' })
    expect(r.success).toBe(false)
    expect(r.error?.issues[0].path).toEqual(['confirmacionClave'])
  })

  it('exige aceptar términos y un correo válido', () => {
    expect(esquemaRegistro.safeParse({ ...registroValido, aceptaTerminos: false }).success).toBe(
      false,
    )
    expect(esquemaRegistro.safeParse({ ...registroValido, correo: 'no-es-correo' }).success).toBe(
      false,
    )
  })
})

describe('esquemaCambioClave', () => {
  it('valida la política y la confirmación', () => {
    expect(
      esquemaCambioClave.safeParse({
        claveActual: 'x',
        claveNueva: 'Nueva5678',
        confirmacion: 'Nueva5678',
      }).success,
    ).toBe(true)
    expect(
      esquemaCambioClave.safeParse({
        claveActual: 'x',
        claveNueva: 'Nueva5678',
        confirmacion: 'distinta1',
      }).success,
    ).toBe(false)
  })
})

describe('fortalezaClave', () => {
  it('sube el puntaje con la variedad y la longitud', () => {
    expect(fortalezaClave('').puntaje).toBe(0)
    expect(fortalezaClave('abc').puntaje).toBeLessThan(fortalezaClave('Clave1234').puntaje)
    expect(fortalezaClave('Clave1234').puntaje).toBeLessThan(
      fortalezaClave('Clave1234!Larga').puntaje,
    )
    expect(fortalezaClave('Clave1234!Larga').etiqueta).toBe('Excelente')
  })
})
