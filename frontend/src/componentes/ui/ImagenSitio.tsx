import type { ImgHTMLAttributes } from 'react'

import { IMAGENES_SITIO } from '@/lib/imagenesSitio'

type Props = Omit<ImgHTMLAttributes<HTMLImageElement>, 'src' | 'srcSet' | 'width' | 'height'> & {
  /** Ruta de una foto del sitio (`/img/nombre.webp`) o la URL de una foto subida. */
  src: string
  /** Descripción de la foto; vacía si es decorativa. */
  alt: string
  /**
   * Ancho que ocupa la imagen según la pantalla (atributo `sizes`): el navegador elige la variante más
   * liviana que alcanza. Por defecto, todo el ancho.
   */
  sizes?: string
  /** La imagen más grande de la primera pantalla: se pide ya (sin carga diferida) y con prioridad alta. */
  prioridad?: boolean
}

/**
 * Foto con variantes responsivas. Las del sitio (generadas por scripts/optimizar-imagenes.py) llevan `srcSet`
 * con varios anchos y sus medidas reales, para que el navegador reserve el espacio y no baje más píxeles de los
 * que se ven. Las subidas por el administrador se cargan tal cual. Por defecto es de carga diferida: solo se
 * descarga al acercarse a la pantalla.
 */
export function ImagenSitio({
  src,
  alt,
  sizes = '100vw',
  prioridad = false,
  className,
  ...resto
}: Props) {
  const nombre = /^\/img\/([a-z0-9-]+)\.webp$/.exec(src)?.[1]
  const datos = nombre ? IMAGENES_SITIO[nombre] : undefined
  const comunes = {
    alt,
    className,
    loading: prioridad ? ('eager' as const) : ('lazy' as const),
    decoding: prioridad ? ('auto' as const) : ('async' as const),
    fetchPriority: prioridad ? ('high' as const) : undefined,
    ...resto,
  }
  if (!nombre || !datos) return <img src={src} {...comunes} />
  const variantes = datos.variantes
  return (
    <img
      src={`/img/${nombre}-${variantes[variantes.length - 1]}.webp`}
      srcSet={variantes.map((w) => `/img/${nombre}-${w}.webp ${w}w`).join(', ')}
      sizes={sizes}
      width={datos.ancho}
      height={datos.alto}
      {...comunes}
    />
  )
}
