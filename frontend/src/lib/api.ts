import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { renovarSesion } from '@/caracteristicas/autenticacion/servicioSesion'

/** Respuesta de error de la API (ProblemDetail, RFC 7807) con el código de negocio estable. */
export interface ErrorApi {
  status: number
  title?: string
  detail?: string
  codigo?: string
  campos?: { campo: string; mensaje: string }[]
}

/** Página de resultados de la API ({@code pagina} empieza en 0). */
export interface Pagina<T> {
  contenido: T[]
  pagina: number
  tamano: number
  totalElementos: number
  totalPaginas: number
}

export const api = axios.create({
  baseURL: '/api/v1',
  withCredentials: true, // el refresh token viaja en una cookie HttpOnly
})

api.interceptors.request.use((config) => {
  const token = useSesion.getState().tokenAcceso
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

type ConfigReintentable = InternalAxiosRequestConfig & { _reintentada?: boolean }

api.interceptors.response.use(
  (r) => r,
  async (error: AxiosError<ErrorApi>) => {
    const config = error.config as ConfigReintentable | undefined
    const esRutaDeAuth = config?.url?.includes('/autenticacion/')
    const teniaSesion = useSesion.getState().tokenAcceso !== null
    if (
      error.response?.status === 401 &&
      config &&
      !config._reintentada &&
      !esRutaDeAuth &&
      teniaSesion
    ) {
      config._reintentada = true
      try {
        const sesion = await renovarSesion()
        config.headers.Authorization = `Bearer ${sesion.tokenAcceso}`
        return api(config)
      } catch {
        useSesion.getState().cerrar()
      }
    }
    return Promise.reject(error)
  },
)

/** Mensaje amigable a partir de cualquier error de la API. */
export function mensajeDeError(error: unknown): string {
  if (axios.isAxiosError<ErrorApi>(error)) {
    if (!error.response) return 'No pudimos conectar con el servidor. Revisa tu conexión.'
    return error.response.data?.detail ?? 'Ocurrió un error inesperado. Intenta de nuevo.'
  }
  return 'Ocurrió un error inesperado.'
}

export function codigoDeError(error: unknown): string | undefined {
  return axios.isAxiosError<ErrorApi>(error) ? error.response?.data?.codigo : undefined
}
