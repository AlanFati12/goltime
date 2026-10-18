import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import { Llaves } from './Llaves'
import { TablaPosiciones } from './TablaPosiciones'
import { TarjetaPartido } from './TarjetaPartido'
import type { Lado, Partido, Tabla } from './tipos'
import { marcador, nombreLado, textoPenales, textoRonda } from './utilidades'

const lado = (id: number, nombre: string): Lado => ({
  inscripcionId: id,
  equipoId: id,
  nombre,
  color: null,
  etiqueta: null,
})

function partido(sobre: Partial<Partido> = {}): Partido {
  return {
    id: 1,
    torneoId: 1,
    torneo: 'Copa Goltime',
    faseId: 1,
    tipoFase: 'ELIMINATORIA',
    grupo: null,
    ronda: 1,
    numeroLlave: 1,
    nombreRonda: 'Semifinal',
    local: lado(1, 'Los Cracks'),
    visitante: lado(2, 'Rayo FC'),
    canchaId: null,
    cancha: null,
    inicio: null,
    duracionMin: null,
    arbitro: null,
    estado: 'PENDIENTE',
    golesLocal: null,
    golesVisitante: null,
    penalesLocal: null,
    penalesVisitante: null,
    ganadorInscripcionId: null,
    descanso: false,
    porWO: false,
    figuraJugadorId: null,
    figura: null,
    siguientePartidoId: null,
    ...sobre,
  }
}

describe('utilidades de torneos', () => {
  it('muestra el nombre, la etiqueta o "Por definir"', () => {
    expect(nombreLado(lado(1, 'Los Cracks'))).toBe('Los Cracks')
    expect(
      nombreLado({
        inscripcionId: null,
        equipoId: null,
        nombre: null,
        color: null,
        etiqueta: '1° Grupo A',
      }),
    ).toBe('1° Grupo A')
    expect(nombreLado(null)).toBe('Por definir')
  })

  it('formatea marcador, penales y ronda', () => {
    expect(marcador(partido())).toBe('—')
    expect(marcador(partido({ golesLocal: 2, golesVisitante: 1 }))).toBe('2 - 1')
    expect(textoPenales(partido({ penalesLocal: 4, penalesVisitante: 3 }))).toBe('(4-3 pen.)')
    expect(textoPenales(partido())).toBeNull()
    expect(textoRonda(partido({ grupo: 'A', nombreRonda: 'Fecha 2' }))).toBe('Grupo A · Fecha 2')
  })
})

describe('TablaPosiciones', () => {
  const tabla: Tabla = {
    faseId: 1,
    fase: 'Liga',
    grupoId: null,
    grupo: null,
    filas: [
      {
        inscripcionId: 1,
        equipoId: 1,
        equipo: 'Los Cracks',
        color: null,
        posicion: 1,
        jugados: 3,
        ganados: 3,
        empatados: 0,
        perdidos: 0,
        golesFavor: 7,
        golesContra: 1,
        diferencia: 6,
        puntos: 9,
        puntosDisciplina: 0,
        forma: ['G', 'G', 'G'],
      },
      {
        inscripcionId: 2,
        equipoId: 2,
        equipo: 'Rayo FC',
        color: null,
        posicion: 2,
        jugados: 3,
        ganados: 0,
        empatados: 1,
        perdidos: 2,
        golesFavor: 1,
        golesContra: 5,
        diferencia: -4,
        puntos: 1,
        puntosDisciplina: 0,
        forma: ['E', 'P', 'P'],
      },
    ],
  }

  it('lista los equipos en orden con sus puntos y diferencia', () => {
    render(
      <MemoryRouter>
        <TablaPosiciones tabla={tabla} />
      </MemoryRouter>,
    )
    const filas = screen.getAllByRole('row')
    expect(filas).toHaveLength(3) // encabezado + 2 equipos
    expect(filas[1]).toHaveTextContent('Los Cracks')
    expect(filas[1]).toHaveTextContent('+6')
    expect(filas[2]).toHaveTextContent('-4')
    expect(screen.getAllByLabelText('Ganó')).toHaveLength(3)
  })
})

describe('Llaves y tarjeta de partido', () => {
  it('agrupa por ronda y separa el tercer puesto', () => {
    const partidos = [
      partido({ id: 1, ronda: 1, nombreRonda: 'Semifinal' }),
      partido({ id: 2, ronda: 2, nombreRonda: 'Final', local: null, visitante: null }),
      partido({ id: 3, ronda: 2, numeroLlave: 2, nombreRonda: 'Tercer puesto' }),
    ]
    render(
      <MemoryRouter>
        <Llaves partidos={partidos} ruta={(p) => `/torneos/partidos/${p.id}`} />
      </MemoryRouter>,
    )
    expect(screen.getByRole('region', { name: 'Semifinal' })).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Final' })).toBeInTheDocument()
    expect(screen.getByText('Tercer puesto')).toBeInTheDocument()
    expect(screen.getAllByText('Por definir')).toHaveLength(2)
  })

  it('resalta el marcador y los penales en la tarjeta', () => {
    render(
      <MemoryRouter>
        <TarjetaPartido
          partido={partido({
            estado: 'FINALIZADO',
            golesLocal: 1,
            golesVisitante: 1,
            penalesLocal: 4,
            penalesVisitante: 3,
            ganadorInscripcionId: 1,
          })}
          ruta="/torneos/partidos/1"
        />
      </MemoryRouter>,
    )
    expect(screen.getByText('1 - 1')).toBeInTheDocument()
    expect(screen.getByText(/4-3 pen\./)).toBeInTheDocument()
    expect(screen.getByRole('link')).toHaveAttribute('href', '/torneos/partidos/1')
  })
})
