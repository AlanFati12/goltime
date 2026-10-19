import { Check, Copy, TicketPercent } from 'lucide-react'
import { useState } from 'react'

import { Alerta } from '@/componentes/ui/Alerta'
import { Esqueleto } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { PortadaPagina } from '@/componentes/ui/PortadaPagina'
import { Revelar } from '@/componentes/ui/Revelar'
import { mensajeDeError } from '@/lib/api'
import { formatearDia } from '@/lib/formato'
import { cn } from '@/lib/utils'

import { usePromocionesPublicas } from './consultas'
import type { Promocion } from './tipos'

const DIAS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo']
const hora = (m: number) =>
  `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`

/** Copia el código al portapapeles y confirma con un check durante un momento. */
function BotonCodigo({ codigo }: { codigo: string }) {
  const [copiado, setCopiado] = useState(false)
  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(codigo)
      setCopiado(true)
      window.setTimeout(() => setCopiado(false), 1600)
    } catch {
      // Sin permiso de portapapeles: el código sigue visible para copiarlo a mano.
    }
  }
  return (
    <button
      type="button"
      onClick={copiar}
      aria-label={`Copiar el código ${codigo}`}
      className="group bg-noche presionable flex w-full items-center justify-between gap-3 rounded-2xl px-5 py-3 text-white"
    >
      <span className="font-titulo text-2xl font-extrabold tracking-[0.12em] uppercase">
        {codigo}
      </span>
      <span
        className={cn(
          'flex items-center gap-1.5 text-sm font-semibold transition-colors',
          copiado ? 'text-volt-vivo' : 'text-white/60 group-hover:text-white',
        )}
        aria-live="polite"
      >
        {copiado ? (
          <Check className="size-4" aria-hidden />
        ) : (
          <Copy className="size-4" aria-hidden />
        )}
        {copiado ? 'Copiado' : 'Copiar'}
      </span>
    </button>
  )
}

function Tiquete({ p }: { p: Promocion }) {
  return (
    <article className="bg-superficie border-borde/80 relative flex h-full flex-col overflow-hidden rounded-[1.75rem] border">
      <div className="bg-volt text-noche relative flex items-end justify-between gap-3 px-6 pt-6 pb-7">
        <p className="font-titulo cifras text-6xl leading-[0.85] font-extrabold">
          {p.descuentoTexto}
        </p>
        <TicketPercent className="size-9 opacity-70" aria-hidden />
        {/* Muescas del tiquete. */}
        <span className="bg-fondo absolute -bottom-3 -left-3 size-6 rounded-full" aria-hidden />
        <span className="bg-fondo absolute -right-3 -bottom-3 size-6 rounded-full" aria-hidden />
      </div>
      <div className="border-borde mx-6 border-t-2 border-dashed" aria-hidden />
      <div className="flex flex-1 flex-col gap-3 p-6">
        {p.soloPrimeraReserva && (
          <span className="bg-volt-claro text-primario-oscuro self-start rounded-full px-3 py-1 text-xs font-bold">
            Primera reserva
          </span>
        )}
        <h2 className="text-2xl leading-none font-extrabold uppercase">{p.nombre}</h2>
        {p.descripcion && <p className="text-texto-suave">{p.descripcion}</p>}
        <p className="text-texto-suave text-sm capitalize">
          {formatearDia(p.fechaDesde)} → {formatearDia(p.fechaHasta)}
          {p.diasSemana.length > 0 && ` · ${p.diasSemana.map((d) => DIAS[d - 1]).join(', ')}`}
          {p.minutoInicio != null &&
            p.minutoFin != null &&
            ` · ${hora(p.minutoInicio)}–${hora(p.minutoFin)}`}
        </p>
        <div className="mt-auto pt-2">
          {p.codigo ? (
            <BotonCodigo codigo={p.codigo} />
          ) : (
            <p className="bg-volt-claro text-primario-oscuro rounded-2xl px-5 py-3 text-center font-semibold">
              Se aplica sola al reservar.
            </p>
          )}
        </div>
      </div>
    </article>
  )
}

/** Vitrina pública de promociones vigentes. */
export function PaginaPromociones() {
  const { data, isPending, isError, error } = usePromocionesPublicas()
  return (
    <div className="flex flex-col gap-8">
      <PortadaPagina
        titulo="Promociones"
        imagen="/img/guayos-sintetica.webp"
        compacta
        acciones={
          <EnlaceBoton to="/reservar" variante="volt" tamano="lg">
            Reservar una cancha
          </EnlaceBoton>
        }
      >
        <p className="max-w-xl text-lg">
          Escribe el código al solicitar tu reserva. Se aplica un solo descuento por reserva.
        </p>
      </PortadaPagina>
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <EstadoVacio icono={TicketPercent} titulo="No hay promociones vigentes por ahora" />
      )}
      <ul className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {isPending &&
          [0, 1, 2].map((i) => (
            <li key={i}>
              <Esqueleto className="h-80 rounded-[1.75rem]" />
            </li>
          ))}
        {data?.map((p, i) => (
          <Revelar como="li" key={p.id} i={i}>
            <Tiquete p={p} />
          </Revelar>
        ))}
      </ul>
    </div>
  )
}
