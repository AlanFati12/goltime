import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'

import { useEstadisticas } from './consultas'
import { IconoTarjeta } from './IconoEvento'
import { cn } from '@/lib/utils'

function Lista({
  titulo,
  vacio,
  filas,
}: {
  titulo: string
  vacio: string
  filas: { clave: number; jugadorId: number; jugador: string; equipo: string; valor: ReactNode }[]
}) {
  return (
    <Tarjeta className="flex flex-col gap-2">
      <h3 className="text-2xl font-extrabold uppercase">{titulo}</h3>
      {filas.length === 0 ? (
        <p className="text-texto-suave text-sm">{vacio}</p>
      ) : (
        <ol className="flex flex-col gap-1 text-sm">
          {filas.map((f, i) => (
            <li
              key={f.clave}
              className={cn(
                'flex items-center justify-between gap-2 rounded-xl px-3 py-2',
                i === 0 &&
                  'bg-noche [&_a:hover]:text-volt-vivo text-white [&_.text-texto-suave]:text-white/55',
              )}
            >
              <span className="min-w-0">
                <span
                  className={cn(
                    'font-titulo mr-2 inline-block w-5 text-lg font-extrabold',
                    i === 0 ? 'text-volt-vivo' : 'text-texto-suave',
                  )}
                >
                  {i + 1}
                </span>
                <Link to={`/jugadores/${f.jugadorId}`} className="hover:text-primario font-medium">
                  {f.jugador}
                </Link>
                <span className="text-texto-suave"> · {f.equipo}</span>
              </span>
              <span
                className={cn(
                  'inline-flex shrink-0 items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-bold tabular-nums',
                  i === 0 ? 'bg-volt text-noche' : 'bg-tenue',
                )}
              >
                {f.valor}
              </span>
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
          valor: (
            <>
              <IconoTarjeta className="h-3 w-2.5" /> {g.amarillas}
              <IconoTarjeta roja className="ml-1 h-3 w-2.5" /> {g.rojas}
            </>
          ),
        }))}
      />
    </div>
  )
}
