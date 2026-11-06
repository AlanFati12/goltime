import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { api } from '@/lib/api'

import { BotonWhatsapp } from './BotonWhatsapp'

function pintar() {
  const cliente = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={cliente}>
      <BotonWhatsapp />
    </QueryClientProvider>,
  )
}

describe('BotonWhatsapp', () => {
  afterEach(() => vi.restoreAllMocks())

  it('enlaza al chat del bot cuando está disponible', async () => {
    vi.spyOn(api, 'get').mockResolvedValue({
      data: { disponible: true, enlace: 'https://wa.me/573239085762?text=Hola' },
    })
    pintar()
    const enlace = await screen.findByRole('link', { name: /whatsapp/i })
    expect(enlace).toHaveAttribute('href', 'https://wa.me/573239085762?text=Hola')
  })

  it('no muestra nada si el bot no está disponible', async () => {
    vi.spyOn(api, 'get').mockResolvedValue({ data: { disponible: false } })
    const { container } = pintar()
    await vi.waitFor(() => expect(api.get).toHaveBeenCalled())
    expect(container).toBeEmptyDOMElement()
  })

  it('no muestra nada si la consulta falla', async () => {
    vi.spyOn(api, 'get').mockRejectedValue(new Error('caído'))
    const { container } = pintar()
    await vi.waitFor(() => expect(api.get).toHaveBeenCalled())
    expect(container).toBeEmptyDOMElement()
  })
})
