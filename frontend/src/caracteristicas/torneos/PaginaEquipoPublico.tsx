import { Link, useParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'

import { usePerfilEquipo } from './consultas'
import { ETIQUETA_ESTADO_TORNEO } from './etiquetas'
import { TarjetaPartido } from './TarjetaPartido'

export function PaginaEquipoPublico() {
  const id = Number(useParams().id)
  const { data, isPending, isError, error } = usePerfilEquipo(id)
  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-4 p-4 sm:p-6">
      {isPending && <Cargando texto="Cargando equipo…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && (
        <>
          <h1 className="flex items-center gap-3 text-3xl font-bold">
            <span
              className="size-6 rounded-full border"
              style={{ backgroundColor: data.equipo.color ?? 'transparent' }}
              aria-hidden="true"
            />
            {data.equipo.nombre}
          </h1>
          <div className="grid gap-4 md:grid-cols-2">
            <Tarjeta className="flex flex-col gap-2">
              <h2 className="text-lg font-semibold">Plantilla</h2>
              <ul className="flex flex-col gap-1 text-sm">
                {data.plantilla.map((j) => (
                  <li key={j.id}>
                    <span className="text-texto-suave inline-block w-8">{j.dorsal ?? ''}</span>
                    <Link to={`/jugadores/${j.id}`} className="font-medium hover:underline">
                      {j.nombre}
                    </Link>
                    {j.posicion && <span className="text-texto-suave"> · {j.posicion}</span>}
                  </li>
                ))}
              </ul>
            </Tarjeta>
            <Tarjeta className="flex flex-col gap-2">
              <h2 className="text-lg font-semibold">Torneos</h2>
              <ul className="flex flex-col gap-1 text-sm">
                {data.torneos.map((t) => (
                  <li key={t.torneoId}>
                    <Link to={`/torneos/${t.torneoId}`} className="font-medium hover:underline">
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
              <h2 className="text-lg font-semibold">Partidos recientes y próximos</h2>
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
