import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import { PaginaPrivacidad, PaginaTerminos } from './PaginasLegales'

describe('Páginas legales', () => {
  it('la política de privacidad nombra la ley y los derechos del titular', () => {
    render(<PaginaPrivacidad />, { wrapper: MemoryRouter })
    expect(
      screen.getByRole('heading', { level: 1, name: /política de privacidad/i }),
    ).toBeInTheDocument()
    expect(screen.getByText(/Ley 1581 de 2012/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /tus derechos/i })).toBeInTheDocument()
  })

  it('los términos enlazan a la política de privacidad', () => {
    render(<PaginaTerminos />, { wrapper: MemoryRouter })
    expect(screen.getByRole('heading', { level: 1, name: /términos de uso/i })).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: /política de privacidad/i })[0]).toHaveAttribute(
      'href',
      '/privacidad',
    )
  })
})
