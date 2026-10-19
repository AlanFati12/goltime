import { useState } from 'react'

import { useSedesAdmin } from '@/caracteristicas/sedes/consultas'
import { DIAS_CORTOS, HORAS } from '@/caracteristicas/sedes/constantes'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, CampoTexto, Casilla } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Modal } from '@/componentes/ui/Modal'
import { codigoDeError, mensajeDeError } from '@/lib/api'
import {
  aIsoBogota,
  fechaEnBogota,
  formatearCOP,
  formatearFechaHora,
  hoyEnBogota,
  minutoDelDia,
  sumarDias,
} from '@/lib/formato'
import { cn } from '@/lib/utils'

import type { Bloqueo, MotivoBloqueo, PrevisualizacionBloqueo } from '../tipos'
import { useAccionesBloqueo, type DatosBloqueo } from './consultas'
import { rangoHorario } from '../utilidades'

export const ETIQUETA_MOTIVO: Record<MotivoBloqueo, string> = {
  MANTENIMIENTO: 'Mantenimiento',
  EVENTO_PRIVADO: 'Evento privado',
  TORNEO: 'Torneo',
  CLIMA: 'Clima',
  OTRO: 'Otro',
}

function horaDe(iso: string | null) {
  if (!iso) return '08:00'
  const m = minutoDelDia(iso)
  return m === 0
    ? '00:00'
    : `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`
}

/**
 * Crear o editar un bloqueo con el flujo de conflictos: se previsualizan las reservas afectadas y, si hay,
 * hay que confirmar explícitamente esa lista (con su huella). Si cambia entre la revisión y la confirmación,
 * se vuelve a mostrar.
 */
export function FormularioBloqueo({
  bloqueo,
  alCerrar,
}: {
  bloqueo: Bloqueo | null
  alCerrar: () => void
}) {
  const sedes = useSedesAdmin()
  const { previsualizar, guardar } = useAccionesBloqueo()

  const [sedeId, setSedeId] = useState<number | null>(bloqueo?.sedeId ?? null)
  const [todaLaSede, setTodaLaSede] = useState(bloqueo ? bloqueo.canchaIds.length === 0 : true)
  const [canchaIds, setCanchaIds] = useState<number[]>(bloqueo?.canchaIds ?? [])
  const [tipo, setTipo] = useState<'PUNTUAL' | 'RECURRENTE'>(bloqueo?.tipo ?? 'PUNTUAL')
  const [fechaInicio, setFechaInicio] = useState(
    bloqueo?.inicio ? fechaEnBogota(bloqueo.inicio) : sumarDias(hoyEnBogota(), 1),
  )
  const [horaInicio, setHoraInicio] = useState(
    bloqueo?.inicio && !bloqueo.todoElDia ? horaDe(bloqueo.inicio) : '08:00',
  )
  const [fechaFin, setFechaFin] = useState(
    bloqueo?.fin
      ? fechaEnBogota(new Date(new Date(bloqueo.fin).getTime() - 1000))
      : sumarDias(hoyEnBogota(), 1),
  )
  const [horaFin, setHoraFin] = useState(
    bloqueo?.fin && !bloqueo.todoElDia ? horaDe(bloqueo.fin) || '24:00' : '12:00',
  )
  const [todoElDia, setTodoElDia] = useState(bloqueo?.todoElDia ?? false)
  const [dias, setDias] = useState<number[]>(bloqueo?.diasSemana ?? [])
  const [recHoraInicio, setRecHoraInicio] = useState(bloqueo?.horaInicio ?? '18:00')
  const [recHoraFin, setRecHoraFin] = useState(bloqueo?.horaFin ?? '20:00')
  const [fechaDesde, setFechaDesde] = useState(bloqueo?.fechaDesde ?? sumarDias(hoyEnBogota(), 1))
  const [fechaHasta, setFechaHasta] = useState(bloqueo?.fechaHasta ?? sumarDias(hoyEnBogota(), 30))
  const [motivo, setMotivo] = useState<MotivoBloqueo>(bloqueo?.motivo ?? 'MANTENIMIENTO')
  const [detalle, setDetalle] = useState(bloqueo?.detalle ?? '')
  const [error, setError] = useState<string | null>(null)
  const [revision, setRevision] = useState<{
    datos: DatosBloqueo
    previa: PrevisualizacionBloqueo
  } | null>(null)

  const sedeActual = sedeId ?? sedes.data?.[0]?.id ?? null
  const sede = sedes.data?.find((s) => s.id === sedeActual)

  const construir = (): DatosBloqueo | null => {
    if (!sedeActual) return null
    const base = {
      sedeId: sedeActual,
      canchaIds: todaLaSede ? [] : canchaIds,
      tipo,
      motivo,
      detalle: detalle.trim() || undefined,
    }
    if (tipo === 'PUNTUAL') {
      return {
        ...base,
        inicio: aIsoBogota(fechaInicio, todoElDia ? '00:00' : horaInicio),
        fin: aIsoBogota(fechaFin, todoElDia ? '24:00' : horaFin),
        todoElDia,
      }
    }
    return {
      ...base,
      diasSemana: dias,
      horaInicio: recHoraInicio,
      horaFin: recHoraFin,
      fechaDesde,
      fechaHasta,
    }
  }

  const valido =
    !!sedeActual &&
    (todaLaSede || canchaIds.length > 0) &&
    (tipo === 'PUNTUAL'
      ? !!fechaInicio &&
        !!fechaFin &&
        (todoElDia
          ? fechaFin >= fechaInicio
          : aIsoBogota(fechaFin, horaFin) > aIsoBogota(fechaInicio, horaInicio))
      : dias.length > 0 &&
        recHoraFin > recHoraInicio &&
        !!fechaDesde &&
        !!fechaHasta &&
        fechaHasta >= fechaDesde)

  const guardarConHuella = (datos: DatosBloqueo, huella?: string) =>
    guardar.mutate(
      { id: bloqueo?.id ?? null, datos, huellaConfirmada: huella },
      {
        onSuccess: () => {
          avisar.exito(bloqueo ? 'Bloqueo actualizado.' : 'Bloqueo creado.')
          alCerrar()
        },
        onError: async (e) => {
          if (codigoDeError(e) === 'CONFLICTOS_SIN_CONFIRMAR') {
            // La lista cambió desde que se revisó: se vuelve a mostrar actualizada.
            const nueva = await previsualizar.mutateAsync(datos).catch(() => null)
            if (nueva) setRevision({ datos, previa: nueva })
          }
          setError(mensajeDeError(e))
        },
      },
    )

  const revisar = () => {
    const datos = construir()
    if (!datos) return
    setError(null)
    previsualizar.mutate(datos, {
      onSuccess: (previa) =>
        previa.requiereConfirmacion ? setRevision({ datos, previa }) : guardarConHuella(datos),
      onError: (e) => setError(mensajeDeError(e)),
    })
  }

  if (revision) {
    const { datos, previa } = revision
    return (
      <Modal abierto titulo="Reservas afectadas por el bloqueo" alCerrar={() => setRevision(null)}>
        <div className="flex flex-col gap-4">
          <Alerta tipo="aviso">
            Estas <strong>{previa.conflictos.length} reservas</strong> serán canceladas y se
            notificará a los clientes. Las que tengan pagos quedarán con reembolso pendiente.
          </Alerta>
          <ul className="border-borde divide-borde rounded-boton max-h-72 divide-y overflow-y-auto border text-sm">
            {previa.conflictos.map((c) => (
              <li key={c.reservaId} className="px-3 py-2">
                <div className="flex justify-between gap-2">
                  <span className="cifras font-medium font-semibold tracking-wide">{c.codigo}</span>
                  <span>{formatearCOP(c.precioFinal)}</span>
                </div>
                <div>{c.cliente}</div>
                <div className="text-texto-suave text-xs capitalize">
                  {c.cancha} · {formatearFechaHora(c.inicio)} · {rangoHorario(c.inicio, c.fin)}
                </div>
                {c.conPago && (
                  <div className="text-alerta text-xs">
                    Tiene pagos registrados ({c.estadoPago.toLowerCase()})
                  </div>
                )}
              </li>
            ))}
          </ul>
          {error && <Alerta tipo="error">{error}</Alerta>}
          <div className="flex justify-end gap-2">
            <Boton variante="secundario" onClick={() => setRevision(null)}>
              Volver
            </Boton>
            <Boton
              variante="peligro"
              disabled={guardar.isPending}
              onClick={() => guardarConHuella(datos, previa.huella)}
            >
              {guardar.isPending
                ? 'Guardando…'
                : `Confirmar y cancelar ${previa.conflictos.length} reservas`}
            </Boton>
          </div>
        </div>
      </Modal>
    )
  }

  return (
    <Modal abierto titulo={bloqueo ? 'Editar bloqueo' : 'Nuevo bloqueo'} alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <CampoSelect
          etiqueta="Sede"
          value={sedeActual ?? ''}
          disabled={!!bloqueo}
          onChange={(e) => (setSedeId(Number(e.target.value)), setCanchaIds([]))}
        >
          {sedes.data?.map((s) => (
            <option key={s.id} value={s.id}>
              {s.nombre}
            </option>
          ))}
        </CampoSelect>

        <fieldset className="flex flex-col gap-2">
          <legend className="mb-1 text-sm font-medium">Canchas</legend>
          <Casilla
            etiqueta="Toda la sede"
            checked={todaLaSede}
            onChange={(e) => setTodaLaSede(e.target.checked)}
          />
          {!todaLaSede && (
            <div className="border-borde rounded-boton flex flex-col gap-1 border p-2">
              {sede?.canchas.map((c) => (
                <Casilla
                  key={c.id}
                  etiqueta={`${c.nombre} (${c.modalidad})`}
                  checked={canchaIds.includes(c.id)}
                  onChange={() =>
                    setCanchaIds((ids) =>
                      ids.includes(c.id) ? ids.filter((x) => x !== c.id) : [...ids, c.id],
                    )
                  }
                />
              ))}
              <p className="text-texto-suave text-xs">
                Si eliges una cancha compuesta, también se bloquean sus componentes.
              </p>
            </div>
          )}
        </fieldset>

        <div role="radiogroup" aria-label="Tipo de bloqueo" className="flex gap-2">
          {(['PUNTUAL', 'RECURRENTE'] as const).map((t) => (
            <button
              key={t}
              type="button"
              role="radio"
              aria-checked={tipo === t}
              disabled={!!bloqueo}
              onClick={() => setTipo(t)}
              className={cn(
                'rounded-boton border px-4 py-2 text-sm disabled:opacity-60',
                tipo === t
                  ? 'border-primario bg-primario text-sobre-primario'
                  : 'border-borde bg-superficie',
              )}
            >
              {t === 'PUNTUAL' ? 'Puntual' : 'Recurrente'}
            </button>
          ))}
        </div>

        {tipo === 'PUNTUAL' ? (
          <div className="flex flex-col gap-3">
            <Casilla
              etiqueta="Todo el día (días completos)"
              checked={todoElDia}
              onChange={(e) => setTodoElDia(e.target.checked)}
            />
            <div className="grid gap-3 sm:grid-cols-2">
              <Campo
                etiqueta="Desde (fecha)"
                type="date"
                value={fechaInicio}
                onChange={(e) => setFechaInicio(e.target.value)}
              />
              {!todoElDia && (
                <CampoSelect
                  etiqueta="Hora de inicio"
                  value={horaInicio}
                  onChange={(e) => setHoraInicio(e.target.value)}
                >
                  {HORAS.slice(0, 48).map((h) => (
                    <option key={h}>{h}</option>
                  ))}
                </CampoSelect>
              )}
              <Campo
                etiqueta="Hasta (fecha)"
                type="date"
                value={fechaFin}
                onChange={(e) => setFechaFin(e.target.value)}
              />
              {!todoElDia && (
                <CampoSelect
                  etiqueta="Hora de fin"
                  value={horaFin}
                  onChange={(e) => setHoraFin(e.target.value)}
                >
                  {HORAS.slice(1).map((h) => (
                    <option key={h}>{h}</option>
                  ))}
                </CampoSelect>
              )}
            </div>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            <fieldset>
              <legend className="mb-1 text-sm font-medium">Días de la semana</legend>
              <div className="flex flex-wrap gap-2">
                {DIAS_CORTOS.map((nombre, i) => {
                  const dia = i + 1
                  const activo = dias.includes(dia)
                  return (
                    <button
                      key={dia}
                      type="button"
                      aria-pressed={activo}
                      onClick={() =>
                        setDias((d) => (activo ? d.filter((x) => x !== dia) : [...d, dia]))
                      }
                      className={cn(
                        'rounded-boton border px-3 py-1 text-sm',
                        activo
                          ? 'border-primario bg-primario text-sobre-primario'
                          : 'border-borde bg-superficie',
                      )}
                    >
                      {nombre}
                    </button>
                  )
                })}
              </div>
            </fieldset>
            <div className="grid gap-3 sm:grid-cols-2">
              <CampoSelect
                etiqueta="Desde las"
                value={recHoraInicio}
                onChange={(e) => setRecHoraInicio(e.target.value)}
              >
                {HORAS.slice(0, 48).map((h) => (
                  <option key={h}>{h}</option>
                ))}
              </CampoSelect>
              <CampoSelect
                etiqueta="Hasta las"
                value={recHoraFin}
                onChange={(e) => setRecHoraFin(e.target.value)}
              >
                {HORAS.slice(1).map((h) => (
                  <option key={h}>{h}</option>
                ))}
              </CampoSelect>
              <Campo
                etiqueta="Desde el"
                type="date"
                value={fechaDesde}
                onChange={(e) => setFechaDesde(e.target.value)}
              />
              <Campo
                etiqueta="Hasta el"
                type="date"
                value={fechaHasta}
                onChange={(e) => setFechaHasta(e.target.value)}
                ayuda="Máximo un año."
              />
            </div>
          </div>
        )}

        <CampoSelect
          etiqueta="Motivo"
          value={motivo}
          onChange={(e) => setMotivo(e.target.value as MotivoBloqueo)}
        >
          {(Object.keys(ETIQUETA_MOTIVO) as MotivoBloqueo[]).map((m) => (
            <option key={m} value={m}>
              {ETIQUETA_MOTIVO[m]}
            </option>
          ))}
        </CampoSelect>
        <CampoTexto
          etiqueta="Nota (opcional, se muestra a los clientes afectados)"
          value={detalle}
          onChange={(e) => setDetalle(e.target.value)}
          maxLength={500}
        />

        {error && <Alerta tipo="error">{error}</Alerta>}
        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton
            onClick={revisar}
            disabled={!valido || previsualizar.isPending || guardar.isPending}
          >
            {previsualizar.isPending || guardar.isPending
              ? 'Revisando…'
              : bloqueo
                ? 'Revisar y guardar'
                : 'Revisar y crear'}
          </Boton>
        </div>
      </div>
    </Modal>
  )
}
