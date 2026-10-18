import { useState } from 'react'
import { CalendarDays, Trophy, Users, Wallet } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { Pestanas } from '@/componentes/ui/Pestanas'
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
    <Link to={ruta} className="group block h-full">
      <Tarjeta className="flex h-full flex-col gap-3 transition-all group-hover:shadow-md">
        <div className="flex items-start justify-between gap-3">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
            <Trophy className="size-5" aria-hidden />
          </span>
          <div className="flex flex-wrap justify-end gap-1.5">
            <Insignia tono={TONO_ESTADO_TORNEO[t.estado]}>
              {ETIQUETA_ESTADO_TORNEO[t.estado]}
            </Insignia>
            <Insignia>{ETIQUETA_FORMATO[t.formato]}</Insignia>
          </div>
        </div>
        <div>
          <h2 className="group-hover:text-primario text-base font-semibold">{t.nombre}</h2>
          <p className="text-texto-suave mt-0.5 text-sm">
            {t.sedeNombre} · Fútbol {t.jugadoresPorEquipo}
          </p>
        </div>
        <ul className="text-texto-suave mt-auto flex flex-col gap-1.5 text-sm">
          <li className="flex items-center gap-2">
            <Users className="size-4" aria-hidden />
            <span>
              {t.equiposInscritos}
              {t.cupoEquipos ? ` de ${t.cupoEquipos}` : ''} equipos
            </span>
          </li>
          {t.fechaInicio && (
            <li className="flex items-center gap-2">
              <CalendarDays className="size-4" aria-hidden />
              <span className="capitalize">
                {formatearDia(t.fechaInicio)}
                {t.fechaFin ? ` → ${formatearDia(t.fechaFin)}` : ''}
              </span>
            </li>
          )}
          {t.valorInscripcion > 0 && (
            <li className="flex items-center gap-2">
              <Wallet className="size-4" aria-hidden />
              <span>Inscripción {formatearCOP(t.valorInscripcion)}</span>
            </li>
          )}
        </ul>
        {t.campeon && (
          <p className="border-borde border-t pt-3 text-sm font-medium">🏆 Campeón: {t.campeon}</p>
        )}
      </Tarjeta>
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
    <div className="flex flex-col gap-6">
      <EncabezadoPagina titulo="Torneos" descripcion="Sigue las ligas y copas de nuestras sedes." />
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
