import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import type { Usuario } from '@/caracteristicas/autenticacion/almacenSesion'
import type { Reserva } from '@/caracteristicas/reservas/tipos'
import { api, type Pagina } from '@/lib/api'
import { useValorConRetraso } from '@/lib/useValorConRetraso'

export interface FiltrosClientes {
  busqueda: string
  activo: 'todos' | 'activos' | 'inactivos'
  pagina: number
}

const TAMANO = 15

export function useClientes(entrada: FiltrosClientes) {
  // La búsqueda sale al servidor cuando la persona deja de escribir, no por cada tecla.
  const busqueda = useValorConRetraso(entrada.busqueda)
  const filtros = { ...entrada, busqueda }
  return useQuery({
    queryKey: ['admin', 'clientes', filtros],
    queryFn: async () => {
      const params: Record<string, string | number> = { pagina: filtros.pagina, tamano: TAMANO }
      if (filtros.busqueda.trim()) params.busqueda = filtros.busqueda.trim()
      if (filtros.activo !== 'todos')
        params.activo = filtros.activo === 'activos' ? 'true' : 'false'
      return (await api.get<Pagina<Usuario>>('/admin/clientes', { params })).data
    },
    placeholderData: (anterior) => anterior,
  })
}

export interface Previsualizacion {
  reservasFuturasActivas: number
  requiereConfirmacion: boolean
}

export function usePrevisualizarDesactivacion(clienteId: number | null) {
  return useQuery({
    queryKey: ['admin', 'clientes', clienteId, 'previsualizar'],
    queryFn: async () =>
      (await api.post<Previsualizacion>(`/admin/clientes/${clienteId}/desactivar/previsualizar`))
        .data,
    enabled: clienteId !== null,
    gcTime: 0,
  })
}

/** Mutaciones sobre clientes; todas refrescan el listado al terminar. */
export function useAccionesCliente() {
  const cliente = useQueryClient()
  const refrescar = () => cliente.invalidateQueries({ queryKey: ['admin', 'clientes'] })
  return {
    desactivar: useMutation({
      mutationFn: async (p: { id: number; motivo: string; confirmarConflictos: boolean }) =>
        (
          await api.post<Usuario>(`/admin/clientes/${p.id}/desactivar`, {
            motivo: p.motivo,
            confirmarConflictos: p.confirmarConflictos,
          })
        ).data,
      onSuccess: refrescar,
    }),
    activar: useMutation({
      mutationFn: async (id: number) =>
        (await api.post<Usuario>(`/admin/clientes/${id}/activar`)).data,
      onSuccess: refrescar,
    }),
    inscripcion: useMutation({
      mutationFn: async (p: { id: number; habilitado: boolean }) =>
        (
          await api.patch<Usuario>(`/admin/clientes/${p.id}/inscripcion-equipos`, {
            habilitado: p.habilitado,
          })
        ).data,
      onSuccess: refrescar,
    }),
  }
}

export function useAdministradores() {
  return useQuery({
    queryKey: ['admin', 'administradores'],
    queryFn: async () => (await api.get<Usuario[]>('/admin/administradores')).data,
  })
}

export function useCrearAdministrador() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (datos: { nombres: string; apellidos: string; correo: string }) =>
      (await api.post<Usuario>('/admin/administradores', datos)).data,
    onSuccess: () => cliente.invalidateQueries({ queryKey: ['admin', 'administradores'] }),
  })
}

/** Solicitudes de reserva por aprobar: la lista corta del inicio y el contador del menú lateral. */
export function useSolicitudesPendientes() {
  return useQuery({
    queryKey: ['reservas', 'admin', 'pendientes-lista'],
    queryFn: async () =>
      (
        await api.get<Pagina<Reserva>>('/admin/reservas', {
          params: { estado: 'PENDIENTE', pagina: 0, tamano: 5 },
        })
      ).data,
    refetchInterval: 30_000,
  })
}

/** Reservas confirmadas de un día (para "Hoy" en el inicio). */
export function useReservasDelDia(fecha: string) {
  return useQuery({
    queryKey: ['reservas', 'admin', 'dia', fecha],
    queryFn: async () =>
      (
        await api.get<Pagina<Reserva>>('/admin/reservas', {
          params: { estado: 'CONFIRMADA', desde: fecha, hasta: fecha, pagina: 0, tamano: 50 },
        })
      ).data,
    refetchInterval: 60_000,
  })
}

/** Tablero del administrador, calculado en el servidor con la hora de Bogotá. */
export interface Tablero {
  generadoEn: string
  hoy: {
    pendientes: number
    confirmadas: number
    completadas: number
    noAsistio: number
    canceladas: number
    partidosTorneo: number
    valorReservado: number
    recaudado: number
    horasReservadas: number
  }
  pendientesPorVencer: number
  cobros: {
    reservasConSaldo: number
    saldoPendiente: number
    reembolsosPendientes: number
    montoReembolsos: number
    mayoresSaldos: {
      reservaId: number
      codigo: string
      cliente?: string | null
      inicio: string
      saldo: number
    }[]
  }
  proximosDias: { fecha: string; reservas: number; pendientes: number; horasConfirmadas: number }[]
  canchas: {
    canchaId: number
    cancha: string
    sede: string
    estado: 'ACTIVA' | 'MANTENIMIENTO' | 'INACTIVA'
    enUso: boolean
    ocupadaHasta?: string | null
    proximoInicio?: string | null
    reservasHoy: number
  }[]
  torneos: {
    enCurso: number
    conInscripcionesAbiertas: number
    solicitudesPendientes: number
    partidosSinProgramar: number
    partidosSinResultado: number
  }
  clientes: { activos: number; nuevosSemana: number; nuevosMes: number }
}

export function useTablero() {
  return useQuery({
    queryKey: ['reservas', 'admin', 'tablero'],
    queryFn: async () => (await api.get<Tablero>('/admin/estadisticas/tablero')).data,
    refetchInterval: 60_000,
  })
}
