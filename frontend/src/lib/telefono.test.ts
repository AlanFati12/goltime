import { describe, expect, it } from 'vitest'

import { formatearTelefono, normalizarTelefono, separarTelefono } from './telefono'

describe('normalizarTelefono', () => {
  it('deja igual todas las formas del mismo celular colombiano', () => {
    for (const texto of [
      '3001234567',
      '300 123 4567',
      '+57 300 123 4567',
      '573001234567',
      '0057 300-123-4567',
    ]) {
      expect(normalizarTelefono(texto)).toBe('+573001234567')
    }
  })

  it('acepta otros países con indicativo y rechaza lo que no es celular', () => {
    expect(normalizarTelefono('+58 412 555 1234')).toBe('+584125551234')
    expect(normalizarTelefono('8712345')).toBeNull()
    expect(normalizarTelefono('+57 601 876 5432')).toBeNull()
    expect(normalizarTelefono('')).toBeNull()
  })
})

describe('separarTelefono y formatearTelefono', () => {
  it('reconoce el indicativo más largo que coincide', () => {
    expect(separarTelefono('+593991234567')).toEqual({ indicativo: '593', numero: '991234567' })
    expect(separarTelefono('+573001234567')).toEqual({ indicativo: '57', numero: '3001234567' })
    expect(separarTelefono('3001234567')).toEqual({ indicativo: '57', numero: '3001234567' })
  })

  it('agrupa los celulares colombianos para leerlos', () => {
    expect(formatearTelefono('+573001234567')).toBe('+57 300 123 4567')
    expect(formatearTelefono('+584125551234')).toBe('+584125551234')
    expect(formatearTelefono(null)).toBe('')
  })
})
