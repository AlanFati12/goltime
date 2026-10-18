import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'

/** Respuesta de error de la API (ProblemDetail, RFC 7807) con el código de negocio estable. */
export interface ErrorApi {
  status: number
  title?: string
  detail?: string
  codigo?: string
  campos?: { campo: string; mensaje: string }[]
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

let renovacionEnCurso: Promise<string> | null = null

/** Pide un nuevo access token con la cookie de refresh. Una sola renovación a la vez. */
function renovarToken(): Promise<string> {
  renovacionEnCurso ??= axios
    .post<{ tokenAcceso: string }>('/api/v1/autenticacion/refrescar', null, {
      withCredentials: true,
    })
    .then((r) => {
      useSesion.getState().actualizarToken(r.data.tokenAcceso)
      return r.data.tokenAcceso
    })
    .finally(() => {
      renovacionEnCurso = null
    })
  return renovacionEnCurso
}

type ConfigReintentable = InternalAxiosRequestConfig & { _reintentada?: boolean }

api.interceptors.response.use(
  (r) => r,
  async (error: AxiosError<ErrorApi>) => {
    const config = error.config as ConfigReintentable | undefined
    const esRutaDeAuth = config?.url?.includes('/autenticacion/')
    if (error.response?.status === 401 && config && !config._reintentada && !esRutaDeAuth) {
      config._reintentada = true
      try {
        const token = await renovarToken()
        config.headers.Authorization = `Bearer ${token}`
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
    return error.response?.data?.detail ?? 'No pudimos conectar con el servidor. Intenta de nuevo.'
  }
  return 'Ocurrió un error inesperado.'
}
