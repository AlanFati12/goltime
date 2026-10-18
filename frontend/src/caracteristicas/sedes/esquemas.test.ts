import { describe, expect, it } from 'vitest'

import { esquemaCancha, esquemaTarifa, numeroOpcional } from './admin/esquemas'

const cancha = {
  nombre: 'Cancha 1',
  jugadoresPorEquipo: 5,
  techada: false,
  iluminacion: true,
  precioBaseHora: 80000,
  duracionBloqueMin: 60,
  duracionMinMin: 60,
  duracionMaxMin: 120,
}

describe('esquemaCancha', () => {
  it('acepta datos válidos con opcionales vacíos', () => {
    expect(esquemaCancha.safeParse(cancha).success).toBe(true)
  })

  it('exige duraciones múltiplos del bloque y máxima >= mínima', () => {
    expect(esquemaCancha.safeParse({ ...cancha, duracionMinMin: 90 }).success).toBe(false)
    expect(
      esquemaCancha.safeParse({ ...cancha, duracionMaxMin: 30, duracionMinMin: 60 }).success,
    ).toBe(false)
  })

  it('rechaza precios negativos o con decimales', () => {
    expect(esquemaCancha.safeParse({ ...cancha, precioBaseHora: -1 }).success).toBe(false)
    expect(esquemaCancha.safeParse({ ...cancha, precioBaseHora: 80000.5 }).success).toBe(false)
  })
})

describe('esquemaTarifa', () => {
  const base = {
    diasSemana: [1, 2],
    aplicaFestivo: false,
    horaInicio: '06:00',
    horaFin: '17:00',
    precioHora: 70000,
  }

  it('exige días salvo que sea de festivo', () => {
    expect(esquemaTarifa.safeParse(base).success).toBe(true)
    expect(esquemaTarifa.safeParse({ ...base, diasSemana: [] }).success).toBe(false)
    expect(esquemaTarifa.safeParse({ ...base, diasSemana: [], aplicaFestivo: true }).success).toBe(
      true,
    )
  })

  it('exige hora final posterior y vigencia coherente', () => {
    expect(esquemaTarifa.safeParse({ ...base, horaFin: '06:00' }).success).toBe(false)
    expect(
      esquemaTarifa.safeParse({ ...base, vigenteDesde: '2026-05-02', vigenteHasta: '2026-05-01' })
        .success,
    ).toBe(false)
  })
})

describe('numeroOpcional', () => {
  it('convierte vacío en undefined', () => {
    expect(numeroOpcional('')).toBeUndefined()
    expect(numeroOpcional('12.5')).toBe(12.5)
    expect(numeroOpcional('abc')).toBeUndefined()
  })
})
