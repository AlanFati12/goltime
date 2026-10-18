import { useState } from 'react'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { cn } from '@/lib/utils'

import { usePartidos, usePosiciones } from './consultas'
import { Llaves } from './Llaves'
import { PanelEstadisticas } from './PanelEstadisticas'
import { TablaPosiciones } from './TablaPosiciones'
import { TarjetaPartido } from './TarjetaPartido'
import type { DetalleTorneo } from './tipos'
import { agruparPor } from './utilidades'

type Pestana = 'posiciones' | 'llaves' | 'calendario' | 'estadisticas' | 'equipos'

/** Contenido de un torneo con sus pestañas: tablas, llaves, calendario, estadísticas y equipos. */
export function PanelTorneo({
  detalle,
  admin = false,
}: {
  detalle: DetalleTorneo
  admin?: boolean
}) {
  const t = detalle.torneo
  const posiciones = usePosiciones(t.id, admin)
  const partidos = usePartidos(t.id, admin)
  const hayLlaves = partidos.data?.some((p) => p.tipoFase === 'ELIMINATORIA') ?? false
  const hayTablas = (posiciones.data?.length ?? 0) > 0
  const pestanas: { id: Pestana; texto: string; visible: boolean }[] = [
    { id: 'posiciones', texto: 'Posiciones', visible: hayTablas },
    { id: 'llaves', texto: 'Llaves', visible: hayLlaves },
    { id: 'calendario', texto: 'Calendario', visible: true },
    { id: 'estadisticas', texto: 'Estadísticas', visible: true },
    { id: 'equipos', texto: 'Equipos', visible: true },
  ]
  const visibles = pestanas.filter((p) => p.visible)
  const [elegida, setElegida] = useState<Pestana | null>(null)
  const actual = visibles.find((p) => p.id === elegida)?.id ?? visibles[0]?.id
  const rutaPartido = (id: number) => (admin ? `/admin/partidos/${id}` : `/torneos/partidos/${id}`)

  return (
    <div className="flex flex-col gap-4">
      <div role="tablist" aria-label="Secciones del torneo" className="flex flex-wrap gap-2">
        {visibles.map((p) => (
          <Boton
            key={p.id}
            role="tab"
            aria-selected={actual === p.id}
            variante={actual === p.id ? 'primario' : 'secundario'}
            tamano="sm"
            onClick={() => setElegida(p.id)}
          >
            {p.texto}
          </Boton>
        ))}
      </div>

      {(posiciones.isPending || partidos.isPending) && <Cargando texto="Cargando torneo…" />}
      {partidos.isError && <Alerta tipo="error">{mensajeDeError(partidos.error)}</Alerta>}

      {actual === 'posiciones' && (
        <div className="flex flex-col gap-6">
          {posiciones.data?.map((tabla) => (
            <TablaPosiciones
              key={`${tabla.faseId}-${tabla.grupoId}`}
              tabla={tabla}
              clasifican={t.formato === 'GRUPOS_Y_ELIMINACION' ? (t.clasificadosPorGrupo ?? 0) : 0}
            />
          ))}
          <p className="text-texto-suave text-xs">
            Puntos: victoria {t.puntosVictoria}, empate {t.puntosEmpate}, derrota {t.puntosDerrota}.
            Desempate: {t.criteriosDesempate.join(' → ').replaceAll('_', ' ').toLowerCase()}.
          </p>
        </div>
      )}

      {actual === 'llaves' && partidos.data && (
        <Llaves partidos={partidos.data} ruta={(p) => rutaPartido(p.id)} />
      )}

      {actual === 'calendario' && partidos.data && (
        <Calendario partidos={partidos.data} ruta={rutaPartido} />
      )}

      {actual === 'estadisticas' && <PanelEstadisticas torneoId={t.id} admin={admin} />}

      {actual === 'equipos' && (
        <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {detalle.inscripciones.map((i) => (
            <li key={i.id}>
              <Tarjeta className="flex items-center gap-3 p-4">
                <span
                  className={cn('size-4 shrink-0 rounded-full border')}
                  style={{ backgroundColor: i.color ?? 'transparent' }}
                  aria-hidden="true"
                />
                <span className="font-medium">{i.equipo}</span>
                <span className="text-texto-suave ml-auto text-xs">{i.jugadores} jugadores</span>
              </Tarjeta>
            </li>
          ))}
          {detalle.inscripciones.length === 0 && (
            <li className="text-texto-suave text-sm">Aún no hay equipos inscritos.</li>
          )}
        </ul>
      )}
    </div>
  )
}

function Calendario({
  partidos,
  ruta,
}: {
  partidos: import('./tipos').Partido[]
  ruta: (id: number) => string
}) {
  const jugables = partidos.filter((p) => p.estado !== 'BYE')
  if (jugables.length === 0) {
    return (
      <p className="text-texto-suave text-sm">
        Aún no hay partidos. El fixture se publica al generarlo.
      </p>
    )
  }
  const porRonda = [
    ...agruparPor(
      jugables,
      (p) => `${p.tipoFase === 'ELIMINATORIA' ? 'E' : 'F'}-${p.ronda}`,
    ).entries(),
  ]
  return (
    <div className="flex flex-col gap-6">
      {porRonda.map(([clave, lista]) => (
        <section key={clave} className="flex flex-col gap-2">
          <h3 className="text-lg font-semibold">
            {lista[0].nombreRonda ?? `Ronda ${lista[0].ronda}`}
          </h3>
          <div className="grid gap-3 md:grid-cols-2">
            {lista.map((p) => (
              <TarjetaPartido key={p.id} partido={p} ruta={ruta(p.id)} />
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}
