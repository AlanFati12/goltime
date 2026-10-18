import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { Boton } from './Boton'

describe('Boton', () => {
  it('ejecuta onClick', async () => {
    const alClic = vi.fn()
    render(<Boton onClick={alClic}>Reservar</Boton>)
    await userEvent.click(screen.getByRole('button', { name: 'Reservar' }))
    expect(alClic).toHaveBeenCalledOnce()
  })

  it('no ejecuta onClick cuando está deshabilitado', async () => {
    const alClic = vi.fn()
    render(
      <Boton disabled onClick={alClic}>
        Reservar
      </Boton>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Reservar' }))
    expect(alClic).not.toHaveBeenCalled()
  })
})
