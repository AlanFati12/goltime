import { describe, expect, it } from 'vitest'

import { rutaDeNotificacion } from './CampanaNotificaciones'

describe('rutaDeNotificacion', () => {
  it('lleva al cliente al detalle de su reserva y al admin al listado de reservas', () => {
    expect(rutaDeNotificacion({ entidad: 'reserva', entidadId: 7 }, 'CLIENTE')).toBe(
      '/mis-reservas/7',
    )
    expect(rutaDeNotificacion({ entidad: 'reserva', entidadId: 7 }, 'ADMIN')).toBe(
      '/admin/reservas',
    )
  })

  it('no navega si la notificación no enlaza a nada', () => {
    expect(rutaDeNotificacion({ entidad: null, entidadId: null }, 'CLIENTE')).toBeNull()
  })
})
