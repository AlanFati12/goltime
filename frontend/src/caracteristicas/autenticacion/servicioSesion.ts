import axios from 'axios'

import { useSesion, type Usuario } from './almacenSesion'

interface RespuestaSesion {
  tokenAcceso: string
  tipo: string
  expiraEnSegundos: number
  usuario: Usuario
}

let renovacion: Promise<RespuestaSesion> | null = null

/**
 * Pide un nuevo access token usando la cookie HttpOnly de refresh (que rota en cada uso).
 * Es de una sola ejecución a la vez: dos llamadas simultáneas comparten la misma petición, porque
 * enviar dos veces el mismo refresh token se interpretaría como robo y cerraría la sesión.
 */
export function renovarSesion(): Promise<RespuestaSesion> {
  renovacion ??= axios
    .post<RespuestaSesion>('/api/v1/autenticacion/refrescar', null, { withCredentials: true })
    .then((r) => {
      useSesion.getState().iniciar(r.data.tokenAcceso, r.data.usuario)
      return r.data
    })
    .finally(() => {
      renovacion = null
    })
  return renovacion
}

/** Al abrir la app intenta recuperar la sesión; si no hay cookie válida, queda como visitante. */
export async function restaurarSesion(): Promise<void> {
  try {
    await renovarSesion()
  } catch {
    useSesion.getState().cerrar()
  } finally {
    useSesion.getState().marcarInicializada()
  }
}
