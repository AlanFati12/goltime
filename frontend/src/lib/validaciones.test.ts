import { describe, expect, it } from 'vitest'

import { esquemaNuevoAdmin, esquemaPerfil } from './validaciones'

const perfilValido = { nombres: 'Ana', apellidos: 'Pérez', telefono: '3001234567' }

describe('esquemaPerfil (completar perfil y editarlo)', () => {
  it('acepta nombres, apellidos y WhatsApp; el documento es opcional', () => {
    expect(esquemaPerfil.safeParse(perfilValido).success).toBe(true)
    expect(esquemaPerfil.safeParse({ ...perfilValido, documento: '1075123456' }).success).toBe(true)
  })

  it('exige el WhatsApp y lo entrega con indicativo', () => {
    const r = esquemaPerfil.safeParse({ ...perfilValido, telefono: '300 123 4567' })
    expect(r.success && r.data.telefono).toBe('+573001234567')
    expect(esquemaPerfil.safeParse({ ...perfilValido, telefono: '' }).success).toBe(false)
    expect(esquemaPerfil.safeParse({ ...perfilValido, telefono: '8712345' }).success).toBe(false)
  })

  it('exige los apellidos (Google no siempre los da)', () => {
    expect(esquemaPerfil.safeParse({ ...perfilValido, apellidos: '  ' }).success).toBe(false)
  })
})

describe('esquemaNuevoAdmin', () => {
  it('autoriza a una persona solo con su nombre y el correo de su cuenta de Google', () => {
    expect(
      esquemaNuevoAdmin.safeParse({
        nombres: 'Luisa',
        apellidos: 'Rojas',
        correo: 'luisa@gmail.com',
      }).success,
    ).toBe(true)
    expect(
      esquemaNuevoAdmin.safeParse({ nombres: 'Luisa', apellidos: 'Rojas', correo: 'no-es-correo' })
        .success,
    ).toBe(false)
  })
})
