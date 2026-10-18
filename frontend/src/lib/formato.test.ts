import { describe, expect, it } from 'vitest'

import {
  aIsoBogota,
  formatearCOP,
  formatearFechaHora,
  formatearHora,
  lunesDeLaSemana,
  minutoDelDia,
  sumarDias,
} from './formato'

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

describe('fechas de la agenda', () => {
  it('convierte fecha y hora de Bogotá a UTC, incluido el cierre a las 24:00', () => {
    expect(aIsoBogota('2026-11-17', '19:00')).toBe('2026-11-18T00:00:00.000Z')
    expect(aIsoBogota('2026-11-17', '24:00')).toBe('2026-11-18T05:00:00.000Z')
    expect(aIsoBogota('2026-12-31', '24:00')).toBe('2027-01-01T05:00:00.000Z')
  })

  it('calcula el minuto del día en hora de Bogotá', () => {
    expect(minutoDelDia('2026-11-18T00:30:00Z')).toBe(19 * 60 + 30)
    expect(minutoDelDia('2026-11-18T05:00:00Z')).toBe(0)
  })

  it('suma días y encuentra el lunes de la semana', () => {
    expect(sumarDias('2026-11-30', 1)).toBe('2026-12-01')
    expect(sumarDias('2026-03-01', -1)).toBe('2026-02-28')
    expect(lunesDeLaSemana('2026-11-17')).toBe('2026-11-16') // martes -> lunes
    expect(lunesDeLaSemana('2026-11-22')).toBe('2026-11-16') // domingo -> lunes anterior
    expect(lunesDeLaSemana('2026-11-16')).toBe('2026-11-16')
  })
})
