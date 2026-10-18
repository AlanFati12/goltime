import { Link, useParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearDia } from '@/lib/formato'

import { useTorneo } from './consultas'
import { ETIQUETA_ESTADO_TORNEO, ETIQUETA_FORMATO, TONO_ESTADO_TORNEO } from './etiquetas'
import { PanelTorneo } from './PanelTorneo'
import type { Torneo } from './tipos'

export function ResumenTorneo({ t }: { t: Torneo }) {
  return (
    <Tarjeta className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2">
        <Insignia tono={TONO_ESTADO_TORNEO[t.estado]}>{ETIQUETA_ESTADO_TORNEO[t.estado]}</Insignia>
        <Insignia>{ETIQUETA_FORMATO[t.formato]}</Insignia>
        <Insignia>Fútbol {t.jugadoresPorEquipo}</Insignia>
      </div>
      <p className="text-texto-suave text-sm">
        {t.sedeNombre}
        {t.fechaInicio && (
          <span className="capitalize">
            {' '}
            · {formatearDia(t.fechaInicio)}
            {t.fechaFin ? ` → ${formatearDia(t.fechaFin)}` : ''}
          </span>
        )}
        {t.valorInscripcion > 0 && ` · Inscripción ${formatearCOP(t.valorInscripcion)}`}
      </p>
      {t.descripcion && <p className="text-sm">{t.descripcion}</p>}
      {t.estado === 'FINALIZADO' && t.campeon && (
        <div className="bg-primario-claro rounded-boton flex flex-wrap gap-x-6 gap-y-1 px-4 py-3 text-sm">
          <span className="font-bold">🏆 Campeón: {t.campeon}</span>
          {t.subcampeon && <span>🥈 Subcampeón: {t.subcampeon}</span>}
          {t.tercero && <span>🥉 Tercero: {t.tercero}</span>}
        </div>
      )}
      {t.premiosTexto && (
        <details className="text-sm">
          <summary className="cursor-pointer font-medium">Premios</summary>
          <p className="mt-1 whitespace-pre-line">{t.premiosTexto}</p>
        </details>
      )}
      {t.reglamento && (
        <details className="text-sm">
          <summary className="cursor-pointer font-medium">Reglamento</summary>
          <p className="mt-1 whitespace-pre-line">{t.reglamento}</p>
        </details>
      )}
    </Tarjeta>
  )
}

export function PaginaTorneoDetalle() {
  const id = Number(useParams().id)
  const { data, isPending, isError, error } = useTorneo(id, false)
  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-4 p-4 sm:p-6">
      <Link to="/torneos" className="text-primario text-sm hover:underline">
        ← Todos los torneos
      </Link>
      {isPending && <Cargando texto="Cargando torneo…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && (
        <>
          <h1 className="text-3xl font-bold">{data.torneo.nombre}</h1>
          <ResumenTorneo t={data.torneo} />
          <PanelTorneo detalle={data} />
        </>
      )}
    </div>
  )
}
