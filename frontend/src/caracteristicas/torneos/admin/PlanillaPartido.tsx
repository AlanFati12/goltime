import { Trash2 } from 'lucide-react'
import { useState } from 'react'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Plegable } from '@/componentes/ui/Plegable'
import { Tarjeta, TituloTarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { cn } from '@/lib/utils'

import { useAccionesPartido, usePlantillaTorneo } from '../consultas'
import { ETIQUETA_EVENTO, ICONO_EVENTO } from '../etiquetas'
import type { DetallePartido, EventoPartido, JugadorEquipo, Lado, TipoEvento } from '../tipos'
import { golesDePlanilla, nombreLado } from '../utilidades'

const error = (e: unknown) => avisar.error(mensajeDeError(e))

const ACCIONES_RAPIDAS: { tipo: TipoEvento; texto: string; clase: string }[] = [
  { tipo: 'GOL', texto: 'Gol', clase: 'hover:bg-primario-claro hover:text-primario-oscuro' },
  { tipo: 'ASISTENCIA', texto: 'Asist.', clase: 'hover:bg-blue-50 hover:text-info' },
  { tipo: 'AMARILLA', texto: '🟨', clase: 'hover:bg-amber-50' },
  { tipo: 'ROJA', texto: '🟥', clase: 'hover:bg-red-50' },
]

const contar = (eventos: EventoPartido[], jugadorId: number, tipos: TipoEvento[]) =>
  eventos.filter((e) => e.jugadorId === jugadorId && tipos.includes(e.tipo)).length

function ColumnaEquipo({
  lado,
  torneoId,
  detalle,
  ocupado,
  alRegistrar,
}: {
  lado: Lado | null
  torneoId: number
  detalle: DetallePartido
  ocupado: boolean
  alRegistrar: (jugador: JugadorEquipo, inscripcionId: number, tipo: TipoEvento) => void
}) {
  const inscripcionId = lado?.inscripcionId ?? null
  const plantilla = usePlantillaTorneo(torneoId, inscripcionId)
  const eventos = detalle.eventos.filter((e) => e.inscripcionId === inscripcionId)

  return (
    <Tarjeta className="flex flex-col gap-3 p-4 sm:p-5">
      <h3 className="flex items-center gap-2 text-base font-semibold">
        <span
          className="ring-borde size-4 shrink-0 rounded ring-1"
          style={{ backgroundColor: lado?.color ?? 'transparent' }}
          aria-hidden
        />
        {nombreLado(lado)}
      </h3>
      {plantilla.isPending && <Cargando texto="Cargando jugadores…" className="py-4" />}
      {plantilla.data?.length === 0 && (
        <p className="text-texto-suave text-sm">
          Este equipo no tiene jugadores en el torneo. Agrégalos en la plantilla del torneo.
        </p>
      )}
      <ul className="divide-borde/70 flex flex-col divide-y">
        {plantilla.data?.map((j) => {
          const goles = contar(eventos, j.id, ['GOL', 'GOL_PENAL'])
          const asistencias = contar(eventos, j.id, ['ASISTENCIA'])
          const amarillas = contar(eventos, j.id, ['AMARILLA'])
          const rojas = contar(eventos, j.id, ['ROJA'])
          return (
            <li key={j.id} className="flex flex-wrap items-center gap-2 py-2">
              <span className="bg-tenue text-texto-suave inline-flex size-7 shrink-0 items-center justify-center rounded-full text-xs font-semibold tabular-nums">
                {j.dorsal ?? '–'}
              </span>
              <span className="min-w-0 flex-1 text-sm">
                <span className="block truncate font-medium">{j.nombreCompleto}</span>
                {(goles || asistencias || amarillas || rojas) > 0 && (
                  <span className="text-texto-suave flex gap-2 text-xs">
                    {goles > 0 && <span>⚽ {goles}</span>}
                    {asistencias > 0 && <span>👟 {asistencias}</span>}
                    {amarillas > 0 && <span>🟨 {amarillas}</span>}
                    {rojas > 0 && <span>🟥 {rojas}</span>}
                  </span>
                )}
              </span>
              <span className="flex gap-1">
                {ACCIONES_RAPIDAS.map((a) => (
                  <button
                    key={a.tipo}
                    type="button"
                    disabled={ocupado || inscripcionId === null}
                    onClick={() => alRegistrar(j, inscripcionId!, a.tipo)}
                    aria-label={`${ETIQUETA_EVENTO[a.tipo]} de ${j.nombreCompleto}`}
                    title={`${ETIQUETA_EVENTO[a.tipo]} de ${j.nombreCompleto}`}
                    className={cn(
                      'border-borde rounded-md border px-2 py-1 text-xs font-medium transition-colors disabled:opacity-50',
                      a.clase,
                    )}
                  >
                    {a.texto}
                  </button>
                ))}
              </span>
            </li>
          )
        })}
      </ul>
    </Tarjeta>
  )
}

/** Formulario completo para los eventos menos comunes: gol de penal, autogol, falta o una asistencia ligada a un gol concreto. */
function OtroEvento({
  detalle,
  torneoId,
  minuto,
}: {
  detalle: DetallePartido
  torneoId: number
  minuto: number | null
}) {
  const p = detalle.partido
  const acciones = useAccionesPartido(p.id)
  const [lado, setLado] = useState<'LOCAL' | 'VISITANTE'>('LOCAL')
  const inscripcionId = (lado === 'LOCAL' ? p.local : p.visitante)?.inscripcionId ?? null
  const plantilla = usePlantillaTorneo(torneoId, inscripcionId)
  const [jugador, setJugador] = useState('')
  const [tipo, setTipo] = useState<TipoEvento>('GOL_PENAL')
  const [gol, setGol] = useState('')
  const golesDelEquipo = detalle.eventos.filter(
    (e) => e.tipo === 'GOL' && e.inscripcionId === inscripcionId,
  )

  return (
    <div className="flex flex-col gap-3 pt-1">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <CampoSelect
          etiqueta="Equipo"
          value={lado}
          onChange={(e) => {
            setLado(e.target.value as 'LOCAL' | 'VISITANTE')
            setJugador('')
            setGol('')
          }}
        >
          <option value="LOCAL">{nombreLado(p.local)}</option>
          <option value="VISITANTE">{nombreLado(p.visitante)}</option>
        </CampoSelect>
        <CampoSelect
          etiqueta="Jugador"
          value={jugador}
          onChange={(e) => setJugador(e.target.value)}
        >
          <option value="">Elige…</option>
          {plantilla.data?.map((j) => (
            <option key={j.id} value={j.id}>
              {j.dorsal ? `${j.dorsal} · ` : ''}
              {j.nombreCompleto}
            </option>
          ))}
        </CampoSelect>
        <CampoSelect
          etiqueta="Evento"
          value={tipo}
          onChange={(e) => setTipo(e.target.value as TipoEvento)}
        >
          {(Object.keys(ETIQUETA_EVENTO) as TipoEvento[]).map((t) => (
            <option key={t} value={t}>
              {ETIQUETA_EVENTO[t]}
            </option>
          ))}
        </CampoSelect>
        {tipo === 'ASISTENCIA' && (
          <CampoSelect etiqueta="Gol asistido" value={gol} onChange={(e) => setGol(e.target.value)}>
            <option value="">Sin ligar</option>
            {golesDelEquipo.map((g) => (
              <option key={g.id} value={g.id}>
                {g.minuto != null ? `${g.minuto}' ` : ''}
                {g.jugador}
              </option>
            ))}
          </CampoSelect>
        )}
      </div>
      <p className="text-texto-suave text-xs">
        Un autogol se registra al jugador que lo hizo y suma al equipo rival.
      </p>
      <div className="flex justify-end">
        <Boton
          tamano="sm"
          disabled={!inscripcionId || !jugador || acciones.agregarEvento.isPending}
          onClick={() =>
            acciones.agregarEvento.mutate(
              {
                inscripcionId: inscripcionId!,
                jugadorId: Number(jugador),
                tipo,
                minuto,
                eventoRelacionadoId: tipo === 'ASISTENCIA' && gol ? Number(gol) : null,
              },
              { onSuccess: () => setGol(''), onError: error },
            )
          }
        >
          Agregar evento
        </Boton>
      </div>
    </div>
  )
}

/**
 * Planilla del partido: un clic por jugador registra gol, asistencia o tarjeta. Si el partido ya tiene resultado y el gol
 * lo supera, se ofrece actualizar el marcador. La lista de abajo es la línea de tiempo, con opción de borrar cada evento.
 */
export function PlanillaPartido({
  detalle,
  torneoId,
  alPedirResultado,
}: {
  detalle: DetallePartido
  torneoId: number
  /** Abre el registro del resultado con el marcador de la planilla. */
  alPedirResultado: () => void
}) {
  const p = detalle.partido
  const acciones = useAccionesPartido(p.id)
  const [minuto, setMinuto] = useState('')
  const minutoNumero = minuto === '' ? null : Number(minuto)
  const [gl, gv] = golesDePlanilla(detalle)
  const jugado = p.golesLocal != null && p.golesVisitante != null
  const coincide = jugado && gl === p.golesLocal && gv === p.golesVisitante
  const ocupado =
    acciones.agregarEvento.isPending ||
    acciones.eliminarEvento.isPending ||
    acciones.resultado.isPending

  const registrar = async (jugador: JugadorEquipo, inscripcionId: number, tipo: TipoEvento) => {
    const esLocal = inscripcionId === p.local?.inscripcionId
    // Una asistencia rápida se liga al último gol del mismo equipo que no tenga asistencia y no sea del mismo jugador.
    const ligado =
      tipo === 'ASISTENCIA'
        ? [...detalle.eventos]
            .reverse()
            .find(
              (e) =>
                e.tipo === 'GOL' &&
                e.inscripcionId === inscripcionId &&
                e.jugadorId !== jugador.id &&
                !detalle.eventos.some((x) => x.eventoRelacionadoId === e.id),
            )
        : undefined
    try {
      if (tipo === 'GOL' && jugado) {
        const nuevoLocal = esLocal ? gl + 1 : gl
        const nuevoVisitante = esLocal ? gv : gv + 1
        if (nuevoLocal > p.golesLocal! || nuevoVisitante > p.golesVisitante!) {
          const ok = window.confirm(
            `Con este gol la planilla queda ${nuevoLocal} - ${nuevoVisitante} y el marcador es ${p.golesLocal} - ${p.golesVisitante}. ¿Actualizar el marcador a ${nuevoLocal} - ${nuevoVisitante}?`,
          )
          if (!ok) return
          await acciones.resultado.mutateAsync({
            golesLocal: nuevoLocal,
            golesVisitante: nuevoVisitante,
            penalesLocal: nuevoLocal === nuevoVisitante ? (p.penalesLocal ?? null) : null,
            penalesVisitante: nuevoLocal === nuevoVisitante ? (p.penalesVisitante ?? null) : null,
            figuraJugadorId: p.figuraJugadorId ?? null,
          })
        }
      }
      await acciones.agregarEvento.mutateAsync({
        inscripcionId,
        jugadorId: jugador.id,
        tipo,
        minuto: minutoNumero,
        eventoRelacionadoId: ligado?.id ?? null,
      })
      avisar.exito(`${ETIQUETA_EVENTO[tipo]} de ${jugador.nombreCompleto} registrado.`)
    } catch (e) {
      error(e)
    }
  }

  return (
    <section aria-labelledby="titulo-planilla" className="flex flex-col gap-4">
      <Tarjeta className="flex flex-col gap-4">
        <TituloTarjeta
          className="mb-0"
          titulo={<span id="titulo-planilla">Planilla del partido</span>}
          descripcion="Toca el botón junto a cada jugador para registrar su gol, asistencia o tarjeta. El minuto es opcional."
        />
        <div className="flex flex-wrap items-end justify-between gap-4">
          <Campo
            className="w-40"
            etiqueta="Minuto (opcional)"
            type="number"
            min={0}
            max={130}
            value={minuto}
            onChange={(e) => setMinuto(e.target.value.slice(0, 3))}
          />
          <div className="flex flex-wrap items-center gap-3 text-sm">
            <span className="text-texto-suave">
              Goles en la planilla:{' '}
              <strong className="text-texto font-titulo tabular-nums">
                {gl} - {gv}
              </strong>
            </span>
            {jugado && !coincide && (
              <span className="text-alerta text-xs font-medium">
                (marcador {p.golesLocal} - {p.golesVisitante})
              </span>
            )}
            {(!jugado || !coincide) && (
              <Boton tamano="sm" variante="secundario" onClick={alPedirResultado}>
                {jugado ? 'Ajustar marcador' : 'Guardar como resultado'}
              </Boton>
            )}
          </div>
        </div>
      </Tarjeta>

      <div className="grid gap-4 lg:grid-cols-2">
        <ColumnaEquipo
          lado={p.local}
          torneoId={torneoId}
          detalle={detalle}
          ocupado={ocupado}
          alRegistrar={registrar}
        />
        <ColumnaEquipo
          lado={p.visitante}
          torneoId={torneoId}
          detalle={detalle}
          ocupado={ocupado}
          alRegistrar={registrar}
        />
      </div>

      <Tarjeta className="flex flex-col gap-3">
        <TituloTarjeta
          className="mb-0"
          titulo={`Eventos registrados (${detalle.eventos.length})`}
        />
        {detalle.eventos.length === 0 ? (
          <p className="text-texto-suave py-2 text-sm">Todavía no hay eventos.</p>
        ) : (
          <ol className="divide-borde/70 flex flex-col divide-y text-sm">
            {detalle.eventos.map((e) => {
              const local = e.inscripcionId === p.local?.inscripcionId
              return (
                <li key={e.id} className="flex items-center gap-3 py-2">
                  <span className="bg-tenue w-12 shrink-0 rounded-full py-0.5 text-center text-xs font-semibold tabular-nums">
                    {e.minuto != null ? `${e.minuto}'` : '—'}
                  </span>
                  <span aria-hidden>{ICONO_EVENTO[e.tipo]}</span>
                  <span className="min-w-0 flex-1">
                    <span className="font-medium">{e.jugador}</span>
                    <span className="text-texto-suave">
                      {' '}
                      · {ETIQUETA_EVENTO[e.tipo]} · {e.equipo}
                      {e.eventoRelacionadoId != null && ' · ligada a un gol'}
                    </span>
                  </span>
                  <span
                    className={cn(
                      'hidden rounded-full px-2 text-xs sm:inline',
                      local ? 'bg-primario-claro text-primario-oscuro' : 'text-info bg-blue-50',
                    )}
                  >
                    {local ? 'Local' : 'Visitante'}
                  </span>
                  <Boton
                    tamano="icono"
                    variante="fantasma"
                    className="hover:text-peligro"
                    aria-label={`Eliminar ${ETIQUETA_EVENTO[e.tipo].toLowerCase()} de ${e.jugador}`}
                    disabled={ocupado}
                    onClick={() => acciones.eliminarEvento.mutate(e.id, { onError: error })}
                  >
                    <Trash2 aria-hidden />
                  </Boton>
                </li>
              )
            })}
          </ol>
        )}
        <Plegable titulo="Otros eventos: gol de penal, autogol, falta o asistencia a un gol concreto">
          <OtroEvento detalle={detalle} torneoId={torneoId} minuto={minutoNumero} />
        </Plegable>
      </Tarjeta>
    </section>
  )
}
