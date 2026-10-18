import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { GraficoIngresos, MapaHorasPico } from './Graficos'

describe('GraficoIngresos', () => {
  const puntos = [
    { fecha: '2026-11-02', ingresos: 0, completadas: 0, canceladas: 0 },
    { fecha: '2026-11-03', ingresos: 240000, completadas: 2, canceladas: 1 },
  ]

  it('describe el gráfico y ofrece la misma información como tabla', () => {
    render(<GraficoIngresos puntos={puntos} titulo="Ingresos reales" />)
    expect(screen.getByRole('img', { name: /Ingresos reales: 2 periodos/ })).toBeInTheDocument()
    const filas = screen.getAllByRole('row')
    expect(filas).toHaveLength(3) // encabezado + 2 periodos
    expect(filas[2]).toHaveTextContent('2026-11-03')
    expect(filas[2]).toHaveTextContent('240.000')
  })
})

describe('MapaHorasPico', () => {
  it('muestra un mensaje sin datos', () => {
    render(<MapaHorasPico datos={{ celdas: [], maximo: 0 }} />)
    expect(screen.getByText('No hay reservas en el periodo.')).toBeInTheDocument()
  })

  it('pinta una fila por día y una columna por hora con reservas', () => {
    render(
      <MapaHorasPico
        datos={{
          celdas: [
            { diaSemana: 1, hora: 18, reservas: 3 },
            { diaSemana: 5, hora: 20, reservas: 1 },
          ],
          maximo: 3,
        }}
      />,
    )
    const tabla = screen.getByRole('table', { name: /Reservas por día/ })
    expect(tabla.querySelectorAll('tbody tr')).toHaveLength(7)
    expect(tabla.querySelectorAll('thead th')).toHaveLength(4) // vacío + 18, 19, 20
    expect(screen.getByTitle('Lun 18:00 · 3 reservas')).toBeInTheDocument()
  })
})
