import { describe, expect, it } from 'vitest'

import { formatearCOP, formatearFechaHora, formatearHora } from './formato'

describe('formatearCOP', () => {
  it('muestra pesos colombianos sin decimales', () => {
    expect(formatearCOP(80000)).toBe('$ 80.000')
    expect(formatearCOP(1250000)).toBe('$ 1.250.000')
  })
})

describe('fechas en hora de Bogotá', () => {
  it('convierte de UTC a America/Bogota', () => {
    // 00:00 UTC del 20/nov equivale a las 19:00 del 19/nov en Bogotá.
    expect(formatearHora('2026-11-20T00:00:00Z')).toBe('19:00')
    expect(formatearFechaHora('2026-11-20T00:00:00Z')).toContain('19/11/2026 19:00')
  })
})
