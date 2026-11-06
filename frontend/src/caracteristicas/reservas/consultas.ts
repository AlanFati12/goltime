import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { api, type Pagina } from '@/lib/api'
import { useValorConRetraso } from '@/lib/useValorConRetraso'

import type {
  DisponibilidadDia,
  EstadoReserva,
  Notificacion,
  PestanaReservas,
  PrevisualizacionReprogramacion,
  Reserva,
  ResumenDia,
} from './tipos'

// ---------- Disponibilidad (pública) ----------

export function useDisponibilidad(sedeId: number | null, fecha: string, modalidad: string) {
  return useQuery({
    queryKey: ['disponibilidad', sedeId, fecha, modalidad],
    queryFn: async () =>
      (
        await api.get<DisponibilidadDia>('/publico/disponibilidad', {
          params: { sedeId, fecha, modalidad: modalidad || undefined },
        })
      ).data,
    enabled: sedeId !== null,
    placeholderData: keepPreviousData,
  })
}

export function useSemana(sedeId: number | null, modalidad: string) {
  return useQuery({
    queryKey: ['disponibilidad', 'semana', sedeId, modalidad],
    queryFn: async () =>
      (
        await api.get<ResumenDia[]>('/publico/disponibilidad/semana', {
          params: { sedeId, modalidad: modalidad || undefined },
        })
      ).data,
    enabled: sedeId !== null,
  })
}

// ---------- Cliente ----------

const refrescar = (cliente: ReturnType<typeof useQueryClient>) => {
  void cliente.invalidateQueries({ queryKey: ['reservas'] })
  void cliente.invalidateQueries({ queryKey: ['disponibilidad'] })
  void cliente.invalidateQueries({ queryKey: ['notificaciones'] })
}

export function useMisReservas(pestana: PestanaReservas, pagina: number) {
  return useQuery({
    queryKey: ['reservas', 'mias', pestana, pagina],
    queryFn: async () =>
      (await api.get<Pagina<Reserva>>('/me/reservas', { params: { pestana, pagina, tamano: 10 } }))
        .data,
    placeholderData: keepPreviousData,
  })
}

export function useReserva(id: number) {
  return useQuery({
    queryKey: ['reservas', 'detalle', id],
    queryFn: async () => (await api.get<Reserva>(`/reservas/${id}`)).data,
  })
}

export interface DatosSolicitud {
  canchaId: number
  inicio: string
  fin: string
  nombreEquipo?: string
  notas?: string
  codigoPromocion?: string
  premioId?: number
}

export function useSolicitarReserva() {
  const cliente = useQueryClient()
  return useMutation({
    /** La clave de idempotencia evita duplicar la reserva si se pulsa dos veces el botón. */
    mutationFn: async (p: { datos: DatosSolicitud; clave: string }) =>
      (await api.post<Reserva>('/reservas', p.datos, { headers: { 'Idempotency-Key': p.clave } }))
        .data,
    onSettled: () => refrescar(cliente),
  })
}

export function useCancelarReserva() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (p: { id: number; motivo?: string }) =>
      (await api.post<Reserva>(`/reservas/${p.id}/cancelar`, { motivo: p.motivo })).data,
    onSuccess: () => refrescar(cliente),
  })
}

export interface DatosReprogramacion {
  canchaId?: number
  inicio: string
  fin: string
}

export function usePrevisualizarReprogramacion() {
  return useMutation({
    mutationFn: async (p: { id: number; datos: DatosReprogramacion; admin?: boolean }) =>
      (
        await api.post<PrevisualizacionReprogramacion>(
          `${p.admin ? '/admin' : ''}/reservas/${p.id}/reprogramar/previsualizar`,
          p.admin ? { ...p.datos, motivo: 'previsualización' } : p.datos,
        )
      ).data,
  })
}

export function useReprogramarReserva() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (p: {
      id: number
      datos: DatosReprogramacion
      admin?: boolean
      motivo?: string
    }) =>
      (
        await api.patch<Reserva>(
          `${p.admin ? '/admin' : ''}/reservas/${p.id}/reprogramar`,
          p.admin ? { ...p.datos, motivo: p.motivo } : p.datos,
        )
      ).data,
    onSuccess: () => refrescar(cliente),
  })
}

// ---------- Administración ----------

export interface FiltrosReservasAdmin {
  estado: EstadoReserva | ''
  busqueda: string
  sedeId: string
  estadoPago: string
  origen: string
  desde: string
  hasta: string
  pagina: number
}

export const FILTROS_RESERVAS_INICIALES: FiltrosReservasAdmin = {
  estado: 'PENDIENTE',
  busqueda: '',
  sedeId: '',
  estadoPago: '',
  origen: '',
  desde: '',
  hasta: '',
  pagina: 0,
}

/** Parámetros de consulta (sin la paginación) de los filtros activos; los usan el listado y la exportación. */
export function paramsDeFiltros(f: FiltrosReservasAdmin): Record<string, string> {
  const params: Record<string, string> = {}
  if (f.estado) params.estado = f.estado
  if (f.busqueda.trim()) params.busqueda = f.busqueda.trim()
  if (f.sedeId) params.sedeId = f.sedeId
  if (f.estadoPago) params.estadoPago = f.estadoPago
  if (f.origen) params.origen = f.origen
  if (f.desde) params.desde = f.desde
  if (f.hasta) params.hasta = f.hasta
  return params
}

export function useReservasAdmin(entrada: FiltrosReservasAdmin) {
  // La búsqueda sale al servidor cuando la persona deja de escribir, no por cada tecla.
  const busqueda = useValorConRetraso(entrada.busqueda)
  const filtros = { ...entrada, busqueda }
  return useQuery({
    queryKey: ['reservas', 'admin', filtros],
    queryFn: async () =>
      (
        await api.get<Pagina<Reserva>>('/admin/reservas', {
          params: { ...paramsDeFiltros(filtros), pagina: filtros.pagina, tamano: 15 },
        })
      ).data,
    placeholderData: keepPreviousData,
    refetchInterval: 30_000,
  })
}

export function useAccionesReservaAdmin() {
  const cliente = useQueryClient()
  const alTerminar = () => refrescar(cliente)
  return {
    aprobar: useMutation({
      mutationFn: async (id: number) =>
        (await api.post<Reserva>(`/admin/reservas/${id}/aprobar`)).data,
      onSuccess: alTerminar,
    }),
    rechazar: useMutation({
      mutationFn: async (p: { id: number; motivo: string }) =>
        (await api.post<Reserva>(`/admin/reservas/${p.id}/rechazar`, { motivo: p.motivo })).data,
      onSuccess: alTerminar,
    }),
    cancelar: useMutation({
      mutationFn: async (p: { id: number; motivo: string }) =>
        (await api.post<Reserva>(`/admin/reservas/${p.id}/cancelar`, { motivo: p.motivo })).data,
      onSuccess: alTerminar,
    }),
  }
}

// ---------- Notificaciones ----------

export function useContadorNotificaciones(habilitado: boolean) {
  return useQuery({
    queryKey: ['notificaciones', 'contador'],
    queryFn: async () =>
      (await api.get<{ noLeidas: number }>('/me/notificaciones/contador')).data.noLeidas,
    enabled: habilitado,
    refetchInterval: 30_000,
    staleTime: 10_000,
  })
}

/** `habilitado` evita pedir la lista cuando no hace falta (visitante sin sesión o desplegable cerrado). */
export function useNotificaciones(pagina: number, tamano = 20, habilitado = true) {
  return useQuery({
    queryKey: ['notificaciones', 'lista', pagina, tamano],
    queryFn: async () =>
      (await api.get<Pagina<Notificacion>>('/me/notificaciones', { params: { pagina, tamano } }))
        .data,
    placeholderData: keepPreviousData,
    enabled: habilitado,
  })
}

type ListaNotificaciones = [readonly unknown[], Pagina<Notificacion> | undefined][]

/**
 * Marcar como leída es una acción sin riesgo y casi nunca falla: se refleja de inmediato en la pantalla y,
 * si el servidor la rechaza, se deshace y se avisa. (Las reservas y los pagos NO se tratan así: ahí el
 * servidor decide y la pantalla espera su respuesta.)
 */
export function useAccionesNotificaciones() {
  const cliente = useQueryClient()
  const refrescar = () => void cliente.invalidateQueries({ queryKey: ['notificaciones'] })

  /** Aplica el cambio en el caché y devuelve cómo estaba para poder deshacerlo. */
  const aplicar = async (
    transformar: (n: Notificacion) => Notificacion,
    restar: (n: number) => number,
  ) => {
    await cliente.cancelQueries({ queryKey: ['notificaciones'] })
    const listas: ListaNotificaciones = cliente.getQueriesData<Pagina<Notificacion>>({
      queryKey: ['notificaciones', 'lista'],
    })
    const contador = cliente.getQueryData<number>(['notificaciones', 'contador'])
    for (const [clave, pagina] of listas) {
      if (pagina)
        cliente.setQueryData(clave, { ...pagina, contenido: pagina.contenido.map(transformar) })
    }
    if (contador !== undefined)
      cliente.setQueryData(['notificaciones', 'contador'], restar(contador))
    return { listas, contador }
  }
  const deshacer = (previo?: { listas: ListaNotificaciones; contador?: number }) => {
    if (!previo) return
    for (const [clave, pagina] of previo.listas) cliente.setQueryData(clave, pagina)
    if (previo.contador !== undefined)
      cliente.setQueryData(['notificaciones', 'contador'], previo.contador)
    avisar.error('No se pudo marcar la notificación. Volvió a quedar como estaba.')
  }

  return {
    leer: useMutation({
      mutationFn: async (id: number) => api.post(`/me/notificaciones/${id}/leer`),
      onMutate: (id) =>
        aplicar(
          (n) => (n.id === id ? { ...n, leida: true } : n),
          (c) => Math.max(0, c - 1),
        ),
      onError: (_e, _id, previo) => deshacer(previo),
      onSettled: refrescar,
    }),
    leerTodas: useMutation({
      mutationFn: async () => api.post('/me/notificaciones/leer-todas'),
      onMutate: () =>
        aplicar(
          (n) => ({ ...n, leida: true }),
          () => 0,
        ),
      onError: (_e, _v, previo) => deshacer(previo),
      onSettled: refrescar,
    }),
  }
}
