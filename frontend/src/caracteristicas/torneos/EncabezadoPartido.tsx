import { Link } from 'react-router-dom'

import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { formatearFechaHora } from '@/lib/formato'

import { ETIQUETA_EVENTO, ETIQUETA_ESTADO_PARTIDO, ICONO_EVENTO } from './etiquetas'
import type { DetallePartido } from './tipos'
import { marcador, nombreLado, textoPenales, textoRonda } from './utilidades'

/** Marcador grande del partido con su información y la línea de tiempo de eventos. */
export function EncabezadoPartido({
  detalle,
  admin = false,
}: {
  detalle: DetallePartido
  admin?: boolean
}) {
  const p = detalle.partido
  const torneoRuta = admin ? `/admin/torneos/${p.torneoId}` : `/torneos/${p.torneoId}`
  return (
    <div className="flex flex-col gap-4">
      <Tarjeta className="flex flex-col items-center gap-3 text-center">
        <Link to={torneoRuta} className="text-primario text-sm hover:underline">
          {p.torneo} · {textoRonda(p)}
        </Link>
        <div className="grid w-full max-w-xl grid-cols-[1fr_auto_1fr] items-center gap-4">
          <span className="text-lg font-semibold">{nombreLado(p.local)}</span>
          <span className="bg-primario-claro rounded-boton px-4 py-2 text-3xl font-bold">
            {marcador(p)}
          </span>
          <span className="text-lg font-semibold">{nombreLado(p.visitante)}</span>
        </div>
        {textoPenales(p) && (
          <p className="text-sm font-medium">Definido por penales {textoPenales(p)}</p>
        )}
        <div className="text-texto-suave flex flex-wrap justify-center gap-x-4 gap-y-1 text-sm">
          <Insignia tono={p.estado === 'FINALIZADO' || p.estado === 'WO' ? 'verde' : 'gris'}>
            {ETIQUETA_ESTADO_PARTIDO[p.estado]}
          </Insignia>
          {p.inicio && <span>{formatearFechaHora(p.inicio)}</span>}
          {p.cancha && <span>{p.cancha}</span>}
          {p.arbitro && <span>Árbitro: {p.arbitro}</span>}
          {p.figura && <span>⭐ Figura: {p.figura}</span>}
        </div>
      </Tarjeta>

      <Tarjeta className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold">Eventos del partido</h2>
        {detalle.eventos.length === 0 ? (
          <p className="text-texto-suave text-sm">No hay eventos registrados.</p>
        ) : (
          <ol className="flex flex-col gap-2 text-sm">
            {detalle.eventos.map((e) => {
              const esLocal = e.inscripcionId === p.local?.inscripcionId
              return (
                <li
                  key={e.id}
                  className={`flex items-center gap-3 ${esLocal ? '' : 'flex-row-reverse text-right'}`}
                >
                  <span className="bg-borde rounded-full px-2 py-0.5 text-xs font-semibold">
                    {e.minuto}'
                  </span>
                  <span aria-hidden="true">{ICONO_EVENTO[e.tipo]}</span>
                  <span>
                    <Link to={`/jugadores/${e.jugadorId}`} className="font-medium hover:underline">
                      {e.dorsal != null && `${e.dorsal} · `}
                      {e.jugador}
                    </Link>
                    <span className="text-texto-suave">
                      {' '}
                      · {ETIQUETA_EVENTO[e.tipo]} · {e.equipo}
                    </span>
                  </span>
                </li>
              )
            })}
          </ol>
        )}
      </Tarjeta>
    </div>
  )
}
