import { MarcaGoltime } from '@/componentes/layout/Logo'
import { cn } from '@/lib/utils'

/** Forma del contenido que está por llegar: así el esqueleto se parece a lo que va a aparecer. */
export type VarianteCarga = 'giro' | 'pagina' | 'tabla' | 'tarjetas' | 'lista'

/**
 * Indicador de carga. Por defecto muestra un esqueleto de página (títulos y bloques grises) en vez de un
 * círculo girando: la pantalla se siente lista antes y no salta cuando llegan los datos. Para cargas muy
 * pequeñas (dentro de un modal, un botón o la carga inicial de toda la app) queda el círculo con la marca.
 *
 * Sin `variante`, se elige solo: si `className` pide un alto o relleno propio (`min-h-…`, `py-…`) es una carga
 * pequeña o de pantalla completa y se muestra el círculo; en cualquier otro caso, el esqueleto de página.
 */
export function Cargando({
  texto = 'Cargando…',
  className,
  variante,
}: {
  texto?: string
  className?: string
  variante?: VarianteCarga
}) {
  const forma = variante ?? (/(^|\s)(min-h-|py-)/.test(className ?? '') ? 'giro' : 'pagina')
  if (forma === 'giro') {
    return (
      <div
        role="status"
        className={cn(
          'text-texto-suave flex flex-col items-center justify-center gap-3 py-14 text-sm font-medium',
          className,
        )}
      >
        <MarcaGoltime animado className="size-10" />
        {texto}
      </div>
    )
  }
  return (
    <div role="status" aria-busy="true" className={cn('flex flex-col gap-4', className)}>
      <span className="sr-only">{texto}</span>
      {forma === 'pagina' && <EsqueletoPagina />}
      {forma === 'tabla' && <EsqueletoTabla />}
      {forma === 'tarjetas' && <EsqueletoTarjetas />}
      {forma === 'lista' && <EsqueletoLista />}
    </div>
  )
}

/** Bloque gris animado que ocupa el lugar del contenido mientras llega. */
export function Esqueleto({ className }: { className?: string }) {
  return (
    <div aria-hidden className={cn('bg-tenue relative overflow-hidden rounded-xl', className)}>
      <div className="animate-barrido oscuro:via-white/5 absolute inset-y-0 w-1/2 bg-gradient-to-r from-transparent via-white/70 to-transparent motion-reduce:hidden" />
    </div>
  )
}

/** Título, una línea de apoyo y tres bloques: sirve para cualquier pantalla de detalle. */
export function EsqueletoPagina() {
  return (
    <>
      <Esqueleto className="h-9 w-2/3 max-w-sm" />
      <Esqueleto className="h-4 w-full max-w-md" />
      <div className="mt-2 grid gap-4 sm:grid-cols-3">
        <Esqueleto className="h-28" />
        <Esqueleto className="h-28" />
        <Esqueleto className="h-28" />
      </div>
      <Esqueleto className="h-48" />
    </>
  )
}

/** Encabezado de tabla y filas: para listados administrativos. */
export function EsqueletoTabla({ filas = 6 }: { filas?: number }) {
  return (
    <div className="border-borde/70 bg-superficie overflow-hidden rounded-[1.5rem] border">
      <Esqueleto className="h-11 rounded-none" />
      {Array.from({ length: filas }, (_, i) => (
        <div key={i} className="border-borde/60 flex items-center gap-4 border-t p-4">
          <Esqueleto className="h-4 w-1/5" />
          <Esqueleto className="h-4 w-1/4" />
          <Esqueleto className="h-4 flex-1" />
          <Esqueleto className="h-7 w-20 rounded-full" />
        </div>
      ))}
    </div>
  )
}

/** Cuadrícula de tarjetas con foto: sedes, canchas, torneos. */
export function EsqueletoTarjetas({ cantidad = 3 }: { cantidad?: number }) {
  return (
    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: cantidad }, (_, i) => (
        <div
          key={i}
          className="border-borde/70 bg-superficie overflow-hidden rounded-[1.75rem] border"
        >
          <Esqueleto className="aspect-[16/10] rounded-none" />
          <div className="flex flex-col gap-3 p-5">
            <Esqueleto className="h-6 w-3/4" />
            <Esqueleto className="h-4 w-1/2" />
          </div>
        </div>
      ))}
    </div>
  )
}

/** Tarjetas apiladas con un dato destacado: reservas, notificaciones, favoritas. */
export function EsqueletoLista({ filas = 4 }: { filas?: number }) {
  return (
    <div className="flex flex-col gap-3">
      {Array.from({ length: filas }, (_, i) => (
        <div
          key={i}
          className="border-borde/70 bg-superficie flex items-center gap-4 rounded-[1.5rem] border p-5"
        >
          <Esqueleto className="size-12 shrink-0 rounded-2xl" />
          <div className="flex flex-1 flex-col gap-2">
            <Esqueleto className="h-5 w-1/2" />
            <Esqueleto className="h-4 w-3/4" />
          </div>
          <Esqueleto className="h-7 w-20 rounded-full" />
        </div>
      ))}
    </div>
  )
}
