import { create } from 'zustand'

export type Rol = 'ADMIN' | 'CLIENTE'

export interface UsuarioSesion {
  id: number
  nombres: string
  apellidos: string
  rol: Rol
}

interface EstadoSesion {
  /** El access token vive solo en memoria (nunca en localStorage). */
  tokenAcceso: string | null
  usuario: UsuarioSesion | null
  iniciar: (tokenAcceso: string, usuario: UsuarioSesion) => void
  actualizarToken: (tokenAcceso: string) => void
  cerrar: () => void
}

export const useSesion = create<EstadoSesion>((set) => ({
  tokenAcceso: null,
  usuario: null,
  iniciar: (tokenAcceso, usuario) => set({ tokenAcceso, usuario }),
  actualizarToken: (tokenAcceso) => set({ tokenAcceso }),
  cerrar: () => set({ tokenAcceso: null, usuario: null }),
}))
