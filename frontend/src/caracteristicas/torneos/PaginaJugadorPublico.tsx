import { Link, useParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'

import { usePerfilJugador } from './consultas'

export function PaginaJugadorPublico() {
  const id = Number(useParams().id)
  const { data, isPending, isError, error } = usePerfilJugador(id)
  return (
    <div className="flex max-w-3xl flex-col gap-6">
      {isPending && <Cargando texto="Cargando jugador…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && (
        <>
          <div className="flex items-center gap-4">
            <span className="bg-primario flex size-14 items-center justify-center rounded-full text-xl font-bold text-white">
              {data.nombre.charAt(0)}
            </span>
            <div>
              <h1 className="text-2xl font-bold sm:text-[1.75rem]">{data.nombre}</h1>
              {data.posicion && <p className="text-texto-suave">{data.posicion}</p>}
            </div>
          </div>
          <ul className="flex flex-col gap-3">
            {data.torneos.map((t) => (
              <li key={t.torneoId}>
                <Tarjeta className="flex flex-col gap-2">
                  <Link to={`/torneos/${t.torneoId}`} className="hover:text-primario font-semibold">
                    {t.torneo}
                  </Link>
                  <p className="text-texto-suave text-sm">{t.equipo}</p>
                  <dl className="grid grid-cols-4 gap-2 text-center">
                    {[
                      ['Goles', t.goles],
                      ['Asistencias', t.asistencias],
                      ['Amarillas', t.amarillas],
                      ['Rojas', t.rojas],
                    ].map(([etiqueta, valor]) => (
                      <div
                        key={etiqueta}
                        className="bg-tenue/70 rounded-boton flex flex-col-reverse px-2 py-2"
                      >
                        <dt className="text-texto-suave text-xs">{etiqueta}</dt>
                        <dd className="font-titulo text-lg font-bold">{valor}</dd>
                      </div>
                    ))}
                  </dl>
                </Tarjeta>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  )
}
