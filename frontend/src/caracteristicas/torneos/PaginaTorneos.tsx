import { useState } from 'react'
import { CalendarDays, Trophy, Users, Wallet } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { PortadaPagina } from '@/componentes/ui/PortadaPagina'
import { Revelar } from '@/componentes/ui/Revelar'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { Pestanas } from '@/componentes/ui/Pestanas'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
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
  const lleno = t.cupoEquipos ? Math.min(100, (t.equiposInscritos / t.cupoEquipos) * 100) : null
  return (
    <Link
      to={ruta}
      className="group bg-noche relative isolate flex h-full flex-col gap-5 overflow-hidden rounded-[1.75rem] p-6 text-white transition-transform duration-300 hover:-translate-y-1"
    >
      <Trophy
        className="text-volt/10 group-hover:text-volt/20 absolute -top-6 -right-6 -z-10 size-40 -rotate-12 transition-[color,transform] duration-500 group-hover:rotate-0"
        aria-hidden
      />
      <div className="flex flex-wrap gap-1.5">
        <Insignia tono={TONO_ESTADO_TORNEO[t.estado]}>{ETIQUETA_ESTADO_TORNEO[t.estado]}</Insignia>
        <span className="rounded-full bg-white/10 px-2.5 py-0.5 text-xs font-semibold">
          {ETIQUETA_FORMATO[t.formato]}
        </span>
      </div>
      <div>
        <h2 className="group-hover:text-volt-vivo text-4xl leading-[0.95] font-extrabold uppercase transition-colors duration-200">
          {t.nombre}
        </h2>
        <p className="mt-2 text-white/60">
          {t.sedeNombre} · Fútbol {t.jugadoresPorEquipo}
        </p>
      </div>
      <div className="mt-auto flex flex-col gap-3 text-sm">
        <div>
          <div className="flex items-baseline justify-between gap-2">
            <span className="flex items-center gap-2 text-white/70">
              <Users className="size-4" aria-hidden />
              Equipos
            </span>
            <span className="font-titulo text-xl font-extrabold">
              {t.equiposInscritos}
              {t.cupoEquipos ? <span className="text-white/45"> / {t.cupoEquipos}</span> : ''}
            </span>
          </div>
          {lleno != null && (
            <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-white/10">
              <div className="bg-volt h-full rounded-full" style={{ width: `${lleno}%` }} />
            </div>
          )}
        </div>
        {t.fechaInicio && (
          <p className="flex items-center gap-2 text-white/70 capitalize">
            <CalendarDays className="size-4" aria-hidden />
            {formatearDia(t.fechaInicio)}
            {t.fechaFin ? ` → ${formatearDia(t.fechaFin)}` : ''}
          </p>
        )}
        {t.valorInscripcion > 0 && (
          <p className="flex items-center gap-2 text-white/70">
            <Wallet className="size-4" aria-hidden />
            Inscripción {formatearCOP(t.valorInscripcion)}
          </p>
        )}
      </div>
      {t.campeon && (
        <p className="bg-volt text-noche -mx-6 -mb-6 flex items-center gap-2 px-6 py-3 font-semibold">
          <Trophy className="size-4" aria-hidden />
          Campeón: {t.campeon}
        </p>
      )}
    </Link>
  )
}

/** Torneos públicos agrupados en activos, próximos y finalizados. */
export function PaginaTorneos() {
  const { data, isPending, isError, error } = useTorneos(false)
  const [grupo, setGrupo] = useState('activos')
  const actual = GRUPOS.find((g) => g.id === grupo)!
  const lista = data?.filter((t) => actual.estados.includes(t.estado)) ?? []

  return (
    <div className="flex flex-col gap-8">
      <PortadaPagina titulo="Torneos" imagen="/img/arco-niebla.webp" compacta>
        <p className="max-w-xl text-lg">
          Ligas y copas de nuestras sedes: fixture, posiciones, goleadores y resultados.
        </p>
      </PortadaPagina>
      <Pestanas
        etiqueta="Estado del torneo"
        pestanas={GRUPOS.map((g) => ({
          id: g.id,
          texto: g.texto,
          conteo: data?.filter((t) => g.estados.includes(t.estado)).length ?? 0,
        }))}
        actual={grupo}
        alCambiar={setGrupo}
      />
      {isPending && <Cargando texto="Cargando torneos…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && lista.length === 0 && (
        <EstadoVacio icono={Trophy} titulo="No hay torneos en esta categoría" />
      )}
      <ul className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {lista.map((t, i) => (
          <Revelar como="li" key={t.id} i={i}>
            <TarjetaTorneo t={t} ruta={`/torneos/${t.id}`} />
          </Revelar>
        ))}
      </ul>
    </div>
  )
}
