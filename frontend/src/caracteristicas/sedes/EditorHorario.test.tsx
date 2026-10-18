import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { EditorHorario } from './EditorHorario'
import type { HorarioDia } from './tipos'

const dias: HorarioDia[] = [
  { diaSemana: 1, cerrado: false, apertura: '08:00', cierre: '23:00' },
  { diaSemana: 7, cerrado: false, apertura: '08:00', cierre: '20:00' },
]

describe('EditorHorario', () => {
  it('muestra los 7 días y completa los que faltan', () => {
    render(<EditorHorario dias={dias} alGuardar={() => undefined} />)
    expect(screen.getAllByRole('listitem')).toHaveLength(7)
    expect(screen.getByLabelText('Cierre Domingo')).toHaveValue('20:00')
    expect(screen.getByLabelText('Cierre Miércoles')).toHaveValue('23:00')
  })

  it('guarda un día cerrado sin horas', async () => {
    const alGuardar = vi.fn()
    render(<EditorHorario dias={dias} alGuardar={alGuardar} />)
    const casillas = screen.getAllByRole('checkbox')
    await userEvent.click(casillas[1]) // martes cerrado
    await userEvent.click(screen.getByRole('button', { name: 'Guardar horario' }))
    const enviado = alGuardar.mock.calls[0][0] as HorarioDia[]
    expect(enviado).toHaveLength(7)
    expect(enviado[1]).toEqual({ diaSemana: 2, cerrado: true, apertura: null, cierre: null })
    expect(enviado[0]).toMatchObject({ cerrado: false, apertura: '08:00', cierre: '23:00' })
  })

  it('permite cerrar a las 24:00', async () => {
    const alGuardar = vi.fn()
    render(<EditorHorario dias={dias} alGuardar={alGuardar} />)
    await userEvent.selectOptions(screen.getByLabelText('Cierre Lunes'), '24:00')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar horario' }))
    expect((alGuardar.mock.calls[0][0] as HorarioDia[])[0].cierre).toBe('24:00')
  })

  it('bloquea el guardado si el cierre no es posterior a la apertura', async () => {
    render(<EditorHorario dias={dias} alGuardar={() => undefined} />)
    await userEvent.selectOptions(screen.getByLabelText('Cierre Lunes'), '07:00')
    expect(screen.getByText('El cierre debe ser posterior a la apertura')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Guardar horario' })).toBeDisabled()
  })
})
