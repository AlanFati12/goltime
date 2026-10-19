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
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6">
      {isPending && <Cargando texto="Cargando jugador…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && (
        <>
          <header className="bg-noche flex items-center gap-5 rounded-[2rem] p-6 text-white sm:p-10">
            <span className="bg-volt text-noche font-titulo flex size-20 shrink-0 items-center justify-center rounded-full text-4xl font-extrabold uppercase">
              {data.nombre.charAt(0)}
            </span>
            <div>
              <h1 className="text-[clamp(2.2rem,5vw,4rem)] leading-[0.9] font-extrabold uppercase">
                {data.nombre}
              </h1>
              {data.posicion && (
                <p className="text-volt-vivo mt-1 font-semibold">{data.posicion}</p>
              )}
            </div>
          </header>
          <ul className="flex flex-col gap-3">
            {data.torneos.map((t) => (
              <li key={t.torneoId}>
                <Tarjeta className="flex flex-col gap-2">
                  <Link
                    to={`/torneos/${t.torneoId}`}
                    className="hover:text-primario font-titulo text-2xl font-extrabold uppercase"
                  >
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
                        className="bg-tenue flex flex-col-reverse rounded-2xl px-2 py-3"
                      >
                        <dt className="text-texto-suave text-xs">{etiqueta}</dt>
                        <dd className="font-titulo text-3xl leading-none font-extrabold">
                          {valor}
                        </dd>
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
