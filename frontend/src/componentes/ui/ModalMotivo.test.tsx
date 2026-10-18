import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { ModalMotivo } from './ModalMotivo'

function montar(motivoObligatorio: boolean) {
  const alConfirmar = vi.fn().mockResolvedValue(undefined)
  render(
    <ModalMotivo
      titulo="Rechazar"
      descripcion="Se liberará el horario."
      textoConfirmar="Rechazar"
      motivoObligatorio={motivoObligatorio}
      alConfirmar={alConfirmar}
      alCerrar={() => undefined}
    />,
  )
  return alConfirmar
}

describe('ModalMotivo', () => {
  it('con motivo obligatorio no deja confirmar hasta escribir uno suficiente', async () => {
    const alConfirmar = montar(true)
    const boton = screen.getByRole('button', { name: 'Rechazar' })
    expect(boton).toBeDisabled()
    await userEvent.type(screen.getByLabelText('Motivo (obligatorio)'), 'ok')
    expect(boton).toBeDisabled()
    await userEvent.type(screen.getByLabelText('Motivo (obligatorio)'), ' es largo')
    expect(boton).toBeEnabled()
    await userEvent.click(boton)
    await waitFor(() => expect(alConfirmar).toHaveBeenCalledWith('ok es largo'))
  })

  it('con motivo opcional permite confirmar vacío', async () => {
    const alConfirmar = montar(false)
    await userEvent.click(screen.getByRole('button', { name: 'Rechazar' }))
    await waitFor(() => expect(alConfirmar).toHaveBeenCalledWith(''))
  })
})
