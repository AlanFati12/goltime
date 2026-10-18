import { Link } from 'react-router-dom'

import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { formatearCOP } from '@/lib/formato'

import { Galeria } from './Galeria'
import type { Cancha } from './tipos'

/** Resumen de una cancha para listados públicos. */
export function TarjetaCancha({ cancha }: { cancha: Cancha }) {
  return (
    <Link to={`/canchas/${cancha.id}`} className="block">
      <Tarjeta className="hover:border-primario flex h-full flex-col gap-3 p-4 transition-colors">
        <Galeria fotos={cancha.fotos.slice(0, 1)} alto="aspect-[16/9]" />
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-semibold">{cancha.nombre}</h3>
          <Insignia tono="verde">{cancha.modalidad}</Insignia>
        </div>
        <div className="text-texto-suave flex flex-wrap gap-1 text-xs">
          {cancha.techada && <Insignia>Techada</Insignia>}
          {cancha.iluminacion && <Insignia>Iluminación</Insignia>}
          {cancha.esCompuesta && (
            <Insignia tono="ambar">Se forma con {cancha.componentes.length} canchas</Insignia>
          )}
        </div>
        <p className="mt-auto text-sm">
          Desde <strong>{formatearCOP(cancha.precioBaseHora)}</strong> / hora
        </p>
      </Tarjeta>
    </Link>
  )
}
