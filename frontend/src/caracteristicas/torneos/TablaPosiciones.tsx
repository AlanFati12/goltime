import { Link } from 'react-router-dom'

import { cn } from '@/lib/utils'

import type { Tabla } from './tipos'

const COLOR_FORMA: Record<string, string> = {
  G: 'bg-volt text-noche',
  E: 'bg-borde text-texto',
  P: 'bg-peligro text-white',
}

/** Tabla de posiciones de una liga o grupo. Los números los calcula el backend. */
export function TablaPosiciones({ tabla, clasifican = 0 }: { tabla: Tabla; clasifican?: number }) {
  return (
    <div className="flex flex-col gap-2">
      <h3 className="text-2xl font-extrabold uppercase">
        {tabla.grupo ? `Grupo ${tabla.grupo}` : tabla.fase}
      </h3>
      <div className="border-borde/80 bg-superficie rounded-tarjeta overflow-x-auto border">
        <table className="w-full min-w-[34rem] text-sm">
          <caption className="sr-only">Tabla de posiciones {tabla.grupo ?? tabla.fase}</caption>
          <thead className="bg-noche text-left text-xs text-white/70">
            <tr>
              <th className="px-3 py-2">#</th>
              <th className="px-3 py-2">Equipo</th>
              <th className="px-2 py-2 text-center" title="Partidos jugados">
                PJ
              </th>
              <th className="px-2 py-2 text-center" title="Ganados">
                G
              </th>
              <th className="px-2 py-2 text-center" title="Empatados">
                E
              </th>
              <th className="px-2 py-2 text-center" title="Perdidos">
                P
              </th>
              <th className="px-2 py-2 text-center" title="Goles a favor">
                GF
              </th>
              <th className="px-2 py-2 text-center" title="Goles en contra">
                GC
              </th>
              <th className="px-2 py-2 text-center" title="Diferencia de gol">
                DG
              </th>
              <th className="text-volt-vivo px-2 py-2 text-center font-bold" title="Puntos">
                Pts
              </th>
              <th className="px-3 py-2">Forma</th>
            </tr>
          </thead>
          <tbody>
            {tabla.filas.map((f) => (
              <tr
                key={f.inscripcionId}
                className={cn(
                  'border-borde hover:bg-tenue/60 border-t transition-colors duration-150',
                  f.posicion <= clasifican && 'bg-volt-claro/50',
                )}
              >
                <td className="px-3 py-2">
                  <span
                    className={cn(
                      'font-titulo inline-flex size-7 items-center justify-center rounded-full text-base font-extrabold',
                      f.posicion === 1 ? 'bg-volt text-noche' : 'text-texto-suave',
                    )}
                  >
                    {f.posicion}
                  </span>
                </td>
                <td className="px-3 py-2">
                  {f.equipoId ? (
                    <Link
                      to={`/equipos/${f.equipoId}`}
                      className="hover:text-primario font-semibold transition-colors"
                    >
                      {f.equipo}
                    </Link>
                  ) : (
                    f.equipo
                  )}
                </td>
                <td className="px-2 py-2 text-center">{f.jugados}</td>
                <td className="px-2 py-2 text-center">{f.ganados}</td>
                <td className="px-2 py-2 text-center">{f.empatados}</td>
                <td className="px-2 py-2 text-center">{f.perdidos}</td>
                <td className="px-2 py-2 text-center">{f.golesFavor}</td>
                <td className="px-2 py-2 text-center">{f.golesContra}</td>
                <td className="px-2 py-2 text-center">
                  {f.diferencia > 0 ? `+${f.diferencia}` : f.diferencia}
                </td>
                <td className="font-titulo px-2 py-2 text-center text-lg font-extrabold">
                  {f.puntos}
                </td>
                <td className="px-3 py-2">
                  <span className="flex gap-1">
                    {f.forma.map((r, i) => (
                      <span
                        key={i}
                        className={cn(
                          'inline-flex size-5 items-center justify-center rounded-full text-[10px] font-bold',
                          COLOR_FORMA[r],
                        )}
                        aria-label={r === 'G' ? 'Ganó' : r === 'E' ? 'Empató' : 'Perdió'}
                      >
                        {r}
                      </span>
                    ))}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
