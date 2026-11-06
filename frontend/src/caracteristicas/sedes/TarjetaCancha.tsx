import { ArrowUpRight, Lightbulb, Umbrella } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Insignia } from '@/componentes/ui/Insignia'
import { formatearCOP } from '@/lib/formato'

import type { Cancha } from './tipos'
import { ImagenSitio } from '@/componentes/ui/ImagenSitio'

/** Resumen de una cancha para listados públicos. */
export function TarjetaCancha({ cancha }: { cancha: Cancha }) {
  const foto = cancha.fotos[0]?.url
  return (
    <Link
      to={`/canchas/${cancha.id}`}
      className="group bg-superficie border-borde/80 hover:shadow-flotante flex h-full flex-col overflow-hidden rounded-[1.75rem] border transition-[border-color,box-shadow,transform] duration-300 hover:-translate-y-1 hover:border-transparent"
    >
      <div className="bg-noche relative aspect-[16/10] overflow-hidden">
        {foto ? (
          <ImagenSitio
            src={foto}
            alt=""
            sizes="(min-width: 1024px) 360px, (min-width: 640px) 50vw, 100vw"
            className="size-full object-cover transition-transform duration-[1200ms] ease-[var(--ease-salida)] group-hover:scale-105"
          />
        ) : (
          <ImagenSitio
            src="/img/cancha-aerea.webp"
            alt=""
            sizes="(min-width: 1024px) 360px, (min-width: 640px) 50vw, 100vw"
            className="size-full object-cover opacity-60"
          />
        )}
        <span className="bg-volt text-noche font-titulo absolute top-4 left-4 rounded-full px-3 py-1 text-lg leading-none font-extrabold">
          {cancha.modalidad}
        </span>
      </div>
      <div className="flex flex-1 flex-col gap-3 p-5">
        <div className="flex items-start justify-between gap-2">
          <h3 className="text-2xl leading-none font-extrabold uppercase">{cancha.nombre}</h3>
          <ArrowUpRight
            className="text-texto-suave group-hover:text-primario size-5 shrink-0 transition-[transform,color] duration-200 group-hover:translate-x-0.5 group-hover:-translate-y-0.5"
            aria-hidden
          />
        </div>
        <div className="text-texto-suave flex flex-wrap gap-x-4 gap-y-1 text-sm">
          <span>{cancha.jugadoresPorEquipo} por equipo</span>
          {cancha.iluminacion && (
            <span className="inline-flex items-center gap-1">
              <Lightbulb className="size-3.5" aria-hidden />
              Iluminada
            </span>
          )}
          {cancha.techada && (
            <span className="inline-flex items-center gap-1">
              <Umbrella className="size-3.5" aria-hidden />
              Techada
            </span>
          )}
        </div>
        {cancha.esCompuesta && (
          <Insignia tono="ambar" className="self-start">
            Se forma con {cancha.componentes.length} canchas
          </Insignia>
        )}
        <p className="text-texto-suave mt-auto pt-2 text-sm">
          Desde{' '}
          <span className="font-titulo cifras text-texto text-2xl font-extrabold">
            {formatearCOP(cancha.precioBaseHora)}
          </span>{' '}
          / hora
        </p>
      </div>
    </Link>
  )
}
