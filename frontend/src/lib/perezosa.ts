import { lazy, type ComponentType } from 'react'

/**
 * Carga perezosa de una página exportada con nombre: el código de la ruta se descarga solo
 * cuando alguien la visita, así la portada no paga por el panel de administración.
 */
export function perezosa<K extends string, M extends Record<K, ComponentType>>(
  cargar: () => Promise<M>,
  nombre: K,
) {
  return lazy(async () => ({ default: (await cargar())[nombre] }))
}
