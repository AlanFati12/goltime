import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useSesion } from './almacenSesion'
import { restaurarSesion } from './servicioSesion'

vi.mock('axios')

describe('restaurarSesion', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.mocked(axios.post).mockReset()
    useSesion.setState({ tokenAcceso: null, usuario: null, inicializada: false })
  })

  it('un visitante sin rastro de sesión no hace ninguna petición y la app queda lista', async () => {
    await restaurarSesion()
    expect(axios.post).not.toHaveBeenCalled()
    expect(useSesion.getState().inicializada).toBe(true)
  })

  it('con rastro de sesión intenta renovarla; si falla, queda de visitante y borra el rastro', async () => {
    localStorage.setItem('goltime:sesion', '1')
    vi.mocked(axios.post).mockRejectedValue(new Error('401'))
    await restaurarSesion()
    expect(axios.post).toHaveBeenCalledOnce()
    expect(useSesion.getState().inicializada).toBe(true)
    expect(useSesion.getState().usuario).toBeNull()
    expect(localStorage.getItem('goltime:sesion')).toBeNull()
  })

  it('al iniciar sesión se deja el rastro, y al cerrarla se borra', () => {
    useSesion.getState().iniciar('token', { id: 1 } as never)
    expect(localStorage.getItem('goltime:sesion')).toBe('1')
    useSesion.getState().cerrar()
    expect(localStorage.getItem('goltime:sesion')).toBeNull()
  })
})
