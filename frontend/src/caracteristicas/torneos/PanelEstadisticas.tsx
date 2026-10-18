import { Link } from 'react-router-dom'

import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'

import { useEstadisticas } from './consultas'

function Lista({
  titulo,
  vacio,
  filas,
}: {
  titulo: string
  vacio: string
  filas: { clave: number; jugadorId: number; jugador: string; equipo: string; valor: string }[]
}) {
  return (
    <Tarjeta className="flex flex-col gap-2">
      <h3 className="text-lg font-semibold">{titulo}</h3>
      {filas.length === 0 ? (
        <p className="text-texto-suave text-sm">{vacio}</p>
      ) : (
        <ol className="flex flex-col gap-1 text-sm">
          {filas.map((f, i) => (
            <li key={f.clave} className="flex items-center justify-between gap-2">
              <span>
                <span className="text-texto-suave mr-2 inline-block w-5">{i + 1}.</span>
                <Link to={`/jugadores/${f.jugadorId}`} className="font-medium hover:underline">
                  {f.jugador}
                </Link>
                <span className="text-texto-suave"> · {f.equipo}</span>
              </span>
              <span className="font-bold">{f.valor}</span>
            </li>
          ))}
        </ol>
      )}
    </Tarjeta>
  )
}

/** Goleadores, asistidores y tarjetas del torneo. */
export function PanelEstadisticas({
  torneoId,
  admin = false,
}: {
  torneoId: number
  admin?: boolean
}) {
  const { data, isPending } = useEstadisticas(torneoId, admin)
  if (isPending) return <Cargando texto="Cargando estadísticas…" />
  if (!data) return null
  return (
    <div className="grid gap-4 md:grid-cols-3">
      <Lista
        titulo="Goleadores"
        vacio="Aún no hay goles registrados."
        filas={data.goleadores.map((g) => ({
          clave: g.jugadorId,
          jugadorId: g.jugadorId,
          jugador: g.jugador,
          equipo: g.equipo,
          valor: `${g.goles}${g.penales ? ` (${g.penales} pen.)` : ''}`,
        }))}
      />
      <Lista
        titulo="Asistencias"
        vacio="Aún no hay asistencias registradas."
        filas={data.asistidores.map((g) => ({
          clave: g.jugadorId,
          jugadorId: g.jugadorId,
          jugador: g.jugador,
          equipo: g.equipo,
          valor: String(g.asistencias),
        }))}
      />
      <Lista
        titulo="Tarjetas"
        vacio="Aún no hay tarjetas registradas."
        filas={data.tarjetas.map((g) => ({
          clave: g.jugadorId,
          jugadorId: g.jugadorId,
          jugador: g.jugador,
          equipo: g.equipo,
          valor: `🟨 ${g.amarillas}  🟥 ${g.rojas}`,
        }))}
      />
    </div>
  )
}
