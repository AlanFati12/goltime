import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { usuarioDePrueba } from '@/test/usuarios'

import { useSesion } from './almacenSesion'
import { PaginaCompletarPerfil } from './PaginaCompletarPerfil'

vi.mock('./servicioAutenticacion', async (original) => ({
  ...(await original<typeof import('./servicioAutenticacion')>()),
  completarPerfil: vi.fn(),
}))
import * as servicio from './servicioAutenticacion'

function montar(desde = '/reservar') {
  return render(
    <MemoryRouter initialEntries={[{ pathname: '/completar-perfil', state: { desde } }]}>
      <Routes>
        <Route path="/completar-perfil" element={<PaginaCompletarPerfil />} />
        <Route path="/reservar" element={<p>Pantalla de reservar</p>} />
        <Route path="/" element={<p>Inicio</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('PaginaCompletarPerfil', () => {
  beforeEach(() => {
    vi.mocked(servicio.completarPerfil).mockReset()
    useSesion.getState().iniciar('t', usuarioDePrueba({ perfilCompleto: false, apellidos: '' }))
  })

  it('da la bienvenida con lo que Google ya dio y pide apellidos y WhatsApp', async () => {
    montar()

    expect(screen.getByText(/bienvenido, ana/i)).toBeInTheDocument()
    expect(screen.getByText('ana@correo.com')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /guardar y continuar/i }))
    expect(await screen.findAllByText(/obligatorio|escribe tu número/i)).not.toHaveLength(0)
    expect(servicio.completarPerfil).not.toHaveBeenCalled()
  })

  it('guarda los datos y devuelve a donde la persona iba', async () => {
    vi.mocked(servicio.completarPerfil).mockResolvedValue(usuarioDePrueba())
    montar('/reservar')

    await userEvent.type(screen.getByLabelText('Apellidos'), 'Pérez Gómez')
    await userEvent.type(screen.getByLabelText(/número de whatsapp|whatsapp/i), '3001234567')
    await userEvent.click(screen.getByRole('button', { name: /guardar y continuar/i }))

    await waitFor(() => expect(servicio.completarPerfil).toHaveBeenCalledOnce())
    expect(vi.mocked(servicio.completarPerfil).mock.calls[0][0]).toMatchObject({
      apellidos: 'Pérez Gómez',
      telefono: '+573001234567',
    })
    expect(await screen.findByText('Pantalla de reservar')).toBeInTheDocument()
  })

  it('quien ya tiene el perfil completo no se queda aquí', () => {
    useSesion.getState().iniciar('t', usuarioDePrueba({ perfilCompleto: true }))
    montar()
    expect(screen.getByText('Inicio')).toBeInTheDocument()
  })
})
