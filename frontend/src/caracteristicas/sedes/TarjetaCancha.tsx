import { Link } from 'react-router-dom'

import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { formatearCOP } from '@/lib/formato'

import { Galeria } from './Galeria'
import type { Cancha } from './tipos'

/** Resumen de una cancha para listados públicos. */
export function TarjetaCancha({ cancha }: { cancha: Cancha }) {
  return (
    <Link to={`/canchas/${cancha.id}`} className="group block">
      <Tarjeta className="flex h-full flex-col overflow-hidden p-0 transition-all group-hover:-translate-y-0.5 group-hover:shadow-md sm:p-0">
        <Galeria fotos={cancha.fotos.slice(0, 1)} alto="aspect-[16/9]" className="rounded-none" />
        <div className="flex flex-1 flex-col gap-3 p-4">
          <div className="flex items-start justify-between gap-2">
            <h3 className="font-semibold">{cancha.nombre}</h3>
            <Insignia tono="verde">{cancha.modalidad}</Insignia>
          </div>
          <div className="flex flex-wrap gap-1.5">
            {cancha.techada && <Insignia>Techada</Insignia>}
            {cancha.iluminacion && <Insignia>Iluminación</Insignia>}
            {cancha.esCompuesta && (
              <Insignia tono="ambar">Se forma con {cancha.componentes.length} canchas</Insignia>
            )}
          </div>
          <p className="border-borde text-texto-suave mt-auto border-t pt-3 text-sm">
            Desde{' '}
            <strong className="text-texto font-semibold">
              {formatearCOP(cancha.precioBaseHora)}
            </strong>{' '}
            / hora
          </p>
        </div>
      </Tarjeta>
    </Link>
  )
}
