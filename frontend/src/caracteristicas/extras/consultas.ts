import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { api } from '@/lib/api'

// ===================== Favoritas =====================

export interface Favorita {
  canchaId: number
  cancha: string
  modalidad: string
  sedeId: number
  sede: string
  precioBaseHora: number
  activa: boolean
}

export const useFavoritas = (habilitado = true) =>
  useQuery({
    queryKey: ['extras', 'favoritas'],
    enabled: habilitado,
    queryFn: async () => (await api.get<Favorita[]>('/me/favoritos')).data,
  })

export function useAlternarFavorita() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (p: { canchaId: number; marcar: boolean }) => {
      if (p.marcar) await api.put(`/me/favoritos/${p.canchaId}`)
      else await api.delete(`/me/favoritos/${p.canchaId}`)
    },
    onSuccess: () => void cliente.invalidateQueries({ queryKey: ['extras', 'favoritas'] }),
  })
}

// ===================== Lista de espera =====================

export interface Espera {
  id: number
  canchaId: number
  cancha: string
  sede: string
  inicio: string
  fin: string
  estado: 'ESPERANDO' | 'NOTIFICADA' | 'CUMPLIDA' | 'CANCELADA'
  creadoEn: string
}

export const useMisEsperas = (habilitado = true) =>
  useQuery({
    queryKey: ['extras', 'esperas'],
    enabled: habilitado,
    queryFn: async () => (await api.get<Espera[]>('/me/lista-espera')).data,
  })

export function useAccionesEspera() {
  const cliente = useQueryClient()
  const refrescar = () => void cliente.invalidateQueries({ queryKey: ['extras', 'esperas'] })
  return {
    unirse: useMutation({
      mutationFn: async (p: { canchaId: number; inicio: string; fin: string }) =>
        (await api.post<Espera>('/me/lista-espera', p)).data,
      onSuccess: refrescar,
    }),
    salir: useMutation({
      mutationFn: async (id: number) => {
        await api.delete(`/me/lista-espera/${id}`)
      },
      onSuccess: refrescar,
    }),
  }
}

// ===================== Calificaciones =====================

export interface Calificacion {
  id: number
  reservaId: number
  estrellas: number
  comentario: string | null
}

export interface EstadoCalificacion {
  puedeCalificar: boolean
  motivo?: string
  calificacion?: Calificacion
}

export interface ResumenCalificaciones {
  promedio: number
  total: number
  distribucion: number[]
  ultimas: { estrellas: number; comentario: string; autor: string; fecha: string }[]
}

export const useEstadoCalificacion = (reservaId: number) =>
  useQuery({
    queryKey: ['extras', 'calificacion', reservaId],
    queryFn: async () =>
      (await api.get<EstadoCalificacion>(`/reservas/${reservaId}/calificacion`)).data,
  })

export const useCalificacionesCancha = (canchaId: number) =>
  useQuery({
    queryKey: ['extras', 'calificaciones', canchaId],
    queryFn: async () =>
      (await api.get<ResumenCalificaciones>(`/publico/canchas/${canchaId}/calificaciones`)).data,
  })

export function useCalificar(reservaId: number) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (p: { estrellas: number; comentario?: string }) =>
      (await api.put<Calificacion>(`/reservas/${reservaId}/calificacion`, p)).data,
    onSuccess: () => void cliente.invalidateQueries({ queryKey: ['extras'] }),
  })
}

/** Descarga el evento de calendario (.ics) de una reserva propia. */
export async function descargarCalendario(reservaId: number, codigo: string): Promise<void> {
  const r = await api.get<Blob>(`/reservas/${reservaId}/calendario.ics`, { responseType: 'blob' })
  const url = URL.createObjectURL(r.data)
  const a = document.createElement('a')
  a.href = url
  a.download = `reserva-${codigo}.ics`
  a.click()
  URL.revokeObjectURL(url)
}

// ===================== Solicitudes de inscripción a torneos =====================

export interface SolicitudTorneo {
  inscripcionId: number
  torneoId: number
  torneo: string
  equipo: string
  estado: 'SOLICITADA' | 'APROBADA' | 'RECHAZADA' | 'RETIRADA'
  creadaEn: string
}

export const useMisSolicitudes = (habilitado = true) =>
  useQuery({
    queryKey: ['extras', 'solicitudes'],
    enabled: habilitado,
    queryFn: async () =>
      (
        await api.get<{ habilitado: boolean; solicitudes: SolicitudTorneo[] }>(
          '/me/solicitudes-torneo',
        )
      ).data,
  })

export interface DatosSolicitudTorneo {
  nombreEquipo: string
  color?: string
  delegadoTelefono?: string
  jugadores: {
    nombres: string
    apellidos: string
    documento?: string
    posicion?: string
    dorsal?: number
  }[]
}

export function useAccionesSolicitud() {
  const cliente = useQueryClient()
  const refrescar = () => {
    void cliente.invalidateQueries({ queryKey: ['extras'] })
    void cliente.invalidateQueries({ queryKey: ['torneos'] })
  }
  return {
    solicitar: useMutation({
      mutationFn: async (p: { torneoId: number; datos: DatosSolicitudTorneo }) =>
        (await api.post<SolicitudTorneo>(`/torneos/${p.torneoId}/solicitudes`, p.datos)).data,
      onSuccess: refrescar,
    }),
    retirar: useMutation({
      mutationFn: async (inscripcionId: number) => {
        await api.delete(`/me/solicitudes-torneo/${inscripcionId}`)
      },
      onSuccess: refrescar,
    }),
    aprobar: useMutation({
      mutationFn: async (p: { torneoId: number; inscripcionId: number }) => {
        await api.post(`/admin/torneos/${p.torneoId}/solicitudes/${p.inscripcionId}/aprobar`)
      },
      onSuccess: refrescar,
    }),
    rechazar: useMutation({
      mutationFn: async (p: { torneoId: number; inscripcionId: number; motivo: string }) => {
        await api.post(`/admin/torneos/${p.torneoId}/solicitudes/${p.inscripcionId}/rechazar`, {
          motivo: p.motivo,
        })
      },
      onSuccess: refrescar,
    }),
  }
}

// ===================== Series de reservas (admin) =====================

export interface DatosSerie {
  canchaId: number
  inicio: string
  fin: string
  semanas: number
  clienteId?: number
  mostradorNombre?: string
  mostradorTelefono?: string
  nombreEquipo?: string
  notas?: string
  omitirOcupadas?: boolean
}

export interface OcurrenciaSerie {
  numero: number
  inicio: string
  fin: string
  estado: 'DISPONIBLE' | 'OCUPADA' | 'FUERA_DE_HORARIO'
  motivo?: string
  precio: number
}

export interface ResumenSerie {
  id: number
  canchaId: number
  cancha: string
  cliente: string | null
  diaSemana: number
  minutoInicio: number
  duracionMin: number
  semanasPedidas: number
  reservasActivas: number
  reservasFuturas: number
  creadaEn: string
}

export const useSeries = () =>
  useQuery({
    queryKey: ['extras', 'series'],
    queryFn: async () => (await api.get<ResumenSerie[]>('/admin/series')).data,
  })

export function useAccionesSerie() {
  const cliente = useQueryClient()
  const refrescar = () => {
    void cliente.invalidateQueries({ queryKey: ['extras', 'series'] })
    void cliente.invalidateQueries({ queryKey: ['reservas'] })
    void cliente.invalidateQueries({ queryKey: ['agenda'] })
  }
  return {
    previsualizar: useMutation({
      mutationFn: async (d: DatosSerie) =>
        (await api.post<OcurrenciaSerie[]>('/admin/series/previsualizar', d)).data,
    }),
    crear: useMutation({
      mutationFn: async (d: DatosSerie) =>
        (
          await api.post<{ serieId: number; reservas: unknown[]; omitidas: OcurrenciaSerie[] }>(
            '/admin/series',
            d,
          )
        ).data,
      onSuccess: refrescar,
    }),
    cancelar: useMutation({
      mutationFn: async (p: { id: number; motivo: string }) =>
        (
          await api.post<{ canceladas: number }>(`/admin/series/${p.id}/cancelar`, {
            motivo: p.motivo,
          })
        ).data,
      onSuccess: refrescar,
    }),
  }
}
