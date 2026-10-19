import { useSyncExternalStore } from 'react'

/** Tema visual del sitio. Se guarda en el navegador de cada persona; no viaja al servidor. */
export type Tema = 'claro' | 'oscuro'

export const CLAVE_TEMA = 'goltime:tema'
const COLOR_BARRA: Record<Tema, string> = { claro: '#0b1220', oscuro: '#080d18' }
const EVENTO = 'goltime:tema'

/** Tema que hay en el documento (index.html lo fija antes de pintar, sin parpadeo). */
export function temaActual(): Tema {
  return document.documentElement.dataset.tema === 'oscuro' ? 'oscuro' : 'claro'
}

/** Aplica el tema al documento, lo recuerda y avisa a los componentes suscritos. */
export function aplicarTema(tema: Tema) {
  const raiz = document.documentElement
  raiz.dataset.tema = tema
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', COLOR_BARRA[tema])
  try {
    localStorage.setItem(CLAVE_TEMA, tema)
  } catch {
    // Navegación privada sin almacenamiento: el tema dura hasta recargar.
  }
  window.dispatchEvent(new Event(EVENTO))
}

function suscribir(aviso: () => void) {
  window.addEventListener(EVENTO, aviso)
  return () => window.removeEventListener(EVENTO, aviso)
}

/** Tema actual, reactivo a los cambios hechos desde cualquier botón. */
export function useTema(): Tema {
  return useSyncExternalStore(suscribir, temaActual, () => 'claro')
}
