import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { ModalCambioEstado } from './ModalCambioEstado'

function montar(reservas: number) {
  const confirmar = vi.fn().mockResolvedValue(undefined)
  const alCerrar = vi.fn()
  render(
    <ModalCambioEstado
      abierto
      titulo="Pasar a mantenimiento"
      descripcion="La cancha dejará de reservarse."
      requiereMotivo
      textoConfirmar="Confirmar"
      previsualizar={() =>
        Promise.resolve({ reservasFuturasActivas: reservas, requiereConfirmacion: reservas > 0 })
      }
      confirmar={confirmar}
      alCerrar={alCerrar}
    />,
  )
  return { confirmar, alCerrar }
}

describe('ModalCambioEstado', () => {
  it('sin reservas futuras solo exige el motivo', async () => {
    const { confirmar, alCerrar } = montar(0)
    const boton = await screen.findByRole('button', { name: 'Confirmar' })
    await waitFor(() => expect(boton).toBeDisabled())
    await userEvent.type(screen.getByLabelText('Motivo (obligatorio)'), 'Cambio de grama')
    expect(boton).toBeEnabled()
    await userEvent.click(boton)
    await waitFor(() => expect(confirmar).toHaveBeenCalledWith('Cambio de grama', false))
    expect(alCerrar).toHaveBeenCalled()
  })

  it('con reservas futuras advierte y exige confirmación explícita', async () => {
    const { confirmar } = montar(3)
    expect(await screen.findByText(/3 reservas futuras/)).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Motivo (obligatorio)'), 'Mantenimiento general')
    const boton = screen.getByRole('button', { name: 'Confirmar' })
    expect(boton).toBeDisabled()
    await userEvent.click(screen.getByLabelText('Entiendo y confirmo que se cancelarán.'))
    expect(boton).toBeEnabled()
    await userEvent.click(boton)
    await waitFor(() => expect(confirmar).toHaveBeenCalledWith('Mantenimiento general', true))
  })

  it('rechaza un motivo demasiado corto', async () => {
    montar(0)
    await userEvent.type(await screen.findByLabelText('Motivo (obligatorio)'), 'ok')
    expect(screen.getByText('Describe el motivo (mínimo 5 caracteres)')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar' })).toBeDisabled()
  })
})
