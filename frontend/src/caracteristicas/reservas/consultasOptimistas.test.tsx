import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useAvisos } from '@/componentes/ui/almacenAvisos'
import { api } from '@/lib/api'

import { useAccionesNotificaciones } from './consultas'

vi.mock('@/lib/api', async (original) => {
  const real = await original<typeof import('@/lib/api')>()
  return { ...real, api: { post: vi.fn() } }
})

const notificacion = (id: number, leida: boolean) =>
  ({ id, leida, titulo: `n${id}`, mensaje: '', creadoEn: '2026-11-01T00:00:00Z' }) as never

function preparar() {
  const cliente = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  cliente.setQueryData(['notificaciones', 'contador'], 2)
  cliente.setQueryData(['notificaciones', 'lista', 0, 20], {
    contenido: [notificacion(1, false), notificacion(2, false)],
    pagina: 0,
    tamano: 20,
    totalElementos: 2,
    totalPaginas: 1,
  })
  const envoltura = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={cliente}>{children}</QueryClientProvider>
  )
  return { cliente, envoltura }
}

const leidas = (c: QueryClient) =>
  (
    c.getQueryData(['notificaciones', 'lista', 0, 20]) as { contenido: { leida: boolean }[] }
  ).contenido.map((n) => n.leida)

describe('acciones optimistas de notificaciones', () => {
  beforeEach(() => {
    vi.mocked(api.post).mockReset()
    useAvisos.setState({ avisos: [] })
  })

  it('refleja el cambio al instante, antes de que responda el servidor', async () => {
    let responder: (v: unknown) => void = () => {}
    vi.mocked(api.post).mockReturnValue(new Promise((r) => (responder = r)) as never)
    const { cliente, envoltura } = preparar()
    const { result } = renderHook(() => useAccionesNotificaciones(), { wrapper: envoltura })

    act(() => result.current.leer.mutate(1))

    await waitFor(() => expect(leidas(cliente)).toEqual([true, false]))
    expect(cliente.getQueryData(['notificaciones', 'contador'])).toBe(1)
    act(() => responder({}))
  })

  it('si el servidor falla, deshace el cambio y avisa', async () => {
    vi.mocked(api.post).mockRejectedValue(new Error('caído'))
    const { cliente, envoltura } = preparar()
    const { result } = renderHook(() => useAccionesNotificaciones(), { wrapper: envoltura })

    act(() => result.current.leerTodas.mutate())

    await waitFor(() => expect(useAvisos.getState().avisos).toHaveLength(1))
    expect(useAvisos.getState().avisos[0].tipo).toBe('error')
    expect(leidas(cliente)).toEqual([false, false])
    expect(cliente.getQueryData(['notificaciones', 'contador'])).toBe(2)
  })
})
