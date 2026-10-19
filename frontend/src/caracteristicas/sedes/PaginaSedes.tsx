import { ArrowUpRight, MapPin } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Esqueleto } from '@/componentes/ui/Cargando'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { PortadaPagina } from '@/componentes/ui/PortadaPagina'
import { Revelar } from '@/componentes/ui/Revelar'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP } from '@/lib/formato'
import { cn } from '@/lib/utils'

import { useSedesPublicas } from './consultas'
import { horarioDeHoy, modalidadesDe, precioDesde } from './horario'

export function PaginaSedes() {
  const { data, isPending, isError, error } = useSedesPublicas()

  return (
    <div className="flex flex-col gap-10">
      <PortadaPagina titulo="Sedes y canchas" imagen="/img/cancha-aerea.webp" compacta>
        <p className="max-w-xl text-lg">
          Elige una sede para ver sus canchas, horarios y precios por franja.
        </p>
      </PortadaPagina>

      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <EstadoVacio icono={MapPin} titulo="Todavía no hay sedes disponibles" />
      )}

      <div className="flex flex-col gap-6">
        {isPending && [0, 1].map((i) => <Esqueleto key={i} className="h-72 rounded-[2rem]" />)}
        {data?.map((s, i) => {
          const desde = precioDesde(s)
          const hoy = horarioDeHoy(s.horario)
          return (
            <Revelar key={s.id} i={i}>
              <Link
                to={`/sedes/${s.id}`}
                className="group bg-superficie border-borde/80 hover:shadow-flotante grid overflow-hidden rounded-[2rem] border transition-[border-color,box-shadow] duration-300 hover:border-transparent md:grid-cols-[1.1fr_1fr]"
              >
                <div className="bg-tenue relative aspect-[16/10] overflow-hidden md:aspect-auto md:min-h-80">
                  <img
                    src={s.fotos[0]?.url ?? '/img/cancha-aerea.webp'}
                    alt={`Sede ${s.nombre}`}
                    loading="lazy"
                    className="absolute inset-0 size-full object-cover transition-transform duration-[1200ms] ease-[var(--ease-salida)] group-hover:scale-105"
                  />
                </div>
                <div className="flex flex-col gap-5 p-6 sm:p-9">
                  <div className="flex items-start justify-between gap-4">
                    <h2 className="text-4xl leading-[0.95] font-extrabold uppercase sm:text-5xl">
                      {s.nombre}
                    </h2>
                    <span className="bg-tenue group-hover:bg-volt text-noche flex size-12 shrink-0 items-center justify-center rounded-full transition-colors duration-300">
                      <ArrowUpRight
                        className="size-5 transition-transform duration-300 group-hover:rotate-45"
                        aria-hidden
                      />
                    </span>
                  </div>
                  <p className="text-texto-suave flex items-center gap-1.5">
                    <MapPin className="size-4 shrink-0" aria-hidden />
                    {s.direccion} · {s.ciudad}
                  </p>
                  {s.descripcion && <p className="max-w-md">{s.descripcion}</p>}
                  <div className="flex flex-wrap gap-2">
                    {modalidadesDe(s).map((m) => (
                      <span
                        key={m}
                        className="bg-volt-claro text-primario-oscuro rounded-full px-3 py-1 text-sm font-bold"
                      >
                        {m}
                      </span>
                    ))}
                    <span className="bg-tenue text-texto-suave rounded-full px-3 py-1 text-sm font-semibold">
                      {s.canchas.length} {s.canchas.length === 1 ? 'cancha' : 'canchas'}
                    </span>
                  </div>
                  <div className="border-borde mt-auto flex flex-wrap items-end justify-between gap-4 border-t pt-5">
                    <p
                      className={cn(
                        'flex items-center gap-2 text-sm font-semibold',
                        hoy.abiertaAhora ? 'text-primario' : 'text-texto-suave',
                      )}
                    >
                      <span
                        className={cn(
                          'size-2 rounded-full',
                          hoy.abiertaAhora ? 'bg-volt' : 'bg-texto-suave/40',
                        )}
                        aria-hidden
                      />
                      {hoy.texto}
                    </p>
                    {desde != null && (
                      <p className="text-texto-suave text-sm">
                        Desde{' '}
                        <span className="font-titulo cifras text-texto text-3xl font-extrabold">
                          {formatearCOP(desde)}
                        </span>{' '}
                        / hora
                      </p>
                    )}
                  </div>
                </div>
              </Link>
            </Revelar>
          )
        })}
      </div>
    </div>
  )
}
