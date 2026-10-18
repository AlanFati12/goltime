import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { api } from '@/lib/api'

import type {
  Cancha,
  Cotizacion,
  EstadoCancha,
  Festivo,
  Foto,
  HorarioDia,
  PrevisualizacionEstado,
  Sede,
  Tarifa,
} from './tipos'

// ---------- Consultas públicas ----------

export const useSedesPublicas = () =>
  useQuery({
    queryKey: ['sedes', 'publicas'],
    queryFn: async () => (await api.get<Sede[]>('/publico/sedes')).data,
  })

export const useSedePublica = (id: number) =>
  useQuery({
    queryKey: ['sedes', 'publica', id],
    queryFn: async () => (await api.get<Sede>(`/publico/sedes/${id}`)).data,
  })

export const useCanchaPublica = (id: number) =>
  useQuery({
    queryKey: ['canchas', 'publica', id],
    queryFn: async () => (await api.get<Cancha>(`/publico/canchas/${id}`)).data,
  })

export function useCotizacion(canchaId: number, inicio: string | null, fin: string | null) {
  return useQuery({
    queryKey: ['cotizacion', canchaId, inicio, fin],
    queryFn: async () =>
      (
        await api.get<Cotizacion>(`/publico/canchas/${canchaId}/cotizacion`, {
          params: { inicio, fin },
        })
      ).data,
    enabled: !!inicio && !!fin,
    retry: false,
  })
}

// ---------- Administración ----------

const refrescarCatalogo = (cliente: ReturnType<typeof useQueryClient>) => {
  void cliente.invalidateQueries({ queryKey: ['admin', 'sedes'] })
  void cliente.invalidateQueries({ queryKey: ['admin', 'canchas'] })
  void cliente.invalidateQueries({ queryKey: ['sedes'] })
  void cliente.invalidateQueries({ queryKey: ['canchas'] })
}

export const useSedesAdmin = () =>
  useQuery({
    queryKey: ['admin', 'sedes'],
    queryFn: async () => (await api.get<Sede[]>('/admin/sedes')).data,
  })

export const useSedeAdmin = (id: number | null) =>
  useQuery({
    queryKey: ['admin', 'sedes', id],
    queryFn: async () => (await api.get<Sede>(`/admin/sedes/${id}`)).data,
    enabled: id !== null,
  })

export const useCanchasAdmin = (sedeId: number | null) =>
  useQuery({
    queryKey: ['admin', 'canchas', { sedeId }],
    queryFn: async () =>
      (await api.get<Cancha[]>('/admin/canchas', { params: sedeId ? { sedeId } : {} })).data,
  })

export const useCanchaAdmin = (id: number | null) =>
  useQuery({
    queryKey: ['admin', 'canchas', id],
    queryFn: async () => (await api.get<Cancha>(`/admin/canchas/${id}`)).data,
    enabled: id !== null,
  })

export interface DatosSedeForm {
  nombre: string
  direccion: string
  ciudad: string
  telefono?: string
  whatsapp?: string
  descripcion?: string
  servicios: string[]
  latitud?: number
  longitud?: number
}

export function useGuardarSede() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (p: { id: number | null; datos: DatosSedeForm }) =>
      (p.id === null
        ? await api.post<Sede>('/admin/sedes', p.datos)
        : await api.put<Sede>(`/admin/sedes/${p.id}`, p.datos)
      ).data,
    onSuccess: () => refrescarCatalogo(cliente),
  })
}

export function useGuardarHorarioSede() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (p: { id: number; dias: HorarioDia[] }) =>
      (await api.put<Sede>(`/admin/sedes/${p.id}/horario`, { dias: p.dias })).data,
    onSuccess: () => refrescarCatalogo(cliente),
  })
}

export interface DatosCanchaForm {
  nombre: string
  jugadoresPorEquipo: number
  capacidadTotal?: number
  superficie?: string
  largoM?: number
  anchoM?: number
  techada: boolean
  iluminacion: boolean
  precioBaseHora: number
  duracionBloqueMin: number
  duracionMinMin: number
  duracionMaxMin: number
  descripcion?: string
  orden?: number
  componentes: number[]
}

export function useGuardarCancha() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (p: { id: number | null; sedeId: number; datos: DatosCanchaForm }) =>
      (p.id === null
        ? await api.post<Cancha>(`/admin/sedes/${p.sedeId}/canchas`, p.datos)
        : await api.put<Cancha>(`/admin/canchas/${p.id}`, p.datos)
      ).data,
    onSuccess: () => refrescarCatalogo(cliente),
  })
}

export function useHorarioCancha() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (p: { id: number; dias: HorarioDia[] | null }) =>
      (p.dias === null
        ? await api.delete<Cancha>(`/admin/canchas/${p.id}/horario`)
        : await api.put<Cancha>(`/admin/canchas/${p.id}/horario`, { dias: p.dias })
      ).data,
    onSuccess: () => refrescarCatalogo(cliente),
  })
}

export function useEstado() {
  const cliente = useQueryClient()
  return {
    previsualizarSede: async (id: number, activa: boolean) =>
      (
        await api.post<PrevisualizacionEstado>(`/admin/sedes/${id}/estado/previsualizar`, {
          activa,
        })
      ).data,
    previsualizarCancha: async (id: number, estado: EstadoCancha) =>
      (
        await api.post<PrevisualizacionEstado>(`/admin/canchas/${id}/estado/previsualizar`, {
          estado,
        })
      ).data,
    cambiarSede: useMutation({
      mutationFn: async (p: {
        id: number
        activa: boolean
        motivo?: string
        confirmarConflictos: boolean
      }) => (await api.post<Sede>(`/admin/sedes/${p.id}/estado`, p)).data,
      onSuccess: () => refrescarCatalogo(cliente),
    }),
    cambiarCancha: useMutation({
      mutationFn: async (p: {
        id: number
        estado: EstadoCancha
        motivo?: string
        confirmarConflictos: boolean
      }) => (await api.post<Cancha>(`/admin/canchas/${p.id}/estado`, p)).data,
      onSuccess: () => refrescarCatalogo(cliente),
    }),
  }
}

export interface DatosTarifaForm {
  diasSemana: number[]
  horaInicio: string
  horaFin: string
  aplicaFestivo: boolean
  precioHora: number
  vigenteDesde?: string
  vigenteHasta?: string
}

export function useTarifas() {
  const cliente = useQueryClient()
  const alTerminar = () => refrescarCatalogo(cliente)
  return {
    crear: useMutation({
      mutationFn: async (p: { canchaId: number; datos: DatosTarifaForm }) =>
        (await api.post<Tarifa>(`/admin/canchas/${p.canchaId}/tarifas`, p.datos)).data,
      onSuccess: alTerminar,
    }),
    eliminar: useMutation({
      mutationFn: async (id: number) => api.delete(`/admin/tarifas/${id}`),
      onSuccess: alTerminar,
    }),
  }
}

export function useFotos() {
  const cliente = useQueryClient()
  const alTerminar = () => refrescarCatalogo(cliente)
  const subir = async (ruta: string, archivo: File) => {
    const datos = new FormData()
    datos.append('archivo', archivo)
    return (await api.post<Foto>(ruta, datos)).data
  }
  return {
    subirASede: useMutation({
      mutationFn: (p: { id: number; archivo: File }) =>
        subir(`/admin/sedes/${p.id}/fotos`, p.archivo),
      onSuccess: alTerminar,
    }),
    subirACancha: useMutation({
      mutationFn: (p: { id: number; archivo: File }) =>
        subir(`/admin/canchas/${p.id}/fotos`, p.archivo),
      onSuccess: alTerminar,
    }),
    quitarDeSede: useMutation({
      mutationFn: (p: { id: number; fotoId: number }) =>
        api.delete(`/admin/sedes/${p.id}/fotos/${p.fotoId}`),
      onSuccess: alTerminar,
    }),
    quitarDeCancha: useMutation({
      mutationFn: (p: { id: number; fotoId: number }) =>
        api.delete(`/admin/canchas/${p.id}/fotos/${p.fotoId}`),
      onSuccess: alTerminar,
    }),
  }
}

export function useFestivos(anio: number) {
  return useQuery({
    queryKey: ['admin', 'festivos', anio],
    queryFn: async () => (await api.get<Festivo[]>('/admin/festivos', { params: { anio } })).data,
  })
}

export function useAccionesFestivos() {
  const cliente = useQueryClient()
  const alTerminar = () => cliente.invalidateQueries({ queryKey: ['admin', 'festivos'] })
  return {
    crear: useMutation({
      mutationFn: async (d: { fecha: string; nombre: string }) =>
        (await api.post<Festivo>('/admin/festivos', d)).data,
      onSuccess: alTerminar,
    }),
    eliminar: useMutation({
      mutationFn: async (id: number) => api.delete(`/admin/festivos/${id}`),
      onSuccess: alTerminar,
    }),
  }
}
