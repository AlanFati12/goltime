import { ArrowLeft } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

import { cn } from '@/lib/utils'
import { ImagenSitio } from '@/componentes/ui/ImagenSitio'

/**
 * Cabecera fotográfica de las páginas públicas: la foto ocupa el ancho del contenedor, oscurecida
 * hacia abajo, y el título grande se apoya sobre ella. Da a cada sección su propia imagen.
 */
export function PortadaPagina({
  titulo,
  imagen,
  alt = '',
  children,
  acciones,
  volver,
  compacta,
}: {
  titulo: ReactNode
  imagen: string
  alt?: string
  /** Datos bajo el título: dirección, insignias, fechas… */
  children?: ReactNode
  /** Botones a la derecha (abajo en móvil). */
  acciones?: ReactNode
  volver?: { a: string; texto: string }
  compacta?: boolean
}) {
  return (
    <header
      className={cn(
        'bg-noche relative isolate flex overflow-hidden rounded-[2rem] text-white',
        compacta ? 'min-h-[17rem] sm:min-h-[19rem]' : 'min-h-[22rem] sm:min-h-[27rem]',
      )}
    >
      <ImagenSitio
        src={imagen}
        alt={alt}
        prioridad
        recorte="banner"
        className="animate-acercar absolute inset-0 -z-10 size-full object-cover motion-reduce:animate-none"
      />
      <div className="from-noche via-noche/60 absolute inset-0 -z-10 bg-gradient-to-t to-transparent" />
      <div className="from-noche/70 absolute inset-0 -z-10 bg-gradient-to-r to-transparent" />

      {volver && (
        <Link
          to={volver.a}
          className="bg-noche/50 group absolute top-5 left-5 inline-flex items-center gap-1.5 rounded-full px-4 py-2 text-sm font-semibold text-white/85 backdrop-blur-md transition-colors hover:text-white sm:top-7 sm:left-7"
        >
          <ArrowLeft
            className="size-4 transition-transform duration-200 group-hover:-translate-x-0.5"
            aria-hidden
          />
          {volver.texto}
        </Link>
      )}

      <div className="mt-auto flex w-full flex-col gap-6 p-6 sm:p-10 lg:flex-row lg:items-end lg:justify-between">
        <div className="animate-subir min-w-0 [animation-delay:120ms]">
          <h1 className="font-titulo text-[clamp(2.6rem,7vw,5.75rem)] leading-[0.9] font-extrabold tracking-[-0.02em] uppercase">
            {titulo}
          </h1>
          {children && <div className="mt-4 flex flex-col gap-3 text-white/80">{children}</div>}
        </div>
        {acciones && (
          <div className="animate-subir flex shrink-0 flex-wrap gap-3 [animation-delay:240ms]">
            {acciones}
          </div>
        )}
      </div>
    </header>
  )
}

/** Insignia translúcida para usar sobre fotos oscuras. */
export function InsigniaFoto({ children }: { children: ReactNode }) {
  return (
    <span className="rounded-full bg-white/12 px-3 py-1 text-sm font-semibold text-white backdrop-blur-sm">
      {children}
    </span>
  )
}
