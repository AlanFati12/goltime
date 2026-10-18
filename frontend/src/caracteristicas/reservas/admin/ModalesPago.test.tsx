import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import type { Reserva } from '../tipos'
import { ModalPago, ModalReembolso } from './ModalesPago'

const reserva = (parcial: Partial<Reserva>): Reserva =>
  ({
    id: 1,
    codigo: 'GT-2026-000001',
    precioFinal: 100000,
    montoPagado: 40000,
    estadoPago: 'ABONADO',
    ...parcial,
  }) as Reserva

function montar(ui: React.ReactNode) {
  render(<QueryClientProvider client={new QueryClient()}>{ui}</QueryClientProvider>)
}

describe('ModalPago', () => {
  it('sugiere el saldo pendiente y no deja pasarse de él', async () => {
    montar(<ModalPago reserva={reserva({})} alCerrar={() => undefined} />)
    const monto = screen.getByLabelText('Monto (COP)')
    expect(monto).toHaveValue(60000)
    const boton = screen.getByRole('button', { name: 'Registrar' })
    expect(boton).toBeEnabled()

    await userEvent.clear(monto)
    await userEvent.type(monto, '60001')
    expect(boton).toBeDisabled()
    expect(screen.getByText('Debe ser un valor entero entre 1 y 60000')).toBeInTheDocument()

    await userEvent.clear(monto)
    await userEvent.type(monto, '25000')
    expect(boton).toBeEnabled()
  })

  it('rechaza montos vacíos o cero', async () => {
    montar(<ModalPago reserva={reserva({})} alCerrar={() => undefined} />)
    const monto = screen.getByLabelText('Monto (COP)')
    await userEvent.clear(monto)
    expect(screen.getByRole('button', { name: 'Registrar' })).toBeDisabled()
    await userEvent.type(monto, '0')
    expect(screen.getByRole('button', { name: 'Registrar' })).toBeDisabled()
  })
})

describe('ModalReembolso', () => {
  it('ofrece retener una parte solo cuando se devuelve menos de lo pagado', async () => {
    montar(
      <ModalReembolso
        reserva={reserva({ montoPagado: 80000, estadoPago: 'REEMBOLSO_PENDIENTE' })}
        alCerrar={() => undefined}
      />,
    )
    expect(screen.queryByText(/retener/)).not.toBeInTheDocument()

    const monto = screen.getByLabelText('Monto a devolver (COP)')
    await userEvent.clear(monto)
    await userEvent.type(monto, '60000')
    expect(screen.getByText(/retener \$ 20\.000/)).toBeInTheDocument()
    expect(screen.getByText(/Quedarán \$ 20\.000 pendientes de devolver/)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('checkbox'))
    expect(screen.queryByText(/pendientes de devolver/)).not.toBeInTheDocument()
  })

  it('no permite devolver más de lo pagado', async () => {
    montar(<ModalReembolso reserva={reserva({ montoPagado: 80000 })} alCerrar={() => undefined} />)
    const monto = screen.getByLabelText('Monto a devolver (COP)')
    await userEvent.clear(monto)
    await userEvent.type(monto, '80001')
    expect(screen.getByRole('button', { name: 'Registrar reembolso' })).toBeDisabled()
  })
})
