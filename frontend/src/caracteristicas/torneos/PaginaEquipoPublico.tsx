import { Link, useParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta, TituloTarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'

import { usePerfilEquipo } from './consultas'
import { ETIQUETA_ESTADO_TORNEO } from './etiquetas'
import { TarjetaPartido } from './TarjetaPartido'

export function PaginaEquipoPublico() {
  const id = Number(useParams().id)
  const { data, isPending, isError, error } = usePerfilEquipo(id)
  return (
    <div className="flex flex-col gap-6">
      {isPending && <Cargando texto="Cargando equipo…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && (
        <>
          <header className="bg-noche relative isolate flex items-end gap-5 overflow-hidden rounded-[2rem] p-6 text-white sm:p-10">
            <span
              className="absolute -top-20 -right-20 -z-10 size-72 rounded-full opacity-40"
              style={{
                background: `radial-gradient(closest-side, ${data.equipo.color ?? 'var(--color-volt)'}, transparent)`,
              }}
              aria-hidden="true"
            />
            <span
              className="size-16 shrink-0 rounded-2xl ring-4 ring-white/15 sm:size-20"
              style={{ backgroundColor: data.equipo.color ?? 'var(--color-volt)' }}
              aria-hidden="true"
            />
            <div>
              <p className="text-sm font-semibold text-white/60">Equipo</p>
              <h1 className="text-[clamp(2.4rem,6vw,4.5rem)] leading-[0.9] font-extrabold uppercase">
                {data.equipo.nombre}
              </h1>
            </div>
          </header>
          <div className="grid gap-4 md:grid-cols-2">
            <Tarjeta className="flex flex-col">
              <TituloTarjeta titulo="Plantilla" className="mb-2" />
              <ul className="divide-borde/70 flex flex-col divide-y text-sm">
                {data.plantilla.map((j) => (
                  <li key={j.id} className="flex items-center gap-3 py-2">
                    <span className="bg-noche text-volt-vivo font-titulo inline-flex size-9 items-center justify-center rounded-full text-base font-extrabold tabular-nums">
                      {j.dorsal ?? '–'}
                    </span>
                    <Link to={`/jugadores/${j.id}`} className="hover:text-primario font-medium">
                      {j.nombre}
                    </Link>
                    {j.posicion && <span className="text-texto-suave"> · {j.posicion}</span>}
                  </li>
                ))}
              </ul>
            </Tarjeta>
            <Tarjeta className="flex flex-col self-start">
              <TituloTarjeta titulo="Torneos" className="mb-2" />
              <ul className="divide-borde/70 flex flex-col divide-y text-sm">
                {data.torneos.map((t) => (
                  <li key={t.torneoId} className="py-2">
                    <Link to={`/torneos/${t.torneoId}`} className="hover:text-primario font-medium">
                      {t.nombre}
                    </Link>
                    <span className="text-texto-suave"> · {ETIQUETA_ESTADO_TORNEO[t.estado]}</span>
                  </li>
                ))}
              </ul>
            </Tarjeta>
          </div>
          {data.partidos.length > 0 && (
            <section className="flex flex-col gap-2">
              <h2 className="text-3xl font-extrabold uppercase">Partidos recientes y próximos</h2>
              <div className="grid gap-3 md:grid-cols-2">
                {data.partidos.map((p) => (
                  <TarjetaPartido
                    key={p.id}
                    partido={p}
                    ruta={`/torneos/partidos/${p.id}`}
                    mostrarTorneo
                  />
                ))}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  )
}
