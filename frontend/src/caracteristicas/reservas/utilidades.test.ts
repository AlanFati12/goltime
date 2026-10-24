import { describe, expect, it } from 'vitest'

import type { DisponibilidadCancha, EntradaHistorial, Slot } from './tipos'
import {
  describirActor,
  describirEntrada,
  duracionesPosibles,
  sumarMinutos,
  textoDuracion,
} from './utilidades'

const entrada = (parcial: Partial<EntradaHistorial>): EntradaHistorial => ({
  id: 1,
  accion: 'CREADA',
  actorId: 1,
  actorNombre: 'Juan Pérez',
  actorRol: 'ADMIN',
  motivo: null,
  datosAnteriores: null,
  datosNuevos: null,
  fecha: '2026-11-12T20:40:00Z', // 15:40 en Bogotá
  ...parcial,
})

describe('describirEntrada', () => {
  it('describe quién reprogramó, cuándo y de qué a qué horario', () => {
    const texto = describirEntrada(
      entrada({
        accion: 'REPROGRAMADA',
        datosAnteriores: { inicio: '2026-11-14T00:00:00Z' }, // jue 13/11 19:00 Bogotá
        datosNuevos: { inicio: '2026-11-15T01:00:00Z' }, // vie 14/11 20:00 Bogotá
        motivo: 'mantenimiento',
      }),
    )
    expect(texto).toContain('Reprogramada por el administrador Juan Pérez')
    expect(texto).toContain('12/11/2026 15:40')
    expect(texto).toContain('13/11/2026 19:00')
    expect(texto).toContain('14/11/2026 20:00')
    expect(texto).toContain('Motivo: mantenimiento')
  })

  it('las acciones del sistema no llevan nombre', () => {
    expect(describirActor({ actorRol: 'SISTEMA', actorNombre: null })).toBe('el sistema')
    expect(
      describirEntrada(entrada({ accion: 'EXPIRADA', actorRol: 'SISTEMA', actorNombre: null })),
    ).toContain('Expiró el')
  })

  it('un cambio de estado del admin dice de qué estado a cuál y por qué', () => {
    const texto = describirEntrada(
      entrada({
        accion: 'ESTADO_CAMBIADO',
        datosAnteriores: { estado: 'CANCELADA' },
        datosNuevos: { estado: 'CONFIRMADA', horario: 'OCUPADA' },
        motivo: 'Se arrepintió',
      }),
    )
    expect(texto).toBe(
      'Estado cambiado por el administrador Juan Pérez el jue 12/11/2026 15:40: de «Cancelada» a «Confirmada». Motivo: Se arrepintió.',
    )
  })

  it('incluye el motivo de un rechazo o una cancelación', () => {
    expect(describirEntrada(entrada({ accion: 'RECHAZADA', motivo: 'Evento privado' }))).toContain(
      'Motivo: Evento privado',
    )
    expect(
      describirEntrada(
        entrada({ accion: 'CANCELADA', actorRol: 'CLIENTE', actorNombre: 'Ana Gómez' }),
      ),
    ).toContain('el cliente Ana Gómez')
  })
})

const slot = (hora: number, estado: Slot['estado']): Slot => ({
  inicio: `2026-11-18T${String(hora).padStart(2, '0')}:00:00Z`,
  fin: `2026-11-18T${String(hora + 1).padStart(2, '0')}:00:00Z`,
  estado,
  precio: 80000,
})

const cancha = (
  estados: Slot['estado'][],
  extra: Partial<DisponibilidadCancha> = {},
): DisponibilidadCancha => ({
  canchaId: 1,
  nombre: 'C1',
  modalidad: 'F5',
  sedeId: 1,
  sedeNombre: 'S',
  duracionBloqueMin: 60,
  duracionMinMin: 60,
  duracionMaxMin: 120,
  cerrado: false,
  slots: estados.map((e, i) => slot(i, e)),
  ...extra,
})

describe('duracionesPosibles', () => {
  it('ofrece 1 y 2 horas si hay dos bloques libres seguidos', () => {
    const c = cancha(['DISPONIBLE', 'DISPONIBLE', 'DISPONIBLE'])
    expect(duracionesPosibles(c, c.slots[0])).toEqual([60, 120])
  })

  it('se limita al máximo de la cancha', () => {
    const c = cancha(['DISPONIBLE', 'DISPONIBLE', 'DISPONIBLE'])
    expect(duracionesPosibles(c, c.slots[0])).not.toContain(180)
  })

  it('se corta donde hay un bloque ocupado', () => {
    const c = cancha(['DISPONIBLE', 'OCUPADO', 'DISPONIBLE'])
    expect(duracionesPosibles(c, c.slots[0])).toEqual([60])
    expect(duracionesPosibles(c, c.slots[1])).toEqual([])
  })

  it('respeta la duración mínima de la cancha', () => {
    const c = cancha(['DISPONIBLE', 'DISPONIBLE'], { duracionMinMin: 120 })
    expect(duracionesPosibles(c, c.slots[0])).toEqual([120])
  })
})

describe('utilidades de tiempo', () => {
  it('suma minutos y da formato a la duración', () => {
    expect(sumarMinutos('2026-11-18T00:00:00.000Z', 90)).toBe('2026-11-18T01:30:00.000Z')
    expect(textoDuracion(60)).toBe('1 h')
    expect(textoDuracion(90)).toBe('1 h 30 min')
    expect(textoDuracion(45)).toBe('45 min')
  })
})
