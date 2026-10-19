import { Download, TrendingDown, TrendingUp } from 'lucide-react'
import { useState } from 'react'

import { useSedesAdmin } from '@/caracteristicas/sedes/consultas'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { Segmentado } from '@/componentes/ui/Pestanas'
import { Plegable } from '@/componentes/ui/Plegable'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearFechaHora, hoyEnBogota, sumarDias } from '@/lib/formato'
import { cn } from '@/lib/utils'

import {
  descargarCsv,
  useClientesFrecuentes,
  useHorasPico,
  usePorCancha,
  useResumen,
  useSerie,
} from './consultas'
import { GraficoIngresos, MapaHorasPico } from './Graficos'
import type { Agrupacion, FiltroEstadisticas, ResumenConComparacion } from './tipos'

const PRESETS = [
  { id: '7', texto: '7 días', dias: 7 },
  { id: '30', texto: '30 días', dias: 30 },
  { id: '90', texto: '90 días', dias: 90 },
]

function Kpi({
  titulo,
  valor,
  variacion,
  ayuda,
  invertir,
}: {
  titulo: string
  valor: string
  variacion?: number
  ayuda?: string
  invertir?: boolean
}) {
  const bueno = variacion === undefined ? null : invertir ? variacion <= 0 : variacion >= 0
  return (
    <Tarjeta className="flex flex-col gap-2 p-5 sm:p-5">
      <p className="text-texto-suave text-sm font-medium">{titulo}</p>
      <div className="flex flex-wrap items-baseline gap-x-2 gap-y-1">
        <p className="font-titulo text-2xl font-bold whitespace-nowrap">{valor}</p>
        {variacion !== undefined && (
          <span
            className={cn(
              'inline-flex items-center gap-0.5 rounded-full px-1.5 py-0.5 text-xs font-semibold',
              bueno ? 'bg-primario-claro text-primario-oscuro' : 'text-peligro bg-peligro-claro',
            )}
            title="Comparado con el periodo anterior"
          >
            {variacion > 0 ? (
              <TrendingUp className="size-3" aria-hidden />
            ) : variacion < 0 ? (
              <TrendingDown className="size-3" aria-hidden />
            ) : null}
            {Math.abs(variacion)}%<span className="sr-only"> vs periodo anterior</span>
          </span>
        )}
      </div>
      {ayuda && <p className="text-texto-suave text-xs">{ayuda}</p>}
    </Tarjeta>
  )
}

function Indicadores({ r }: { r: ResumenConComparacion }) {
  const a = r.actual
  const v = r.variaciones
  return (
    <div className="flex flex-col gap-4">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Kpi
          titulo="Ingresos reales"
          valor={formatearCOP(a.ingresosReales)}
          variacion={v.ingresosReales}
          ayuda="Solo reservas completadas"
        />
        <Kpi
          titulo="Recaudado"
          valor={formatearCOP(a.recaudado)}
          variacion={v.recaudado}
          ayuda={`Por cobrar: ${formatearCOP(a.porCobrar)}`}
        />
        <Kpi
          titulo="Ocupación"
          valor={`${a.ocupacionPct}%`}
          variacion={v.ocupacionPct}
          ayuda={`${a.horasJugadas} h jugadas · ${a.partidosTorneo} partidos de torneo`}
        />
        <Kpi
          titulo="Reservas"
          valor={String(a.reservas)}
          variacion={v.reservas}
          ayuda={`${a.completadas} completadas`}
        />
      </div>
      <Plegable titulo="Más indicadores">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Kpi
            titulo="Ticket promedio"
            valor={formatearCOP(a.ticketPromedio)}
            variacion={v.ticketPromedio}
            ayuda={`${a.clientesUnicos} clientes distintos`}
          />
          <Kpi
            titulo="Cancelaciones"
            valor={`${a.tasaCancelacion}%`}
            ayuda={`${a.canceladas} reservas canceladas`}
          />
          <Kpi
            titulo="No asistencia"
            valor={`${a.tasaNoAsistencia}%`}
            ayuda={`${a.noAsistio} reservas sin presentarse`}
          />
          <Kpi
            titulo="Por estado"
            valor={String(a.reservas)}
            ayuda={
              Object.entries(a.porEstado)
                .map(([e, n]) => `${e.toLowerCase().replace('_', ' ')}: ${n}`)
                .join(' · ') || 'Sin reservas'
            }
          />
        </div>
      </Plegable>
    </div>
  )
}

export function PaginaEstadisticas() {
  const sedes = useSedesAdmin()
  const [filtro, setFiltro] = useState<FiltroEstadisticas>(() => {
    const hoy = hoyEnBogota()
    return { desde: sumarDias(hoy, -29), hasta: hoy, sedeId: null, canchaId: null }
  })
  const [agrupacion, setAgrupacion] = useState<Agrupacion>('DIA')
  const resumen = useResumen(filtro)
  const serie = useSerie(filtro, agrupacion)
  const canchas = usePorCancha(filtro)
  const pico = useHorasPico(filtro)
  const clientes = useClientesFrecuentes(filtro)
  const sede = sedes.data?.find((s) => s.id === filtro.sedeId)
  const valido = filtro.desde <= filtro.hasta

  const presetActual = PRESETS.find(
    (p) =>
      filtro.hasta === hoyEnBogota() && filtro.desde === sumarDias(filtro.hasta, -(p.dias - 1)),
  )?.id
  const preset = (dias: number) => {
    const hoy = hoyEnBogota()
    setFiltro({ ...filtro, desde: sumarDias(hoy, -(dias - 1)), hasta: hoy })
  }

  return (
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Estadísticas"
        descripcion="Cómo va el negocio en el periodo elegido."
        acciones={
          <Boton
            variante="secundario"
            disabled={!valido}
            onClick={() =>
              descargarCsv(filtro, agrupacion).catch((e) => avisar.error(mensajeDeError(e)))
            }
          >
            <Download aria-hidden />
            Exportar CSV
          </Boton>
        }
      />

      <Tarjeta className="flex flex-col gap-4 p-4 sm:p-4">
        <div className="flex flex-wrap items-end gap-4">
          <div className="flex flex-col gap-1.5">
            <span className="text-sm font-medium">Periodo</span>
            <Segmentado
              etiqueta="Periodos rápidos"
              opciones={PRESETS.map((p) => ({ id: p.id, texto: p.texto }))}
              actual={presetActual ?? ''}
              alCambiar={(id) => preset(PRESETS.find((p) => p.id === id)!.dias)}
            />
          </div>
          <CampoSelect
            className="min-w-44"
            etiqueta="Sede"
            value={filtro.sedeId ?? ''}
            onChange={(e) =>
              setFiltro({
                ...filtro,
                sedeId: e.target.value ? Number(e.target.value) : null,
                canchaId: null,
              })
            }
          >
            <option value="">Todas</option>
            {sedes.data?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nombre}
              </option>
            ))}
          </CampoSelect>
        </div>
        <Plegable titulo="Fechas exactas y cancha">
          <div className="flex flex-wrap items-end gap-4">
            <Campo
              etiqueta="Desde"
              type="date"
              value={filtro.desde}
              max={filtro.hasta}
              onChange={(e) => setFiltro({ ...filtro, desde: e.target.value })}
            />
            <Campo
              etiqueta="Hasta"
              type="date"
              value={filtro.hasta}
              onChange={(e) => setFiltro({ ...filtro, hasta: e.target.value })}
            />
            <CampoSelect
              className="min-w-44"
              etiqueta="Cancha"
              value={filtro.canchaId ?? ''}
              disabled={!sede}
              onChange={(e) =>
                setFiltro({ ...filtro, canchaId: e.target.value ? Number(e.target.value) : null })
              }
            >
              <option value="">Todas</option>
              {sede?.canchas.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombre}
                </option>
              ))}
            </CampoSelect>
          </div>
        </Plegable>
      </Tarjeta>
      {!valido && <Alerta tipo="error">La fecha inicial no puede ser posterior a la final.</Alerta>}

      {valido && resumen.isPending && <Cargando texto="Calculando estadísticas…" />}
      {valido && resumen.isError && <Alerta tipo="error">{mensajeDeError(resumen.error)}</Alerta>}
      {valido && resumen.data && <Indicadores r={resumen.data} />}

      {valido && (
        <>
          <Tarjeta className="flex flex-col gap-3">
            <div className="flex justify-end">
              <Segmentado
                etiqueta="Agrupación"
                opciones={[
                  { id: 'DIA' as Agrupacion, texto: 'Día' },
                  { id: 'SEMANA' as Agrupacion, texto: 'Semana' },
                  { id: 'MES' as Agrupacion, texto: 'Mes' },
                ]}
                actual={agrupacion}
                alCambiar={setAgrupacion}
              />
            </div>
            {serie.data ? (
              <GraficoIngresos puntos={serie.data} titulo="Ingresos reales" />
            ) : (
              <Cargando texto="Cargando serie…" />
            )}
          </Tarjeta>

          <div className="grid gap-4 lg:grid-cols-2">
            <Tarjeta>
              {pico.data ? (
                <MapaHorasPico datos={pico.data} />
              ) : (
                <Cargando texto="Cargando horas pico…" />
              )}
            </Tarjeta>
            <Tarjeta className="flex flex-col gap-2">
              <h2 className="text-xl font-extrabold uppercase">Clientes frecuentes</h2>
              {clientes.data?.length === 0 && (
                <p className="text-texto-suave text-sm">Sin reservas completadas en el periodo.</p>
              )}
              <ol className="flex flex-col gap-2 text-sm">
                {clientes.data?.map((c, i) => (
                  <li
                    key={`${c.nombre}-${i}`}
                    className="border-borde/70 flex items-center justify-between gap-2 border-b pb-2 last:border-0"
                  >
                    <span>
                      <span className="text-texto-suave mr-2">{i + 1}.</span>
                      <span className="font-medium">{c.nombre}</span>
                      {c.mostrador && <span className="text-texto-suave"> · mostrador</span>}
                      <span className="text-texto-suave block pl-5 text-xs">
                        última: {formatearFechaHora(c.ultimaReserva)}
                      </span>
                    </span>
                    <span className="text-right">
                      <span className="font-bold">{c.completadas}</span> reservas
                      <span className="text-texto-suave block text-xs">
                        {formatearCOP(c.gasto)}
                      </span>
                    </span>
                  </li>
                ))}
              </ol>
            </Tarjeta>
          </div>

          <Tarjeta className="flex flex-col overflow-hidden p-0 sm:p-0">
            <h2 className="px-5 pt-5 pb-3 text-base font-semibold">Rendimiento por cancha</h2>
            <div className="overflow-x-auto">
              <table className="tabla min-w-[32rem]">
                <thead>
                  <tr>
                    <th>Cancha</th>
                    <th className="text-right">Completadas</th>
                    <th className="text-right">Horas</th>
                    <th className="text-right">Ingresos</th>
                    <th>Ocupación</th>
                  </tr>
                </thead>
                <tbody>
                  {canchas.data?.map((c) => (
                    <tr key={c.canchaId}>
                      <td>
                        <span className="font-medium">{c.cancha}</span>
                        <span className="text-texto-suave block text-xs">{c.sede}</span>
                      </td>
                      <td className="text-right tabular-nums">{c.completadas}</td>
                      <td className="text-right tabular-nums">{c.horas}</td>
                      <td className="text-right tabular-nums">{formatearCOP(c.ingresos)}</td>
                      <td>
                        <span className="flex items-center gap-2">
                          <span
                            className="bg-tenue h-2 w-28 overflow-hidden rounded-full"
                            aria-hidden="true"
                          >
                            <span
                              className="bg-primario block h-full rounded-full"
                              style={{ width: `${c.ocupacionPct}%` }}
                            />
                          </span>
                          <span className="text-xs">{c.ocupacionPct}%</span>
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Tarjeta>
        </>
      )}
    </div>
  )
}
