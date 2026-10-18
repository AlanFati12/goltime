import { Building2, ChevronLeft, ChevronRight, Flag } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { useSedesAdmin } from '@/caracteristicas/sedes/consultas'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { Segmentado } from '@/componentes/ui/Pestanas'
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
  PENDIENTE: 'border-amber-500 bg-amber-50 text-amber-900 hover:bg-amber-100',
  CONFIRMADA: 'border-primario bg-emerald-50 text-primario-oscuro hover:bg-emerald-100',
  COMPLETADA: 'border-slate-300 bg-tenue text-texto-suave hover:bg-slate-200',
  NO_ASISTIO: 'border-peligro bg-red-50 text-peligro hover:bg-red-100',
  BLOQUEO:
    'border-slate-500 bg-[repeating-linear-gradient(45deg,#f1f5f9_0,#f1f5f9_6px,#e2e8f0_6px,#e2e8f0_12px)] text-texto',
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
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Agenda"
        descripcion="Toca un espacio vacío para crear una reserva a esa hora."
        acciones={
          <Segmentado
            etiqueta="Vista"
            opciones={[
              { id: 'dia' as const, texto: 'Día' },
              { id: 'semana' as const, texto: 'Semana' },
            ]}
            actual={vista}
            alCambiar={setVista}
          />
        }
      />

      <Tarjeta className="flex flex-wrap items-end gap-3 p-4 sm:p-4">
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
            tamano="icono"
            className="size-10"
            aria-label={vista === 'dia' ? 'Día anterior' : 'Semana anterior'}
            onClick={() => cambiarFecha(-paso)}
          >
            <ChevronLeft aria-hidden />
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
            tamano="icono"
            className="size-10"
            aria-label={vista === 'dia' ? 'Día siguiente' : 'Semana siguiente'}
            onClick={() => cambiarFecha(paso)}
          >
            <ChevronRight aria-hidden />
          </Boton>
          <Boton variante="secundario" onClick={() => setFecha(hoyEnBogota())}>
            Hoy
          </Boton>
        </div>
        <div className="text-texto-suave ml-auto flex flex-wrap gap-3 text-xs" aria-hidden>
          <span className="flex items-center gap-1.5">
            <span className="size-3 rounded-sm border-l-4 border-amber-500 bg-amber-50" /> Pendiente
          </span>
          <span className="flex items-center gap-1.5">
            <span className="border-primario size-3 rounded-sm border-l-4 bg-emerald-50" />{' '}
            Confirmada
          </span>
          <span className="flex items-center gap-1.5">
            <span className="size-3 rounded-sm border-l-4 border-slate-500 bg-slate-200" /> Bloqueo
          </span>
        </div>
      </Tarjeta>

      {agenda.isPending && sedeId !== null && <Cargando texto="Cargando agenda…" />}
      {agenda.isError && <Alerta tipo="error">{mensajeDeError(agenda.error)}</Alerta>}
      {sedes.data?.length === 0 && (
        <EstadoVacio icono={Building2} titulo="Primero crea una sede con canchas" />
      )}
      {agenda.data?.canchas.length === 0 && (
        <EstadoVacio icono={Flag} titulo="Esta sede no tiene canchas" />
      )}

      {agenda.data && columnas.length > 0 && (
        <Tarjeta
          className={cn('overflow-x-auto p-0 sm:p-0', agenda.isPlaceholderData && 'opacity-60')}
        >
          <div className="flex min-w-max">
            <div className="border-borde bg-superficie sticky left-0 z-10 w-14 shrink-0 border-r">
              <div className="border-borde h-12 border-b" />
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
                  <div className="border-borde bg-tenue/50 flex h-12 flex-col justify-center border-b px-2 text-center text-sm font-semibold capitalize">
                    {col.titulo}
                    {col.subtitulo && (
                      <span className="text-texto-suave text-xs font-normal normal-case">
                        {col.subtitulo}
                      </span>
                    )}
                  </div>
                  <div
                    className="hover:bg-primario-claro/30 relative cursor-cell transition-colors"
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
                        'absolute inset-x-1 overflow-hidden rounded-md border-l-[3px] px-2 py-1 text-xs shadow-suave transition-colors',
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
        Fecha mostrada: {fechaEnBogota(aIsoBogota(fecha, '12:00'))}.
      </p>
    </div>
  )
}
