import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { api } from '@/lib/api'

import type {
  DetallePromocion,
  EstadoPremio,
  MisPremios,
  Premio,
  Promocion,
  PrevisualizacionDescuento,
  ReglaPremio,
} from './tipos'

const useRefresco = () => {
  const cliente = useQueryClient()
  return () => {
    void cliente.invalidateQueries({ queryKey: ['promociones'] })
    void cliente.invalidateQueries({ queryKey: ['premios'] })
  }
}

// ---------- Público y cliente ----------

export const usePromocionesPublicas = () =>
  useQuery({
    queryKey: ['promociones', 'publicas'],
    queryFn: async () => (await api.get<Promocion[]>('/publico/promociones')).data,
  })

export const useMisPremios = (habilitado = true) =>
  useQuery({
    queryKey: ['premios', 'mios'],
    enabled: habilitado,
    queryFn: async () => (await api.get<MisPremios>('/me/premios')).data,
  })

export function usePrevisualizarDescuento(p: {
  canchaId: number
  inicio: string
  fin: string
  codigo?: string
  premioId?: number
}) {
  return useQuery({
    queryKey: ['promociones', 'previsualizar', p],
    retry: false,
    queryFn: async () =>
      (
        await api.get<PrevisualizacionDescuento>('/me/descuentos/previsualizar', {
          params: { ...p, codigo: p.codigo || undefined, premioId: p.premioId || undefined },
        })
      ).data,
  })
}

// ---------- Administración ----------

export interface DatosPromocion {
  nombre: string
  descripcion?: string
  codigo?: string
  tipo: string
  valor: number
  fechaDesde: string
  fechaHasta: string
  diasSemana?: number[]
  minutoInicio?: number | null
  minutoFin?: number | null
  sedeId?: number | null
  canchaId?: number | null
  usosMaximos?: number | null
  usosPorCliente?: number | null
  soloPrimeraReserva?: boolean
  publica?: boolean
}

export const usePromocionesAdmin = () =>
  useQuery({
    queryKey: ['promociones', 'admin'],
    queryFn: async () => (await api.get<Promocion[]>('/admin/promociones')).data,
  })

export const useDetallePromocion = (id: number | null) =>
  useQuery({
    queryKey: ['promociones', 'detalle', id],
    enabled: id !== null,
    queryFn: async () => (await api.get<DetallePromocion>(`/admin/promociones/${id}`)).data,
  })

export function useAccionesPromocion() {
  const refrescar = useRefresco()
  return {
    guardar: useMutation({
      mutationFn: async (p: { id: number | null; datos: DatosPromocion }) =>
        p.id === null
          ? (await api.post<Promocion>('/admin/promociones', p.datos)).data
          : (await api.put<Promocion>(`/admin/promociones/${p.id}`, p.datos)).data,
      onSuccess: refrescar,
    }),
    cambiarActiva: useMutation({
      mutationFn: async (p: { id: number; activa: boolean }) =>
        (await api.post<Promocion>(`/admin/promociones/${p.id}/estado`, { activa: p.activa })).data,
      onSuccess: refrescar,
    }),
  }
}

export interface DatosRegla {
  nombre: string
  descripcion?: string
  reservasRequeridas: number
  tipo: string
  valor: number
  vigenciaDias: number
  activa: boolean
}

export const useReglas = () =>
  useQuery({
    queryKey: ['premios', 'reglas'],
    queryFn: async () => (await api.get<ReglaPremio[]>('/admin/premios/reglas')).data,
  })

export const usePremiosOtorgados = (estado: EstadoPremio | '') =>
  useQuery({
    queryKey: ['premios', 'otorgados', estado],
    queryFn: async () =>
      (
        await api.get<Premio[]>('/admin/premios/otorgados', {
          params: { estado: estado || undefined },
        })
      ).data,
  })

export function useAccionesPremios() {
  const refrescar = useRefresco()
  return {
    guardarRegla: useMutation({
      mutationFn: async (p: { id: number | null; datos: DatosRegla }) =>
        p.id === null
          ? (await api.post<ReglaPremio>('/admin/premios/reglas', p.datos)).data
          : (await api.put<ReglaPremio>(`/admin/premios/reglas/${p.id}`, p.datos)).data,
      onSuccess: refrescar,
    }),
    revocar: useMutation({
      mutationFn: async (p: { id: number; motivo: string }) => {
        await api.post(`/admin/premios/otorgados/${p.id}/revocar`, { motivo: p.motivo })
      },
      onSuccess: refrescar,
    }),
  }
}
