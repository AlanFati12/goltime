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
    <div className="mx-auto flex max-w-3xl flex-col gap-4 p-4 sm:p-6">
      {isPending && <Cargando texto="Cargando jugador…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && (
        <>
          <h1 className="text-3xl font-bold">{data.nombre}</h1>
          {data.posicion && <p className="text-texto-suave">{data.posicion}</p>}
          <ul className="flex flex-col gap-3">
            {data.torneos.map((t) => (
              <li key={t.torneoId}>
                <Tarjeta className="flex flex-col gap-2">
                  <Link to={`/torneos/${t.torneoId}`} className="font-semibold hover:underline">
                    {t.torneo}
                  </Link>
                  <p className="text-texto-suave text-sm">{t.equipo}</p>
                  <p className="flex flex-wrap gap-x-5 gap-y-1 text-sm">
                    <span>⚽ {t.goles} goles</span>
                    <span>👟 {t.asistencias} asistencias</span>
                    <span>🟨 {t.amarillas}</span>
                    <span>🟥 {t.rojas}</span>
                  </p>
                </Tarjeta>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  )
}
