import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { Pestanas } from './Pestanas'

const PESTANAS = [
  { id: 'a', texto: 'Equipos', conteo: 3 },
  { id: 'b', texto: 'Fixture' },
]

describe('Pestanas', () => {
  it('marca la pestaña activa y muestra el conteo', () => {
    render(<Pestanas etiqueta="Secciones" pestanas={PESTANAS} actual="a" alCambiar={() => {}} />)
    expect(screen.getByRole('tab', { name: /Equipos/ })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Fixture' })).toHaveAttribute('aria-selected', 'false')
    expect(screen.getByText('3')).toBeInTheDocument()
  })

  it('avisa qué pestaña se eligió', async () => {
    const alCambiar = vi.fn()
    render(<Pestanas etiqueta="Secciones" pestanas={PESTANAS} actual="a" alCambiar={alCambiar} />)
    await userEvent.click(screen.getByRole('tab', { name: 'Fixture' }))
    expect(alCambiar).toHaveBeenCalledWith('b')
  })
})
