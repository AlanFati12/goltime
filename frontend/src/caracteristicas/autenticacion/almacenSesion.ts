import { create } from 'zustand'

export type Rol = 'ADMIN' | 'CLIENTE'

/** Usuario tal como lo devuelve la API (nunca incluye datos sensibles). */
export interface Usuario {
  id: number
  nombres: string
  apellidos: string
  correo: string
  telefono: string | null
  /** El teléfono de la cuenta se probó desde WhatsApp (ADR 0018). */
  telefonoVerificado: boolean
  /** El bot de WhatsApp reconoce la cuenta, por su teléfono o por el WhatsApp vinculado con el código del correo. */
  whatsappVinculado: boolean
  documento: string | null
  rol: Rol
  activo: boolean
  correoVerificado: boolean
  motivoDesactivacion: string | null
  fechaDesactivacion: string | null
  puedeInscribirEquipos: boolean
  ultimoLogin: string | null
  creadoEn: string
}

interface EstadoSesion {
  /** El access token vive solo en memoria (nunca en localStorage). */
  tokenAcceso: string | null
  usuario: Usuario | null
  /** false mientras se intenta recuperar la sesión con la cookie de refresh al abrir la app. */
  inicializada: boolean
  iniciar: (tokenAcceso: string, usuario: Usuario) => void
  actualizarToken: (tokenAcceso: string) => void
  actualizarUsuario: (usuario: Usuario) => void
  cerrar: () => void
  marcarInicializada: () => void
}

export const useSesion = create<EstadoSesion>((set) => ({
  tokenAcceso: null,
  usuario: null,
  inicializada: false,
  iniciar: (tokenAcceso, usuario) => set({ tokenAcceso, usuario, inicializada: true }),
  actualizarToken: (tokenAcceso) => set({ tokenAcceso }),
  actualizarUsuario: (usuario) => set({ usuario }),
  cerrar: () => set({ tokenAcceso: null, usuario: null }),
  marcarInicializada: () => set({ inicializada: true }),
}))

export function nombreCompleto(u: Pick<Usuario, 'nombres' | 'apellidos'>) {
  return `${u.nombres} ${u.apellidos}`.trim()
}
