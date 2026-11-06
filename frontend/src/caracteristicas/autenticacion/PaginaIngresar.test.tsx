import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { usuarioDePrueba } from '@/test/usuarios'

import { useSesion } from './almacenSesion'
import { PaginaIngresar } from './PaginaIngresar'

vi.mock('./BotonGoogle', () => ({
  BotonGoogle: ({ clientId }: { clientId: string }) => <p>Botón de Google para {clientId}</p>,
}))
vi.mock('./servicioAutenticacion', async (original) => ({
  ...(await original<typeof import('./servicioAutenticacion')>()),
  cargarConfiguracionIngreso: vi.fn(),
  ingresarConGoogle: vi.fn(),
}))
import * as servicio from './servicioAutenticacion'

function montar() {
  const cliente = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={cliente}>
      <MemoryRouter initialEntries={['/ingresar']}>
        <Routes>
          <Route path="/ingresar" element={<PaginaIngresar />} />
          <Route path="/" element={<p>Pantalla de inicio</p>} />
          <Route path="/completar-perfil" element={<p>Completa tu perfil</p>} />
          <Route path="/admin" element={<p>Panel</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('PaginaIngresar', () => {
  beforeEach(() => {
    useSesion.getState().cerrar()
    vi.mocked(servicio.cargarConfiguracionIngreso).mockReset()
    vi.mocked(servicio.ingresarConGoogle).mockReset()
  })

  it('muestra el botón de Google y ninguna forma de registrarse ni de usar contraseña', async () => {
    vi.mocked(servicio.cargarConfiguracionIngreso).mockResolvedValue({
      googleClientId: 'abc.apps.googleusercontent.com',
      modoPrueba: false,
    })
    montar()

    expect(
      await screen.findByText('Botón de Google para abc.apps.googleusercontent.com'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/contraseña/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/regístrate|crear cuenta/i)).not.toBeInTheDocument()
    expect(screen.getByText(/aceptas los términos/i)).toBeInTheDocument()
  })

  it('avisa si el ingreso con Google todavía no está configurado', async () => {
    vi.mocked(servicio.cargarConfiguracionIngreso).mockResolvedValue({
      googleClientId: '',
      modoPrueba: false,
    })
    montar()

    expect(await screen.findByText(/todavía no está configurado/i)).toBeInTheDocument()
  })

  it('en desarrollo ofrece el ingreso de prueba y manda a completar el perfil si faltan datos', async () => {
    vi.mocked(servicio.cargarConfiguracionIngreso).mockResolvedValue({
      googleClientId: '',
      modoPrueba: true,
    })
    vi.mocked(servicio.ingresarConGoogle).mockImplementation(async () => {
      const u = usuarioDePrueba({ perfilCompleto: false })
      useSesion.getState().iniciar('t', u)
      return u
    })
    montar()

    await userEvent.type(await screen.findByLabelText('Correo'), 'nuevo@correo.com')
    await userEvent.type(screen.getByLabelText(/nombre y apellido/i), 'Nuevo Cliente')
    await userEvent.click(screen.getByRole('button', { name: /entrar \(desarrollo\)/i }))

    await waitFor(() =>
      expect(servicio.ingresarConGoogle).toHaveBeenCalledWith(
        'prueba|nuevo@correo.com|Nuevo|Cliente',
        true,
      ),
    )
    expect(await screen.findByText('Completa tu perfil')).toBeInTheDocument()
  })

  it('un cliente con el perfil completo vuelve al inicio y un administrador va al panel', async () => {
    vi.mocked(servicio.cargarConfiguracionIngreso).mockResolvedValue({
      googleClientId: '',
      modoPrueba: true,
    })
    vi.mocked(servicio.ingresarConGoogle).mockImplementation(async () => {
      const u = usuarioDePrueba()
      useSesion.getState().iniciar('t', u)
      return u
    })
    montar()
    await userEvent.type(await screen.findByLabelText('Correo'), 'ana@correo.com')
    await userEvent.click(screen.getByRole('button', { name: /entrar \(desarrollo\)/i }))
    expect(await screen.findByText('Pantalla de inicio')).toBeInTheDocument()
  })

  it('muestra el mensaje del servidor si el ingreso falla', async () => {
    vi.mocked(servicio.cargarConfiguracionIngreso).mockResolvedValue({
      googleClientId: '',
      modoPrueba: true,
    })
    vi.mocked(servicio.ingresarConGoogle).mockRejectedValue(new Error('x'))
    montar()
    await userEvent.type(await screen.findByLabelText('Correo'), 'ana@correo.com')
    await userEvent.click(screen.getByRole('button', { name: /entrar \(desarrollo\)/i }))
    expect(await screen.findByRole('alert')).toBeInTheDocument()
  })
})
