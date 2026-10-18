import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'

import { Estrellas } from './Estrellas'
import { PanelCalificacion } from './PanelCalificacion'
import { PaginaListaEspera } from './PaginasCliente'
import { ResumenCalificaciones } from './ResumenCalificaciones'

const calificar = vi.fn()

vi.mock('./consultas', () => ({
  useCalificacionesCancha: () => ({
    data: {
      promedio: 3.5,
      total: 2,
      distribucion: [0, 1, 0, 0, 1],
      ultimas: [
        {
          estrellas: 2,
          comentario: 'Faltaba iluminación',
          autor: 'Ana P.',
          fecha: '2026-11-03T15:00:00Z',
        },
      ],
    },
  }),
  useEstadoCalificacion: () => ({ data: { puedeCalificar: true } }),
  useCalificar: () => ({ mutate: calificar, isPending: false }),
  useMisEsperas: () => ({
    isPending: false,
    isError: false,
    data: [
      {
        id: 1,
        canchaId: 7,
        cancha: 'Cancha 1',
        sede: 'Goltime Centro',
        inicio: '2026-11-10T00:00:00Z',
        fin: '2026-11-10T01:00:00Z',
        estado: 'NOTIFICADA',
        creadoEn: '2026-11-01T12:00:00Z',
      },
      {
        id: 2,
        canchaId: 7,
        cancha: 'Cancha 1',
        sede: 'Goltime Centro',
        inicio: '2026-11-11T00:00:00Z',
        fin: '2026-11-11T01:00:00Z',
        estado: 'ESPERANDO',
        creadoEn: '2026-11-01T12:00:00Z',
      },
    ],
  }),
  useAccionesEspera: () => ({ salir: { mutate: vi.fn() }, unirse: { mutate: vi.fn() } }),
}))

describe('Estrellas', () => {
  it('describe el valor para lectores de pantalla', () => {
    render(<Estrellas valor={4} />)
    expect(screen.getByRole('img', { name: '4 de 5 estrellas' })).toBeInTheDocument()
  })
})

describe('ResumenCalificaciones', () => {
  it('muestra el promedio, la distribución y las opiniones sin datos personales completos', () => {
    render(<ResumenCalificaciones canchaId={7} />)
    expect(screen.getByText('3.5')).toBeInTheDocument()
    expect(screen.getByText('2 calificaciones')).toBeInTheDocument()
    expect(
      screen.getByRole('list', { name: 'Distribución de calificaciones' }).querySelectorAll('li'),
    ).toHaveLength(5)
    expect(screen.getByText('Ana P.')).toBeInTheDocument()
    expect(screen.getByText('Faltaba iluminación')).toBeInTheDocument()
  })
})

describe('PanelCalificacion', () => {
  it('exige elegir estrellas antes de enviar', async () => {
    render(<PanelCalificacion reservaId={5} />)
    const enviar = screen.getByRole('button', { name: 'Enviar calificación' })
    expect(enviar).toBeDisabled()
    await userEvent.click(screen.getByRole('radio', { name: '4 estrellas' }))
    expect(enviar).toBeEnabled()
    await userEvent.click(enviar)
    expect(calificar).toHaveBeenCalledWith(
      { estrellas: 4, comentario: undefined },
      expect.anything(),
    )
  })
})

describe('PaginaListaEspera', () => {
  it('destaca los horarios que ya se liberaron', () => {
    render(
      <MemoryRouter>
        <PaginaListaEspera />
      </MemoryRouter>,
    )
    expect(screen.getByText('¡Se liberó! Reserva ya')).toBeInTheDocument()
    expect(screen.getByText('Esperando')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Quitar' })).toHaveLength(2)
    expect(screen.getAllByRole('link', { name: 'Reservar' })).toHaveLength(1)
  })
})
