import { api } from '@/lib/api'

import { useSesion, type Usuario } from './almacenSesion'

interface RespuestaSesion {
  tokenAcceso: string
  usuario: Usuario
}

/** Lo que el servidor dice sobre cómo ingresar: el ID de cliente de Google y si está el ingreso de prueba (solo desarrollo). */
export interface ConfiguracionIngreso {
  googleClientId: string
  modoPrueba: boolean
}

export const cargarConfiguracionIngreso = async () =>
  (await api.get<ConfiguracionIngreso>('/autenticacion/configuracion')).data

/**
 * Entra con el token de identidad que entrega el botón de Google. El servidor lo verifica y crea la cuenta si es la
 * primera vez. No hay contraseñas ni registro con formulario.
 */
export async function ingresarConGoogle(credencial: string, recordarme: boolean): Promise<Usuario> {
  const { data } = await api.post<RespuestaSesion>('/autenticacion/google', {
    credencial,
    recordarme,
  })
  useSesion.getState().iniciar(data.tokenAcceso, data.usuario)
  return data.usuario
}

export async function salir(): Promise<void> {
  try {
    await api.post('/autenticacion/salir')
  } finally {
    useSesion.getState().cerrar()
  }
}

/** Datos que Google no entrega y Goltime necesita: apellidos y WhatsApp (obligatorios) y documento (opcional). */
export interface DatosCompletarPerfil {
  nombres: string
  apellidos: string
  telefono: string
  documento?: string
}

export async function completarPerfil(datos: DatosCompletarPerfil): Promise<Usuario> {
  const { data } = await api.post<Usuario>('/me/completar-perfil', {
    ...datos,
    documento: datos.documento || undefined,
  })
  useSesion.getState().actualizarUsuario(data)
  return data
}

/** A dónde mandar a alguien recién ingresado: el panel, o completar el perfil si le faltan datos, o de donde venía. */
export function destinoTrasIngresar(usuario: Usuario, desde?: string): string {
  if (usuario.rol === 'ADMIN') return '/admin'
  if (!usuario.perfilCompleto) return '/completar-perfil'
  return desde ?? '/'
}

/** Ruta a la que enviar al usuario que ya tiene sesión y abre una pantalla de acceso. */
export function rutaInicial(usuario: Usuario): string {
  return destinoTrasIngresar(usuario)
}
