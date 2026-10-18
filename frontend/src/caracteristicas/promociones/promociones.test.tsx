import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'

import { PaginaMisPremios } from './PaginaMisPremios'
import { PaginaPromociones } from './PaginaPromociones'

vi.mock('./consultas', () => ({
  useMisPremios: () => ({
    isPending: false,
    isError: false,
    data: {
      progreso: [
        {
          regla: {
            id: 1,
            nombre: 'Hora con descuento',
            descripcion: null,
            reservasRequeridas: 5,
            tipo: 'PORCENTAJE',
            valor: 20,
            beneficio: '20%',
            vigenciaDias: 60,
            activa: true,
          },
          completadas: 7,
          enCiclo: 2,
          faltan: 3,
        },
      ],
      premios: [
        {
          id: 9,
          clienteId: 1,
          nombre: 'Hora con descuento',
          tipo: 'PORCENTAJE',
          valor: 20,
          beneficio: '20%',
          estado: 'DISPONIBLE',
          otorgadoEn: '2026-10-01T15:00:00Z',
          venceEn: '2026-12-01T15:00:00Z',
          reservaUsoId: null,
          motivoRevocacion: null,
        },
      ],
    },
  }),
  usePromocionesPublicas: () => ({
    isPending: false,
    isError: false,
    data: [
      {
        id: 1,
        nombre: 'Verano',
        descripcion: 'Descuento de temporada',
        codigo: 'VERANO20',
        tipo: 'PORCENTAJE',
        valor: 20,
        descuentoTexto: '20%',
        fechaDesde: '2026-11-01',
        fechaHasta: '2026-11-30',
        diasSemana: [2, 3],
        minutoInicio: 840,
        minutoFin: 1080,
        sedeId: null,
        canchaId: null,
        usosMaximos: null,
        usosPorCliente: null,
        soloPrimeraReserva: false,
        publica: true,
        activa: true,
        automatica: false,
      },
      {
        id: 2,
        nombre: 'Martes de amigos',
        descripcion: null,
        codigo: null,
        tipo: 'MONTO_FIJO',
        valor: 5000,
        descuentoTexto: '$ 5.000',
        fechaDesde: '2026-11-01',
        fechaHasta: '2026-11-30',
        diasSemana: [],
        minutoInicio: null,
        minutoFin: null,
        sedeId: null,
        canchaId: null,
        usosMaximos: null,
        usosPorCliente: null,
        soloPrimeraReserva: true,
        publica: true,
        activa: true,
        automatica: true,
      },
    ],
  }),
}))

describe('PaginaMisPremios', () => {
  it('muestra el progreso y cuántas reservas faltan', () => {
    render(
      <MemoryRouter>
        <PaginaMisPremios />
      </MemoryRouter>,
    )
    const barra = screen.getByRole('progressbar', { name: /Hora con descuento/ })
    expect(barra).toHaveAttribute('aria-valuenow', '2')
    expect(barra).toHaveAttribute('aria-valuemax', '5')
    expect(screen.getByText(/Te faltan 3 reservas/)).toBeInTheDocument()
    expect(screen.getByText(/Tienes 1 premio disponible/)).toBeInTheDocument()
    expect(screen.getByText('Disponible')).toBeInTheDocument()
  })
})

describe('PaginaPromociones', () => {
  it('distingue las promociones con código de las automáticas', () => {
    render(
      <MemoryRouter>
        <PaginaPromociones />
      </MemoryRouter>,
    )
    expect(screen.getByText('VERANO20')).toBeInTheDocument()
    expect(screen.getByText('Se aplica sola al reservar.')).toBeInTheDocument()
    expect(screen.getByText('Primera reserva')).toBeInTheDocument()
    expect(screen.getByText(/14:00–18:00/)).toBeInTheDocument()
  })
})
