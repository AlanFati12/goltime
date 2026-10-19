import { Link } from 'react-router-dom'

import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { formatearFechaHora } from '@/lib/formato'

import { Star } from 'lucide-react'

import { ETIQUETA_EVENTO, ETIQUETA_ESTADO_PARTIDO } from './etiquetas'
import { IconoEvento } from './IconoEvento'
import type { DetallePartido } from './tipos'
import { marcador, nombreLado, textoPenales, textoRonda } from './utilidades'

/** Marcador grande del partido con su información y la línea de tiempo de eventos. */
export function EncabezadoPartido({
  detalle,
  admin = false,
  mostrarEventos = true,
}: {
  detalle: DetallePartido
  admin?: boolean
  /** En la administración la planilla reemplaza a la línea de tiempo. */
  mostrarEventos?: boolean
}) {
  const p = detalle.partido
  const torneoRuta = admin ? `/admin/torneos/${p.torneoId}` : `/torneos/${p.torneoId}`
  return (
    <div className="flex flex-col gap-4">
      <div className="bg-noche relative isolate flex flex-col items-center gap-6 overflow-hidden rounded-[2rem] px-5 py-10 text-center text-white sm:py-14">
        <img
          src="/img/jugadores-movimiento.webp"
          alt=""
          className="absolute inset-0 -z-10 size-full object-cover opacity-25"
        />
        <div className="from-noche via-noche/80 absolute inset-0 -z-10 bg-gradient-to-t to-transparent" />
        <Link
          to={torneoRuta}
          className="text-volt-vivo text-sm font-semibold underline-offset-4 hover:underline"
        >
          {p.torneo} · {textoRonda(p)}
        </Link>
        <div className="grid w-full max-w-3xl grid-cols-[1fr_auto_1fr] items-center gap-3 sm:gap-6">
          <span className="font-titulo text-right text-2xl leading-none font-extrabold uppercase sm:text-4xl">
            {nombreLado(p.local)}
          </span>
          <span className="bg-volt text-noche font-titulo animate-subir cifras rounded-2xl px-5 py-3 text-5xl leading-none font-extrabold sm:px-8 sm:text-7xl">
            {marcador(p)}
          </span>
          <span className="font-titulo text-left text-2xl leading-none font-extrabold uppercase sm:text-4xl">
            {nombreLado(p.visitante)}
          </span>
        </div>
        {textoPenales(p) && <p className="font-semibold">Definido por penales {textoPenales(p)}</p>}
        <div className="flex flex-wrap items-center justify-center gap-x-4 gap-y-2 text-sm text-white/70">
          <Insignia tono={p.estado === 'FINALIZADO' || p.estado === 'WO' ? 'verde' : 'gris'}>
            {ETIQUETA_ESTADO_PARTIDO[p.estado]}
          </Insignia>
          {p.inicio && <span className="capitalize">{formatearFechaHora(p.inicio)}</span>}
          {p.cancha && <span>{p.cancha}</span>}
          {p.arbitro && <span>Árbitro: {p.arbitro}</span>}
          {p.figura && (
            <span className="inline-flex items-center gap-1.5">
              <Star className="text-volt-vivo size-4 fill-current" aria-hidden />
              Figura: {p.figura}
            </span>
          )}
        </div>
      </div>

      {mostrarEventos && (
        <Tarjeta className="flex flex-col gap-3">
          <h2 className="text-2xl font-extrabold uppercase">Eventos del partido</h2>
          {detalle.eventos.length === 0 ? (
            <p className="text-texto-suave py-4 text-center text-sm">No hay eventos registrados.</p>
          ) : (
            <ol className="flex flex-col gap-2 text-sm">
              {detalle.eventos.map((e) => {
                const esLocal = e.inscripcionId === p.local?.inscripcionId
                return (
                  <li
                    key={e.id}
                    className={`flex items-center gap-3 ${esLocal ? '' : 'flex-row-reverse text-right'}`}
                  >
                    <span className="bg-noche text-volt-vivo min-w-11 rounded-full px-2 py-0.5 text-center text-xs font-bold tabular-nums">
                      {e.minuto != null ? `${e.minuto}'` : '—'}
                    </span>
                    <IconoEvento tipo={e.tipo} />
                    <span>
                      <Link
                        to={`/jugadores/${e.jugadorId}`}
                        className="hover:text-primario font-medium"
                      >
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
      )}
    </div>
  )
}
