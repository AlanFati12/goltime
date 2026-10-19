import { ClipboardPen } from 'lucide-react'
import { useState } from 'react'

import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { Insignia } from '@/componentes/ui/Insignia'
import { Segmentado } from '@/componentes/ui/Pestanas'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { formatearFechaHora } from '@/lib/formato'

import { usePartidos } from '../consultas'
import { ETIQUETA_ESTADO_PARTIDO } from '../etiquetas'
import type { Partido } from '../tipos'
import { marcador, nombreLado, textoRonda } from '../utilidades'

type Filtro = 'pendientes' | 'jugados' | 'todos'

const jugado = (p: Partido) => p.estado === 'FINALIZADO' || p.estado === 'WO'

/** Todos los partidos del torneo con acceso directo a su planilla (resultado, goles, asistencias y tarjetas). */
export function ListaPartidosAdmin({ torneoId, enCurso }: { torneoId: number; enCurso: boolean }) {
  const partidos = usePartidos(torneoId, true)
  const [filtro, setFiltro] = useState<Filtro>('pendientes')
  const jugables = (partidos.data ?? []).filter((p) => p.estado !== 'BYE')
  const lista = jugables.filter((p) =>
    filtro === 'todos' ? true : filtro === 'jugados' ? jugado(p) : !jugado(p),
  )

  return (
    <Tarjeta className="flex flex-col overflow-hidden p-0 sm:p-0">
      <div className="flex flex-wrap items-center justify-between gap-3 px-5 pt-5 pb-3">
        <div>
          <h2 className="text-xl font-extrabold uppercase">Partidos</h2>
          <p className="text-texto-suave text-sm">
            Abre la planilla para programar, registrar el resultado y quién marcó o asistió.
          </p>
        </div>
        <Segmentado
          etiqueta="Filtrar partidos"
          opciones={[
            { id: 'pendientes' as const, texto: 'Por jugar' },
            { id: 'jugados' as const, texto: 'Jugados' },
            { id: 'todos' as const, texto: 'Todos' },
          ]}
          actual={filtro}
          alCambiar={setFiltro}
        />
      </div>
      {partidos.isPending && <Cargando texto="Cargando partidos…" />}
      {partidos.data && jugables.length === 0 && (
        <EstadoVacio
          className="m-5 mt-0"
          titulo="Aún no hay partidos"
          descripcion="Genera el fixture en la pestaña Fixture."
        />
      )}
      {jugables.length > 0 && lista.length === 0 && (
        <p className="text-texto-suave px-5 pb-5 text-sm">No hay partidos en este filtro.</p>
      )}
      {lista.length > 0 && (
        <div className="overflow-x-auto">
          <table className="tabla min-w-[40rem]">
            <thead>
              <tr>
                <th>Ronda</th>
                <th className="text-right">Local</th>
                <th className="text-center">Marcador</th>
                <th>Visitante</th>
                <th>Fecha</th>
                <th className="text-right">Planilla</th>
              </tr>
            </thead>
            <tbody>
              {lista.map((p) => {
                const listo = !!p.local?.inscripcionId && !!p.visitante?.inscripcionId
                return (
                  <tr key={p.id}>
                    <td className="text-texto-suave text-xs whitespace-nowrap">{textoRonda(p)}</td>
                    <td className="text-right font-medium">{nombreLado(p.local)}</td>
                    <td className="text-center">
                      <span className="bg-tenue font-titulo inline-block min-w-14 rounded-md px-2 py-0.5 font-bold tabular-nums">
                        {marcador(p)}
                      </span>
                    </td>
                    <td className="font-medium">{nombreLado(p.visitante)}</td>
                    <td className="text-xs whitespace-nowrap">
                      {p.inicio ? (
                        <span className="capitalize">{formatearFechaHora(p.inicio)}</span>
                      ) : (
                        <Insignia tono="ambar">{ETIQUETA_ESTADO_PARTIDO[p.estado]}</Insignia>
                      )}
                    </td>
                    <td className="text-right">
                      <EnlaceBoton
                        to={`/admin/partidos/${p.id}`}
                        tamano="sm"
                        variante={enCurso && listo && !jugado(p) ? 'primario' : 'secundario'}
                      >
                        <ClipboardPen aria-hidden />
                        {jugado(p) ? 'Editar' : enCurso && listo ? 'Cargar resultado' : 'Abrir'}
                      </EnlaceBoton>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </Tarjeta>
  )
}
