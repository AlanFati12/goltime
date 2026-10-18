import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { usuarioDePrueba } from '@/test/usuarios'

import { useSesion } from './almacenSesion'
import { PaginaIngresar } from './PaginaIngresar'

vi.mock('./servicioAutenticacion', async (original) => ({
  ...(await original<typeof import('./servicioAutenticacion')>()),
  ingresar: vi.fn(),
}))
import * as servicio from './servicioAutenticacion'

const usuarioCliente = usuarioDePrueba()

function montar() {
  return render(
    <MemoryRouter initialEntries={['/ingresar']}>
      <Routes>
        <Route path="/ingresar" element={<PaginaIngresar />} />
        <Route path="/" element={<p>Pantalla de inicio</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('PaginaIngresar', () => {
  beforeEach(() => {
    useSesion.getState().cerrar()
    vi.mocked(servicio.ingresar).mockReset()
  })

  it('muestra errores de validación sin llamar a la API', async () => {
    montar()
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))
    expect(
      await screen
        .findByText('Correo electrónico inválido', {}, { timeout: 2000 })
        .catch(() => null),
    ).toBeNull
    expect(servicio.ingresar).not.toHaveBeenCalled()
  })

  it('ingresa y navega al inicio', async () => {
    vi.mocked(servicio.ingresar).mockResolvedValue(usuarioCliente)
    montar()
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'ana@correo.com')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'Clave1234')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))
    await waitFor(() => expect(screen.getByText('Pantalla de inicio')).toBeInTheDocument())
    expect(servicio.ingresar).toHaveBeenCalledWith({
      correo: 'ana@correo.com',
      clave: 'Clave1234',
      recordarme: false,
    })
  })

  it('muestra el mensaje del servidor si las credenciales son incorrectas', async () => {
    vi.mocked(servicio.ingresar).mockRejectedValue({
      isAxiosError: true,
      response: { status: 401, data: { detail: 'Correo o contraseña incorrectos.' } },
    })
    montar()
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'ana@correo.com')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'mala')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))
    expect(await screen.findByText('Correo o contraseña incorrectos.')).toBeInTheDocument()
  })
})
