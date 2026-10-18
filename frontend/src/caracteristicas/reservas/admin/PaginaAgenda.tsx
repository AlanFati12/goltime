import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { useSedesAdmin } from '@/caracteristicas/sedes/consultas'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import {
  aIsoBogota,
  fechaEnBogota,
  formatearDia,
  formatearHora,
  hoyEnBogota,
  lunesDeLaSemana,
  sumarDias,
} from '@/lib/formato'
import { cn } from '@/lib/utils'

import type { ElementoAgenda } from '../tipos'
import { useAgenda } from './consultas'

const PX_POR_HORA = 56
const COLORES: Record<string, string> = {
  PENDIENTE: 'border-alerta bg-alerta/15 text-texto',
  CONFIRMADA: 'border-primario bg-primario-claro text-primario-oscuro',
  COMPLETADA: 'border-borde bg-borde/60 text-texto-suave',
  NO_ASISTIO: 'border-peligro bg-peligro/10 text-peligro',
  BLOQUEO: 'border-texto-suave bg-texto-suave/20 text-texto',
}

interface Cuadro {
  elemento: ElementoAgenda
  desde: number
  hasta: number
}

/** Los elementos que tocan un día, recortados a ese día, en minutos desde la medianoche de Bogotá. */
function cuadrosDelDia(elementos: ElementoAgenda[], fecha: string): Cuadro[] {
  const inicioDia = new Date(aIsoBogota(fecha, '00:00')).getTime()
  const finDia = new Date(aIsoBogota(fecha, '24:00')).getTime()
  return elementos
    .filter((e) => new Date(e.inicio).getTime() < finDia && new Date(e.fin).getTime() > inicioDia)
    .map((e) => ({
      elemento: e,
      desde: Math.max(0, (new Date(e.inicio).getTime() - inicioDia) / 60000),
      hasta: Math.min(1440, (new Date(e.fin).getTime() - inicioDia) / 60000),
    }))
}

function Etiqueta({ c }: { c: Cuadro }) {
  const e = c.elemento
  return (
    <>
      <span className="block font-medium">
        {e.tipo === 'BLOQUEO' ? 'Bloqueado' : (e.cliente ?? 'Reserva')}
      </span>
      <span className="block truncate">
        {formatearHora(e.inicio)}–{formatearHora(e.fin)}
        {e.tipo === 'RESERVA' && e.canchaReservaNombre ? ` · por ${e.canchaReservaNombre}` : ''}
      </span>
      {e.tipo === 'BLOQUEO' && e.motivoBloqueo && (
        <span className="block truncate">{e.motivoBloqueo}</span>
      )}
      {e.tipo === 'RESERVA' && e.estadoPago && e.estadoPago !== 'PENDIENTE' && (
        <span className="block truncate">{e.estadoPago.toLowerCase()}</span>
      )}
    </>
  )
}

export function PaginaAgenda() {
  const navegar = useNavigate()
  const sedes = useSedesAdmin()
  const [sedeElegida, setSedeElegida] = useState<number | null>(null)
  const [fecha, setFecha] = useState(hoyEnBogota())
  const [vista, setVista] = useState<'dia' | 'semana'>('dia')
  const [canchaSemana, setCanchaSemana] = useState<number | null>(null)

  const sedeId = sedeElegida ?? sedes.data?.[0]?.id ?? null
  const sede = sedes.data?.find((s) => s.id === sedeId)
  const desde = vista === 'dia' ? fecha : lunesDeLaSemana(fecha)
  const hasta = vista === 'dia' ? fecha : sumarDias(desde, 6)
  const agenda = useAgenda(sedeId, desde, hasta)

  const dias = vista === 'dia' ? [fecha] : Array.from({ length: 7 }, (_, i) => sumarDias(desde, i))
  const canchaDeLaSemana = canchaSemana ?? agenda.data?.canchas[0]?.id ?? null

  // Rango de horas visible: el horario de la sede en los días mostrados (o 6:00–24:00 si no hay).
  const [inicioHoras, finHoras] = useMemo(() => {
    const abiertos =
      sede?.horario
        .filter((h) => !h.cerrado && h.apertura && h.cierre)
        .filter((h) =>
          dias.some((d) => (new Date(`${d}T12:00:00Z`).getUTCDay() + 6) % 7 === h.diaSemana - 1),
        ) ?? []
    if (abiertos.length === 0) return [6, 24]
    const min = Math.min(...abiertos.map((h) => Number(h.apertura!.slice(0, 2))))
    const max = Math.max(
      ...abiertos.map((h) =>
        Math.ceil(Number(h.cierre!.slice(0, 2)) + Number(h.cierre!.slice(3)) / 60),
      ),
    )
    return [Math.max(0, min - 1), Math.min(24, max + 1)]
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sede, desde, vista])

  const horas = Array.from({ length: finHoras - inicioHoras }, (_, i) => inicioHoras + i)
  const alto = (finHoras - inicioHoras) * PX_POR_HORA

  const columnas =
    vista === 'dia'
      ? (agenda.data?.canchas.map((c) => ({
          clave: String(c.id),
          titulo: c.nombre,
          subtitulo: c.modalidad + (c.estado !== 'ACTIVA' ? ' · fuera de servicio' : ''),
          canchaId: c.id,
          fecha,
        })) ?? [])
      : dias.map((d) => ({
          clave: d,
          titulo: formatearDia(d),
          subtitulo: '',
          canchaId: canchaDeLaSemana,
          fecha: d,
        }))

  const nuevaReserva = (
    canchaId: number | null,
    dia: string,
    evento: React.MouseEvent<HTMLDivElement>,
  ) => {
    if (!canchaId || !sedeId) return
    const caja = evento.currentTarget.getBoundingClientRect()
    const minuto = Math.floor(((evento.clientY - caja.top) / PX_POR_HORA + inicioHoras) * 60)
    const hora = Math.floor(minuto / 60)
    navegar(
      `/admin/reservas/nueva?sedeId=${sedeId}&canchaId=${canchaId}&fecha=${dia}&hora=${String(hora).padStart(2, '0')}:00`,
    )
  }

  const cambiarFecha = (dias: number) => setFecha((f) => sumarDias(f, dias))
  const paso = vista === 'dia' ? 1 : 7

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-bold">Agenda</h1>
        <div role="tablist" aria-label="Vista" className="flex gap-1">
          {(['dia', 'semana'] as const).map((v) => (
            <Boton
              key={v}
              variante={vista === v ? 'primario' : 'secundario'}
              tamano="sm"
              role="tab"
              aria-selected={vista === v}
              onClick={() => setVista(v)}
            >
              {v === 'dia' ? 'Día' : 'Semana'}
            </Boton>
          ))}
        </div>
      </div>

      <Tarjeta className="flex flex-wrap items-end gap-3">
        <CampoSelect
          className="min-w-48"
          etiqueta="Sede"
          value={sedeId ?? ''}
          onChange={(e) => (setSedeElegida(Number(e.target.value)), setCanchaSemana(null))}
        >
          {sedes.data?.map((s) => (
            <option key={s.id} value={s.id}>
              {s.nombre}
            </option>
          ))}
        </CampoSelect>
        {vista === 'semana' && (
          <CampoSelect
            className="min-w-40"
            etiqueta="Cancha"
            value={canchaDeLaSemana ?? ''}
            onChange={(e) => setCanchaSemana(Number(e.target.value))}
          >
            {agenda.data?.canchas.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
          </CampoSelect>
        )}
        <div className="flex items-end gap-1">
          <Boton
            variante="secundario"
            aria-label={vista === 'dia' ? 'Día anterior' : 'Semana anterior'}
            onClick={() => cambiarFecha(-paso)}
          >
            ←
          </Boton>
          <Campo
            className="w-40"
            etiqueta="Fecha"
            type="date"
            value={fecha}
            onChange={(e) => e.target.value && setFecha(e.target.value)}
          />
          <Boton
            variante="secundario"
            aria-label={vista === 'dia' ? 'Día siguiente' : 'Semana siguiente'}
            onClick={() => cambiarFecha(paso)}
          >
            →
          </Boton>
          <Boton variante="fantasma" onClick={() => setFecha(hoyEnBogota())}>
            Hoy
          </Boton>
        </div>
        <div className="text-texto-suave ml-auto flex flex-wrap gap-3 text-xs" aria-hidden>
          <span className="flex items-center gap-1">
            <span className="border-alerta bg-alerta/15 size-3 rounded-sm border" /> Pendiente
          </span>
          <span className="flex items-center gap-1">
            <span className="border-primario bg-primario-claro size-3 rounded-sm border" />{' '}
            Confirmada
          </span>
          <span className="flex items-center gap-1">
            <span className="border-texto-suave bg-texto-suave/20 size-3 rounded-sm border" />{' '}
            Bloqueo
          </span>
        </div>
      </Tarjeta>

      {agenda.isPending && sedeId !== null && <Cargando texto="Cargando agenda…" />}
      {agenda.isError && <Alerta tipo="error">{mensajeDeError(agenda.error)}</Alerta>}
      {sedes.data?.length === 0 && (
        <Tarjeta className="text-texto-suave text-center text-sm">
          Primero crea una sede con canchas.
        </Tarjeta>
      )}
      {agenda.data?.canchas.length === 0 && (
        <Tarjeta className="text-texto-suave text-center text-sm">
          Esta sede no tiene canchas.
        </Tarjeta>
      )}

      {agenda.data && columnas.length > 0 && (
        <Tarjeta className={cn('overflow-x-auto p-0', agenda.isPlaceholderData && 'opacity-60')}>
          <div className="flex min-w-max">
            <div className="w-14 shrink-0 border-r" style={{ borderColor: 'var(--color-borde)' }}>
              <div className="h-12" />
              <div className="relative" style={{ height: alto }}>
                {horas.map((h) => (
                  <span
                    key={h}
                    className="text-texto-suave absolute right-2 -translate-y-1/2 text-xs"
                    style={{ top: (h - inicioHoras) * PX_POR_HORA }}
                  >
                    {String(h).padStart(2, '0')}:00
                  </span>
                ))}
              </div>
            </div>
            {columnas.map((col) => {
              const cuadros = cuadrosDelDia(
                agenda.data.elementos
                  .filter((e) => vista === 'dia' || e.canchaId === col.canchaId)
                  .filter((e) => vista === 'semana' || e.canchaId === col.canchaId),
                col.fecha,
              )
              return (
                <div
                  key={col.clave}
                  className="border-borde w-44 shrink-0 border-r last:border-r-0"
                >
                  <div className="border-borde flex h-12 flex-col justify-center border-b px-2 text-center text-sm font-medium capitalize">
                    {col.titulo}
                    {col.subtitulo && (
                      <span className="text-texto-suave text-xs font-normal normal-case">
                        {col.subtitulo}
                      </span>
                    )}
                  </div>
                  <div
                    className="relative cursor-cell"
                    style={{
                      height: alto,
                      backgroundImage:
                        'repeating-linear-gradient(to bottom, transparent 0, transparent calc(56px - 1px), var(--color-borde) calc(56px - 1px), var(--color-borde) 56px)',
                    }}
                    onClick={(e) =>
                      e.target === e.currentTarget && nuevaReserva(col.canchaId, col.fecha, e)
                    }
                    role="presentation"
                  >
                    {cuadros.map((c) => {
                      const e = c.elemento
                      const estilo = {
                        top: ((c.desde - inicioHoras * 60) / 60) * PX_POR_HORA,
                        height: Math.max(((c.hasta - c.desde) / 60) * PX_POR_HORA - 2, 18),
                      }
                      const clases = cn(
                        'absolute inset-x-1 overflow-hidden rounded-boton border-l-4 px-1.5 py-1 text-xs',
                        COLORES[e.tipo === 'BLOQUEO' ? 'BLOQUEO' : (e.estado ?? 'CONFIRMADA')],
                      )
                      return e.tipo === 'RESERVA' && e.reservaId ? (
                        <Link
                          key={`${e.reservaId}-${e.canchaId}-${e.inicio}`}
                          to={`/admin/reservas/${e.reservaId}`}
                          className={clases}
                          style={estilo}
                          title={`${e.codigo} · ${e.cliente}`}
                        >
                          <Etiqueta c={c} />
                        </Link>
                      ) : (
                        <Link
                          key={`b${e.bloqueoId}-${e.canchaId}-${e.inicio}`}
                          to="/admin/bloqueos"
                          className={clases}
                          style={estilo}
                          title={e.motivoBloqueo ?? 'Bloqueo'}
                        >
                          <Etiqueta c={c} />
                        </Link>
                      )
                    })}
                  </div>
                </div>
              )
            })}
          </div>
        </Tarjeta>
      )}
      <p className="text-texto-suave text-xs">
        Toca un espacio vacío para crear una reserva a esa hora. Fecha mostrada:{' '}
        {fechaEnBogota(aIsoBogota(fecha, '12:00'))}.
      </p>
    </div>
  )
}
