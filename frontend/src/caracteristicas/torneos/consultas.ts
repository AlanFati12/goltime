import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { api } from '@/lib/api'
import { useValorConRetraso } from '@/lib/useValorConRetraso'

import type {
  DetalleEquipo,
  DetalleSolicitud,
  DetallePartido,
  DetalleTorneo,
  Estadisticas,
  EstadoPartido,
  EstadoTorneo,
  Inscripcion,
  JugadorEquipo,
  Partido,
  PerfilEquipo,
  PerfilJugador,
  PrevisualizacionPartido,
  ResumenEquipo,
  ResumenFixture,
  Tabla,
  Torneo,
  TipoEvento,
} from './tipos'

const base = (admin: boolean) => (admin ? '/admin' : '/publico')

// ===================== Lectura (pública o de administración) =====================

export const useTorneos = (admin: boolean, estado?: EstadoTorneo) =>
  useQuery({
    queryKey: ['torneos', 'lista', admin, estado ?? null],
    queryFn: async () =>
      (await api.get<Torneo[]>(`${base(admin)}/torneos`, { params: { estado } })).data,
  })

export const useTorneo = (id: number, admin: boolean) =>
  useQuery({
    queryKey: ['torneos', 'detalle', admin, id],
    enabled: id > 0,
    queryFn: async () => (await api.get<DetalleTorneo>(`${base(admin)}/torneos/${id}`)).data,
  })

export const usePosiciones = (id: number, admin: boolean) =>
  useQuery({
    queryKey: ['torneos', 'posiciones', admin, id],
    queryFn: async () => (await api.get<Tabla[]>(`${base(admin)}/torneos/${id}/posiciones`)).data,
  })

export const usePartidos = (id: number, admin: boolean, estado?: EstadoPartido) =>
  useQuery({
    queryKey: ['torneos', 'partidos', admin, id, estado ?? null],
    queryFn: async () =>
      (await api.get<Partido[]>(`${base(admin)}/torneos/${id}/partidos`, { params: { estado } }))
        .data,
  })

export const useEstadisticas = (id: number, admin: boolean) =>
  useQuery({
    queryKey: ['torneos', 'estadisticas', admin, id],
    queryFn: async () =>
      (await api.get<Estadisticas>(`${base(admin)}/torneos/${id}/estadisticas`)).data,
  })

export const useDetallePartido = (id: number, admin: boolean) =>
  useQuery({
    queryKey: ['torneos', 'partido', admin, id],
    queryFn: async () => (await api.get<DetallePartido>(`${base(admin)}/partidos/${id}`)).data,
  })

export const useProximosPartidos = (limite = 6) =>
  useQuery({
    queryKey: ['torneos', 'proximos', limite],
    queryFn: async () =>
      (await api.get<Partido[]>('/publico/partidos/proximos', { params: { limite } })).data,
  })

export const usePerfilEquipo = (id: number) =>
  useQuery({
    queryKey: ['torneos', 'equipo-publico', id],
    queryFn: async () => (await api.get<PerfilEquipo>(`/publico/equipos/${id}`)).data,
  })

export const usePerfilJugador = (id: number) =>
  useQuery({
    queryKey: ['torneos', 'jugador-publico', id],
    queryFn: async () => (await api.get<PerfilJugador>(`/publico/jugadores/${id}`)).data,
  })

// ===================== Administración: equipos =====================

export function useEquipos(entrada: string, incluirInactivos: boolean) {
  // La búsqueda sale al servidor cuando la persona deja de escribir, no por cada tecla.
  const busqueda = useValorConRetraso(entrada)
  return useQuery({
    queryKey: ['torneos', 'equipos', busqueda, incluirInactivos],
    queryFn: async () =>
      (
        await api.get<ResumenEquipo[]>('/admin/equipos', {
          params: { busqueda: busqueda || undefined, incluirInactivos },
        })
      ).data,
    placeholderData: keepPreviousData,
  })
}

export const useEquipo = (id: number) =>
  useQuery({
    queryKey: ['torneos', 'equipo', id],
    queryFn: async () => (await api.get<DetalleEquipo>(`/admin/equipos/${id}`)).data,
  })

export function useBuscarJugadores(entrada: string) {
  const texto = useValorConRetraso(entrada)
  return useQuery({
    queryKey: ['torneos', 'buscar-jugadores', texto],
    enabled: texto.trim().length >= 3,
    queryFn: async () =>
      (await api.get<JugadorEquipo[]>('/admin/jugadores', { params: { texto } })).data,
  })
}

export interface DatosEquipo {
  nombre: string
  color?: string
  delegadoNombre?: string
  delegadoTelefono?: string
  usuarioResponsableId?: number | null
}

export interface DatosJugador {
  jugadorId?: number
  nombres?: string
  apellidos?: string
  documento?: string
  posicion?: string
  dorsal?: number | null
}

function useRefresco() {
  const cliente = useQueryClient()
  return () => {
    void cliente.invalidateQueries({ queryKey: ['torneos'] })
    void cliente.invalidateQueries({ queryKey: ['agenda'] })
    void cliente.invalidateQueries({ queryKey: ['notificaciones'] })
  }
}

export function useAccionesEquipo() {
  const refrescar = useRefresco()
  return {
    crear: useMutation({
      mutationFn: async (d: DatosEquipo) =>
        (await api.post<DetalleEquipo>('/admin/equipos', d)).data,
      onSuccess: refrescar,
    }),
    actualizar: useMutation({
      mutationFn: async (p: { id: number; datos: DatosEquipo }) =>
        (await api.put<DetalleEquipo>(`/admin/equipos/${p.id}`, p.datos)).data,
      onSuccess: refrescar,
    }),
    cambiarActivo: useMutation({
      mutationFn: async (p: { id: number; activo: boolean }) =>
        (await api.post<DetalleEquipo>(`/admin/equipos/${p.id}/estado`, { activo: p.activo })).data,
      onSuccess: refrescar,
    }),
    agregarJugador: useMutation({
      mutationFn: async (p: { id: number; datos: DatosJugador }) =>
        (await api.post<DetalleEquipo>(`/admin/equipos/${p.id}/jugadores`, p.datos)).data,
      onSuccess: refrescar,
    }),
    quitarJugador: useMutation({
      mutationFn: async (p: { id: number; jugadorId: number }) =>
        (await api.delete<DetalleEquipo>(`/admin/equipos/${p.id}/jugadores/${p.jugadorId}`)).data,
      onSuccess: refrescar,
    }),
    /** Corrige los datos del jugador y su dorsal en la plantilla habitual del equipo. */
    editarJugador: useMutation({
      mutationFn: async (p: { id: number; jugadorId: number; datos: DatosEdicionJugador }) => {
        await api.put(`/admin/jugadores/${p.jugadorId}`, datosPersona(p.datos))
        return (
          await api.put<DetalleEquipo>(`/admin/equipos/${p.id}/jugadores/${p.jugadorId}/dorsal`, {
            dorsal: p.datos.dorsal,
          })
        ).data
      },
      onSuccess: refrescar,
    }),
  }
}

/** Datos editables de un jugador: su persona (vale para todos sus equipos) y el dorsal en este equipo. */
export interface DatosEdicionJugador {
  nombres: string
  apellidos: string
  documento: string
  posicion: string
  dorsal: number | null
}

const datosPersona = (d: DatosEdicionJugador) => ({
  nombres: d.nombres.trim(),
  apellidos: d.apellidos.trim(),
  documento: d.documento.trim() || undefined,
  posicion: d.posicion.trim() || undefined,
})

// ===================== Administración: torneos =====================

export interface DatosTorneo {
  nombre: string
  descripcion?: string
  sedeId: number
  jugadoresPorEquipo: number
  formato: string
  fechaInicio?: string
  fechaFin?: string
  cupoEquipos?: number
  valorInscripcion?: number
  premiosTexto?: string
  reglamento?: string
  modoEmparejamiento?: string
  idaYVuelta?: boolean
  tercerPuesto?: boolean
  numGrupos?: number
  clasificadosPorGrupo?: number
  puntosVictoria?: number
  puntosEmpate?: number
  puntosDerrota?: number
  criteriosDesempate?: string[]
  marcadorWoGoles?: number
  canchaIds?: number[]
}

export function useAccionesTorneo() {
  const refrescar = useRefresco()
  return {
    crear: useMutation({
      mutationFn: async (d: DatosTorneo) =>
        (await api.post<DetalleTorneo>('/admin/torneos', d)).data,
      onSuccess: refrescar,
    }),
    actualizar: useMutation({
      mutationFn: async (p: { id: number; datos: DatosTorneo }) =>
        (await api.put<DetalleTorneo>(`/admin/torneos/${p.id}`, p.datos)).data,
      onSuccess: refrescar,
    }),
    cambiarEstado: useMutation({
      mutationFn: async (p: { id: number; estado: EstadoTorneo; motivo?: string }) =>
        (
          await api.post<DetalleTorneo>(`/admin/torneos/${p.id}/estado`, {
            estado: p.estado,
            motivo: p.motivo,
          })
        ).data,
      onSuccess: refrescar,
    }),
    inscribir: useMutation({
      mutationFn: async (p: { id: number; equipoId: number; semilla?: number }) =>
        (
          await api.post<Inscripcion>(`/admin/torneos/${p.id}/inscripciones`, {
            equipoId: p.equipoId,
            semilla: p.semilla,
          })
        ).data,
      onSuccess: refrescar,
    }),
    retirar: useMutation({
      mutationFn: async (p: { id: number; inscripcionId: number }) => {
        await api.delete(`/admin/torneos/${p.id}/inscripciones/${p.inscripcionId}`)
      },
      onSuccess: refrescar,
    }),
    generarFixture: useMutation({
      mutationFn: async (p: { id: number; ordenManual?: number[] }) =>
        (
          await api.post<ResumenFixture>(`/admin/torneos/${p.id}/fixture`, {
            ordenManual: p.ordenManual,
          })
        ).data,
      onSuccess: refrescar,
    }),
    descartarFixture: useMutation({
      mutationFn: async (id: number) => {
        await api.delete(`/admin/torneos/${id}/fixture`)
      },
      onSuccess: refrescar,
    }),
  }
}

export const usePlantillaTorneo = (torneoId: number, inscripcionId: number | null) =>
  useQuery({
    queryKey: ['torneos', 'plantilla', torneoId, inscripcionId],
    enabled: inscripcionId !== null,
    queryFn: async () =>
      (
        await api.get<JugadorEquipo[]>(
          `/admin/torneos/${torneoId}/inscripciones/${inscripcionId}/jugadores`,
        )
      ).data,
  })

export function useAccionesPlantilla(torneoId: number, inscripcionId: number) {
  const refrescar = useRefresco()
  const ruta = `/admin/torneos/${torneoId}/inscripciones/${inscripcionId}/jugadores`
  return {
    agregar: useMutation({
      mutationFn: async (d: DatosJugador) => (await api.post<JugadorEquipo[]>(ruta, d)).data,
      onSuccess: refrescar,
    }),
    quitar: useMutation({
      mutationFn: async (jugadorId: number) =>
        (await api.delete<JugadorEquipo[]>(`${ruta}/${jugadorId}`)).data,
      onSuccess: refrescar,
    }),
    editar: useMutation({
      mutationFn: async (p: { jugadorId: number; datos: DatosEdicionJugador }) => {
        await api.put(`/admin/jugadores/${p.jugadorId}`, datosPersona(p.datos))
        return (
          await api.put<JugadorEquipo[]>(`${ruta}/${p.jugadorId}/dorsal`, {
            dorsal: p.datos.dorsal,
          })
        ).data
      },
      onSuccess: refrescar,
    }),
  }
}

export const useDetalleSolicitud = (torneoId: number, inscripcionId: number | null) =>
  useQuery({
    queryKey: ['torneos', 'solicitud', torneoId, inscripcionId],
    enabled: inscripcionId !== null,
    queryFn: async () =>
      (await api.get<DetalleSolicitud>(`/admin/torneos/${torneoId}/solicitudes/${inscripcionId}`))
        .data,
  })

// ===================== Administración: partidos =====================

export interface DatosProgramacion {
  canchaId: number
  inicio: string
  duracionMin: number
  arbitro?: string
  huellaConfirmada?: string
}

export interface DatosResultado {
  golesLocal: number
  golesVisitante: number
  penalesLocal?: number | null
  penalesVisitante?: number | null
  figuraJugadorId?: number | null
}

export interface DatosEvento {
  inscripcionId: number
  jugadorId: number
  tipo: TipoEvento
  minuto?: number | null
  eventoRelacionadoId?: number | null
  observacion?: string
}

export function useAccionesPartido(partidoId: number) {
  const refrescar = useRefresco()
  const ruta = `/admin/partidos/${partidoId}`
  return {
    previsualizar: useMutation({
      mutationFn: async (d: DatosProgramacion) =>
        (await api.post<PrevisualizacionPartido>(`${ruta}/programacion/previsualizar`, d)).data,
    }),
    programar: useMutation({
      mutationFn: async (d: DatosProgramacion) =>
        (await api.put<DetallePartido>(`${ruta}/programacion`, d)).data,
      onSuccess: refrescar,
    }),
    desprogramar: useMutation({
      mutationFn: async (aplazar: boolean) =>
        (await api.delete<DetallePartido>(`${ruta}/programacion`, { params: { aplazar } })).data,
      onSuccess: refrescar,
    }),
    resultado: useMutation({
      mutationFn: async (d: DatosResultado) =>
        (await api.put<DetallePartido>(`${ruta}/resultado`, d)).data,
      onSuccess: refrescar,
    }),
    wo: useMutation({
      mutationFn: async (p: { ganador: 'LOCAL' | 'VISITANTE'; motivo?: string }) =>
        (await api.post<DetallePartido>(`${ruta}/wo`, p)).data,
      onSuccess: refrescar,
    }),
    iniciar: useMutation({
      mutationFn: async () => (await api.post<DetallePartido>(`${ruta}/iniciar`)).data,
      onSuccess: refrescar,
    }),
    reabrir: useMutation({
      mutationFn: async () => (await api.post<DetallePartido>(`${ruta}/reabrir`)).data,
      onSuccess: refrescar,
    }),
    agregarEvento: useMutation({
      mutationFn: async (d: DatosEvento) =>
        (await api.post<DetallePartido>(`${ruta}/eventos`, d)).data,
      onSuccess: refrescar,
    }),
    eliminarEvento: useMutation({
      mutationFn: async (eventoId: number) =>
        (await api.delete<DetallePartido>(`${ruta}/eventos/${eventoId}`)).data,
      onSuccess: refrescar,
    }),
  }
}
