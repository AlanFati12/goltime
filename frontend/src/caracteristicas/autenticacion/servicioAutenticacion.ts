import { api } from '@/lib/api'
import type { DatosRegistro, DatosIngreso } from '@/lib/validaciones'

import { useSesion, type Usuario } from './almacenSesion'

interface RespuestaSesion {
  tokenAcceso: string
  usuario: Usuario
}

export async function ingresar(datos: DatosIngreso): Promise<Usuario> {
  const { data } = await api.post<RespuestaSesion>('/autenticacion/ingresar', datos)
  useSesion.getState().iniciar(data.tokenAcceso, data.usuario)
  return data.usuario
}

export async function registrar(datos: DatosRegistro): Promise<void> {
  await api.post('/autenticacion/registrar', {
    ...datos,
    documento: datos.documento || undefined,
  })
}

export async function salir(): Promise<void> {
  try {
    await api.post('/autenticacion/salir')
  } finally {
    useSesion.getState().cerrar()
  }
}

export const solicitarRecuperacion = (correo: string) =>
  api.post('/autenticacion/olvide-clave', { correo })

export const restablecerClave = (token: string, claveNueva: string, confirmacion: string) =>
  api.post('/autenticacion/restablecer-clave', { token, claveNueva, confirmacion })

export const verificarCorreo = (token: string) =>
  api.post('/autenticacion/verificar-correo', { token })

/** Ruta a la que enviar al usuario después de iniciar sesión según su rol. */
export function rutaInicial(usuario: Usuario): string {
  return usuario.rol === 'ADMIN' ? '/admin' : '/'
}
