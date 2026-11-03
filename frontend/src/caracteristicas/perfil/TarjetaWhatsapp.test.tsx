import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { api } from '@/lib/api'
import { usuarioDePrueba } from '@/test/usuarios'

import { TarjetaWhatsapp } from './TarjetaWhatsapp'

function montar(vinculado: boolean) {
  const cliente = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={cliente}>
      <TarjetaWhatsapp usuario={usuarioDePrueba({ whatsappVinculado: vinculado })} />
    </QueryClientProvider>,
  )
}

describe('TarjetaWhatsapp', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('genera un código con su enlace a WhatsApp', async () => {
    vi.spyOn(api, 'post').mockResolvedValue({
      data: {
        codigo: 'X7K2PQ',
        expiraEn: new Date(Date.now() + 600_000).toISOString(),
        numeroBot: '+573001234567',
        enlace: 'https://wa.me/573001234567?text=hola',
      },
    })
    vi.spyOn(api, 'get').mockResolvedValue({ data: usuarioDePrueba() })
    montar(false)

    await userEvent.click(screen.getByRole('button', { name: /Vincular WhatsApp/ }))

    expect(await screen.findByText('X7K2PQ')).toBeInTheDocument()
    expect(screen.getByText('+57 300 123 4567')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Abrir WhatsApp/ })).toHaveAttribute(
      'href',
      'https://wa.me/573001234567?text=hola',
    )
    expect(api.post).toHaveBeenCalledWith('/me/whatsapp/codigo')
  })

  it('si ya está vinculado ofrece desvincular con confirmación', async () => {
    const borrar = vi.spyOn(api, 'delete').mockResolvedValue({ data: usuarioDePrueba() })
    montar(true)

    expect(screen.getByText('Vinculado')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Desvincular' }))
    await userEvent.click(screen.getByRole('button', { name: 'Sí, desvincular' }))
    expect(borrar).toHaveBeenCalledWith('/me/whatsapp')
  })
})
