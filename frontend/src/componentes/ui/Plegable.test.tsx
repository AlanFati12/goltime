import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { Plegable } from './Plegable'

describe('Plegable', () => {
  it('oculta el contenido hasta que se abre', async () => {
    render(
      <Plegable titulo="Más filtros">
        <p>Filtro avanzado</p>
      </Plegable>,
    )
    expect(screen.queryByText('Filtro avanzado')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /Más filtros/ }))
    expect(screen.getByText('Filtro avanzado')).toBeInTheDocument()
  })

  it('puede empezar abierto, por ejemplo cuando hay filtros activos', () => {
    render(
      <Plegable titulo="Más filtros" abiertoInicial>
        <p>Filtro avanzado</p>
      </Plegable>,
    )
    expect(screen.getByText('Filtro avanzado')).toBeInTheDocument()
  })
})
