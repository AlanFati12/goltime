import { useState } from 'react'
import { Link } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearDia } from '@/lib/formato'

import { useTorneos } from './consultas'
import { ETIQUETA_ESTADO_TORNEO, ETIQUETA_FORMATO, TONO_ESTADO_TORNEO } from './etiquetas'
import type { EstadoTorneo, Torneo } from './tipos'

const GRUPOS: { id: string; texto: string; estados: EstadoTorneo[] }[] = [
  { id: 'activos', texto: 'Activos', estados: ['EN_CURSO'] },
  { id: 'proximos', texto: 'Próximos', estados: ['INSCRIPCIONES'] },
  { id: 'finalizados', texto: 'Finalizados', estados: ['FINALIZADO', 'CANCELADO'] },
]

export function TarjetaTorneo({ t, ruta }: { t: Torneo; ruta: string }) {
  return (
    <Tarjeta className="flex h-full flex-col gap-2">
      <div className="flex flex-wrap items-center gap-2">
        <Insignia tono={TONO_ESTADO_TORNEO[t.estado]}>{ETIQUETA_ESTADO_TORNEO[t.estado]}</Insignia>
        <Insignia>{ETIQUETA_FORMATO[t.formato]}</Insignia>
      </div>
      <h2 className="text-lg font-semibold">
        <Link to={ruta} className="hover:underline">
          {t.nombre}
        </Link>
      </h2>
      <p className="text-texto-suave text-sm">
        {t.sedeNombre} · Fútbol {t.jugadoresPorEquipo} · {t.equiposInscritos}
        {t.cupoEquipos ? ` de ${t.cupoEquipos}` : ''} equipos
      </p>
      {t.fechaInicio && (
        <p className="text-sm capitalize">
          {formatearDia(t.fechaInicio)}
          {t.fechaFin ? ` → ${formatearDia(t.fechaFin)}` : ''}
        </p>
      )}
      {t.valorInscripcion > 0 && (
        <p className="text-sm">Inscripción: {formatearCOP(t.valorInscripcion)}</p>
      )}
      {t.campeon && <p className="text-sm font-medium">🏆 Campeón: {t.campeon}</p>}
    </Tarjeta>
  )
}

/** Torneos públicos agrupados en activos, próximos y finalizados. */
export function PaginaTorneos() {
  const { data, isPending, isError, error } = useTorneos(false)
  const [grupo, setGrupo] = useState('activos')
  const actual = GRUPOS.find((g) => g.id === grupo)!
  const lista = data?.filter((t) => actual.estados.includes(t.estado)) ?? []

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-4 p-4 sm:p-6">
      <h1 className="text-3xl font-bold">Torneos</h1>
      <div role="tablist" aria-label="Estado del torneo" className="flex gap-2">
        {GRUPOS.map((g) => (
          <Boton
            key={g.id}
            role="tab"
            aria-selected={grupo === g.id}
            tamano="sm"
            variante={grupo === g.id ? 'primario' : 'secundario'}
            onClick={() => setGrupo(g.id)}
          >
            {g.texto} ({data?.filter((t) => g.estados.includes(t.estado)).length ?? 0})
          </Boton>
        ))}
      </div>
      {isPending && <Cargando texto="Cargando torneos…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && lista.length === 0 && (
        <Tarjeta className="text-texto-suave text-center text-sm">
          No hay torneos en esta categoría.
        </Tarjeta>
      )}
      <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {lista.map((t) => (
          <li key={t.id}>
            <TarjetaTorneo t={t} ruta={`/torneos/${t.id}`} />
          </li>
        ))}
      </ul>
    </div>
  )
}
