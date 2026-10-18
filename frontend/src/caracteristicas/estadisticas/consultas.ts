import { useQuery } from '@tanstack/react-query'

import { api } from '@/lib/api'

import type {
  Agrupacion,
  ClienteFrecuente,
  FilaCancha,
  FiltroEstadisticas,
  HorasPico,
  PuntoSerie,
  ResumenConComparacion,
} from './tipos'

const parametros = (f: FiltroEstadisticas) => ({
  desde: f.desde,
  hasta: f.hasta,
  sedeId: f.sedeId ?? undefined,
  canchaId: f.canchaId ?? undefined,
})

export const useResumen = (f: FiltroEstadisticas) =>
  useQuery({
    queryKey: ['estadisticas', 'resumen', f],
    queryFn: async () =>
      (
        await api.get<ResumenConComparacion>('/admin/estadisticas/resumen', {
          params: parametros(f),
        })
      ).data,
  })

export const useSerie = (f: FiltroEstadisticas, agrupacion: Agrupacion) =>
  useQuery({
    queryKey: ['estadisticas', 'serie', f, agrupacion],
    queryFn: async () =>
      (
        await api.get<{ puntos: PuntoSerie[] }>('/admin/estadisticas/serie', {
          params: { ...parametros(f), agrupacion },
        })
      ).data.puntos,
  })

export const usePorCancha = (f: FiltroEstadisticas) =>
  useQuery({
    queryKey: ['estadisticas', 'canchas', f],
    queryFn: async () =>
      (await api.get<FilaCancha[]>('/admin/estadisticas/canchas', { params: parametros(f) })).data,
  })

export const useHorasPico = (f: FiltroEstadisticas) =>
  useQuery({
    queryKey: ['estadisticas', 'horas-pico', f],
    queryFn: async () =>
      (await api.get<HorasPico>('/admin/estadisticas/horas-pico', { params: parametros(f) })).data,
  })

export const useClientesFrecuentes = (f: FiltroEstadisticas) =>
  useQuery({
    queryKey: ['estadisticas', 'clientes', f],
    queryFn: async () =>
      (
        await api.get<ClienteFrecuente[]>('/admin/estadisticas/clientes-frecuentes', {
          params: parametros(f),
        })
      ).data,
  })

/** Descarga el CSV (el token viaja en la cabecera, por eso no es un simple enlace). */
export async function descargarCsv(f: FiltroEstadisticas, agrupacion: Agrupacion): Promise<void> {
  const r = await api.get<Blob>('/admin/estadisticas/exportar', {
    params: { ...parametros(f), agrupacion },
    responseType: 'blob',
  })
  const url = URL.createObjectURL(r.data)
  const a = document.createElement('a')
  a.href = url
  a.download = `estadisticas-goltime-${f.desde}-a-${f.hasta}.csv`
  a.click()
  URL.revokeObjectURL(url)
}
