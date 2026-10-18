import { create } from 'zustand'

interface Aviso {
  id: number
  tipo: 'exito' | 'error'
  texto: string
}

interface EstadoAvisos {
  avisos: Aviso[]
  mostrar: (tipo: Aviso['tipo'], texto: string) => void
  quitar: (id: number) => void
}

let contador = 0

export const useAvisos = create<EstadoAvisos>((set, get) => ({
  avisos: [],
  mostrar: (tipo, texto) => {
    const id = ++contador
    set((s) => ({ avisos: [...s.avisos, { id, tipo, texto }] }))
    setTimeout(() => get().quitar(id), 5000)
  },
  quitar: (id) => set((s) => ({ avisos: s.avisos.filter((a) => a.id !== id) })),
}))

/** Atajos para mostrar avisos (toasts) desde cualquier parte. */
export const avisar = {
  exito: (texto: string) => useAvisos.getState().mostrar('exito', texto),
  error: (texto: string) => useAvisos.getState().mostrar('error', texto),
}
