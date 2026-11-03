import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { PrevisualizacionEstado, Reserva } from '../tipos'
import { efectosDelCambio } from '../utilidades'
import { ModalCambiarEstado } from './ModalCambiarEstado'

const previa = (parcial: Partial<PrevisualizacionEstado>): PrevisualizacionEstado => ({
  actual: 'CANCELADA',
  nuevo: 'CONFIRMADA',
  ocuparaHorario: false,
  horarioLibre: true,
  liberaraHorario: false,
  cuentaParaPremios: false,
  dejaDeContarParaPremios: false,
  quedaReembolsoPendiente: false,
  seCompletaraSola: false,
  expiraraSola: false,
  avisaAlCliente: true,
  ...parcial,
})

const estadoPrevia: { data?: PrevisualizacionEstado } = {}
const cambiar = vi.fn()

vi.mock('./consultas', () => ({
  usePrevisualizacionEstado: (_id: number, estado: string | null) =>
    estado === null
      ? { isPending: true, isSuccess: false, isError: false }
      : { isPending: false, isSuccess: true, isError: false, data: estadoPrevia.data },
  useCambiarEstadoReserva: () => ({ mutateAsync: cambiar, isPending: false }),
}))

const reserva = { id: 7, codigo: 'GT-2026-000007', estado: 'CANCELADA' } as Reserva

describe('efectosDelCambio', () => {
  it('explica en palabras lo que calculó el backend', () => {
    expect(efectosDelCambio(previa({ ocuparaHorario: true }))).toContain(
      'Volverá a ocupar la cancha en su horario.',
    )
    const liberar = efectosDelCambio(
      previa({
        liberaraHorario: true,
        quedaReembolsoPendiente: true,
        dejaDeContarParaPremios: true,
      }),
    )
    expect(liberar[0]).toMatch(/Liberará la cancha/)
    expect(liberar.join(' ')).toMatch(/reembolso pendiente/)
    expect(liberar.join(' ')).toMatch(/se le revoca/)
    expect(efectosDelCambio(previa({ avisaAlCliente: false })).at(-1)).toMatch(/No se le avisa/)
  })
})

describe('ModalCambiarEstado', () => {
  beforeEach(() => {
    cambiar.mockReset()
    estadoPrevia.data = undefined
  })

  it('ofrece todos los estados menos el actual y exige motivo antes de cambiar', async () => {
    estadoPrevia.data = previa({ ocuparaHorario: true })
    render(<ModalCambiarEstado reserva={reserva} alCerrar={() => undefined} />)
    const selector = screen.getByLabelText('Estado nuevo')
    expect(screen.queryByRole('option', { name: 'Cancelada' })).not.toBeInTheDocument()
    expect(screen.getAllByRole('option')).toHaveLength(7) // 6 estados + «Elige un estado…»

    await userEvent.selectOptions(selector, 'COMPLETADA')
    expect(screen.getByText('Volverá a ocupar la cancha en su horario.')).toBeInTheDocument()
    const boton = screen.getByRole('button', { name: 'Cambiar a «Completada»' })
    expect(boton).toBeDisabled()

    await userEvent.type(screen.getByLabelText('Motivo (obligatorio)'), 'Pagó por adelantado')
    expect(boton).toBeEnabled()
    await userEvent.click(boton)
    expect(cambiar).toHaveBeenCalledWith({
      id: 7,
      estado: 'COMPLETADA',
      motivo: 'Pagó por adelantado',
    })
  })

  it('no deja confirmar si el horario ya lo tomó otra reserva', async () => {
    estadoPrevia.data = previa({ ocuparaHorario: true, horarioLibre: false })
    render(<ModalCambiarEstado reserva={reserva} alCerrar={() => undefined} />)
    await userEvent.selectOptions(screen.getByLabelText('Estado nuevo'), 'CONFIRMADA')
    await userEvent.type(screen.getByLabelText('Motivo (obligatorio)'), 'Vuelve a jugar')
    expect(screen.getByText(/ya lo tomó otra reserva/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cambiar a «Confirmada»' })).toBeDisabled()
  })
})
