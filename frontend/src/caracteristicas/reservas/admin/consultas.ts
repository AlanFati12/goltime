import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { api, type Pagina } from '@/lib/api'

import type {
  Agenda,
  Bloqueo,
  ClienteMostrador,
  EstadoReserva,
  MetodoPago,
  Pago,
  PrevisualizacionBloqueo,
  PrevisualizacionEstado,
  RegistroAuditado,
  Reserva,
} from '../tipos'

const refrescar = (cliente: ReturnType<typeof useQueryClient>) => {
  void cliente.invalidateQueries({ queryKey: ['reservas'] })
  void cliente.invalidateQueries({ queryKey: ['agenda'] })
  void cliente.invalidateQueries({ queryKey: ['disponibilidad'] })
  void cliente.invalidateQueries({ queryKey: ['notificaciones'] })
}

// ---------- Reservas ----------

export const useReservaAdmin = (id: number) =>
  useQuery({
    queryKey: ['reservas', 'admin-detalle', id],
    queryFn: async () => (await api.get<Reserva>(`/admin/reservas/${id}`)).data,
  })

export const usePagos = (id: number) =>
  useQuery({
    queryKey: ['reservas', 'pagos', id],
    queryFn: async () => (await api.get<Pago[]>(`/admin/reservas/${id}/pagos`)).data,
  })

export interface DatosReservaAdmin {
  canchaId: number
  inicio: string
  fin: string
  clienteId?: number
  mostrador?: { nombre: string; telefono: string }
  nombreEquipo?: string
  notas?: string
}

export function useCrearReservaAdmin() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (datos: DatosReservaAdmin) =>
      (await api.post<Reserva>('/admin/reservas', datos)).data,
    onSuccess: () => refrescar(cliente),
  })
}

export function useAccionesPago() {
  const cliente = useQueryClient()
  const alTerminar = () => refrescar(cliente)
  return {
    pagar: useMutation({
      mutationFn: async (p: {
        id: number
        monto: number
        metodo: MetodoPago
        referencia?: string
      }) =>
        (
          await api.post<Reserva>(`/admin/reservas/${p.id}/pagos`, {
            monto: p.monto,
            metodo: p.metodo,
            referencia: p.referencia || undefined,
          })
        ).data,
      onSuccess: alTerminar,
    }),
    reembolsar: useMutation({
      mutationFn: async (p: {
        id: number
        monto: number
        metodo: MetodoPago
        referencia?: string
        cerrar: boolean
      }) =>
        (
          await api.post<Reserva>(`/admin/reservas/${p.id}/reembolsos`, {
            monto: p.monto,
            metodo: p.metodo,
            referencia: p.referencia || undefined,
            cerrar: p.cerrar,
          })
        ).data,
      onSuccess: alTerminar,
    }),
  }
}

// ---------- Cambio de estado (ADR 0019) ----------

/** Qué pasaría al llevar la reserva a {@code estado}; no consulta nada mientras no se elija uno. */
export const usePrevisualizacionEstado = (id: number, estado: EstadoReserva | null) =>
  useQuery({
    queryKey: ['reservas', 'previsualizar-estado', id, estado],
    queryFn: async () =>
      (
        await api.get<PrevisualizacionEstado>(`/admin/reservas/${id}/estado/previsualizar`, {
          params: { estado },
        })
      ).data,
    enabled: estado !== null,
  })

export function useCambiarEstadoReserva() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (p: { id: number; estado: EstadoReserva; motivo: string }) =>
      (
        await api.patch<Reserva>(`/admin/reservas/${p.id}/estado`, {
          estado: p.estado,
          motivo: p.motivo,
        })
      ).data,
    onSuccess: () => {
      refrescar(cliente)
      void cliente.invalidateQueries({ queryKey: ['premios'] })
    },
  })
}

export function useBuscarClientesMostrador(busqueda: string) {
  return useQuery({
    queryKey: ['mostrador', busqueda],
    queryFn: async () =>
      (await api.get<ClienteMostrador[]>('/admin/clientes-mostrador', { params: { busqueda } }))
        .data,
    enabled: busqueda.trim().length >= 2,
  })
}

/** Descarga el listado filtrado como CSV (la petición lleva el token, por eso no es un simple enlace). */
export async function exportarReservasCsv(params: Record<string, string | number>) {
  const r = await api.get<Blob>('/admin/reservas/exportar', { params, responseType: 'blob' })
  const url = URL.createObjectURL(r.data)
  const enlace = document.createElement('a')
  enlace.href = url
  enlace.download = 'reservas-goltime.csv'
  enlace.click()
  URL.revokeObjectURL(url)
}

// ---------- Agenda ----------

export function useAgenda(sedeId: number | null, desde: string, hasta: string) {
  return useQuery({
    queryKey: ['agenda', sedeId, desde, hasta],
    queryFn: async () =>
      (await api.get<Agenda>('/admin/agenda', { params: { sedeId, desde, hasta } })).data,
    enabled: sedeId !== null,
    placeholderData: keepPreviousData,
    refetchInterval: 60_000,
  })
}

// ---------- Bloqueos ----------

export interface DatosBloqueo {
  sedeId: number
  canchaIds: number[]
  tipo: 'PUNTUAL' | 'RECURRENTE'
  inicio?: string
  fin?: string
  todoElDia?: boolean
  diasSemana?: number[]
  horaInicio?: string
  horaFin?: string
  fechaDesde?: string
  fechaHasta?: string
  motivo: string
  detalle?: string
}

export function useBloqueos(sedeId: number | null, soloActivos: boolean) {
  return useQuery({
    queryKey: ['bloqueos', sedeId, soloActivos],
    queryFn: async () =>
      (
        await api.get<Bloqueo[]>('/admin/bloqueos', {
          params: { sedeId: sedeId ?? undefined, soloActivos },
        })
      ).data,
  })
}

export function useAccionesBloqueo() {
  const cliente = useQueryClient()
  const alTerminar = () => {
    void cliente.invalidateQueries({ queryKey: ['bloqueos'] })
    refrescar(cliente)
  }
  return {
    previsualizar: useMutation({
      mutationFn: async (d: DatosBloqueo) =>
        (await api.post<PrevisualizacionBloqueo>('/admin/bloqueos/previsualizar', d)).data,
    }),
    guardar: useMutation({
      mutationFn: async (p: {
        id: number | null
        datos: DatosBloqueo
        huellaConfirmada?: string
      }) => {
        const cuerpo = { ...p.datos, huellaConfirmada: p.huellaConfirmada }
        return (
          p.id === null
            ? await api.post<Bloqueo>('/admin/bloqueos', cuerpo)
            : await api.put<Bloqueo>(`/admin/bloqueos/${p.id}`, cuerpo)
        ).data
      },
      onSuccess: alTerminar,
    }),
    eliminar: useMutation({
      mutationFn: async (id: number) => api.delete(`/admin/bloqueos/${id}`),
      onSuccess: alTerminar,
    }),
  }
}

// ---------- Auditoría ----------

export interface FiltrosAuditoria {
  entidad: string
  accion: string
  desde: string
  hasta: string
  pagina: number
}

export function useAuditoria(f: FiltrosAuditoria) {
  return useQuery({
    queryKey: ['auditoria', f],
    queryFn: async () => {
      const params: Record<string, string | number> = { pagina: f.pagina, tamano: 20 }
      if (f.entidad) params.entidad = f.entidad
      if (f.accion) params.accion = f.accion
      if (f.desde) params.desde = f.desde
      if (f.hasta) params.hasta = f.hasta
      return (await api.get<Pagina<RegistroAuditado>>('/admin/auditoria', { params })).data
    },
    placeholderData: keepPreviousData,
  })
}

export const useFiltrosAuditoria = () =>
  useQuery({
    queryKey: ['auditoria', 'filtros'],
    queryFn: async () =>
      (await api.get<{ entidades: string[]; acciones: string[] }>('/admin/auditoria/filtros')).data,
  })
