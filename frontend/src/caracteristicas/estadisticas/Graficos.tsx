import { useState } from 'react'

import { formatearCOP, formatearDia } from '@/lib/formato'

import type { HorasPico, PuntoSerie } from './tipos'

const DIAS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

/** Barras de ingresos por cubeta de tiempo, con una sola serie (verde de marca), tooltip al pasar y vista de tabla. */
export function GraficoIngresos({ puntos, titulo }: { puntos: PuntoSerie[]; titulo: string }) {
  const [activo, setActivo] = useState<number | null>(null)
  const maximo = Math.max(1, ...puntos.map((p) => p.ingresos))
  const ancho = 720
  const alto = 220
  const margen = { izq: 8, der: 8, sup: 12, inf: 26 }
  const util = ancho - margen.izq - margen.der
  const paso = util / Math.max(puntos.length, 1)
  const barra = Math.max(2, Math.min(28, paso - 2))
  const etiquetaCada = Math.ceil(puntos.length / 8)

  return (
    <figure className="flex flex-col gap-2">
      <figcaption className="flex items-baseline justify-between gap-2">
        <span className="font-titulo text-xl font-extrabold uppercase">{titulo}</span>
        <span className="text-texto-suave text-xs">Máximo {formatearCOP(maximo)}</span>
      </figcaption>
      <div className="relative">
        <svg
          viewBox={`0 0 ${ancho} ${alto}`}
          role="img"
          aria-label={`${titulo}: ${puntos.length} periodos`}
          className="w-full"
        >
          <line
            x1={margen.izq}
            x2={ancho - margen.der}
            y1={alto - margen.inf}
            y2={alto - margen.inf}
            className="stroke-borde"
            strokeWidth={1}
          />
          {puntos.map((p, i) => {
            const h = ((alto - margen.sup - margen.inf) * p.ingresos) / maximo
            const x = margen.izq + i * paso + (paso - barra) / 2
            return (
              <g
                key={p.fecha}
                onMouseEnter={() => setActivo(i)}
                onMouseLeave={() => setActivo(null)}
              >
                <rect
                  x={margen.izq + i * paso}
                  y={0}
                  width={paso}
                  height={alto - margen.inf}
                  fill="transparent"
                />
                <rect
                  x={x}
                  y={alto - margen.inf - h}
                  width={barra}
                  height={Math.max(h, p.ingresos > 0 ? 2 : 0)}
                  rx={3}
                  className={activo === i ? 'fill-noche' : 'fill-primario'}
                />
                {i % etiquetaCada === 0 && (
                  <text
                    x={x + barra / 2}
                    y={alto - 8}
                    textAnchor="middle"
                    className="fill-texto-suave text-[10px]"
                  >
                    {p.fecha.slice(5)}
                  </text>
                )}
              </g>
            )
          })}
        </svg>
        {activo !== null && (
          <div
            role="status"
            className="border-borde bg-superficie rounded-boton pointer-events-none absolute top-0 right-0 border px-3 py-2 text-xs shadow-md"
          >
            <p className="font-semibold capitalize">{formatearDia(puntos[activo].fecha)}</p>
            <p>Ingresos: {formatearCOP(puntos[activo].ingresos)}</p>
            <p>Completadas: {puntos[activo].completadas}</p>
            <p>Canceladas: {puntos[activo].canceladas}</p>
          </div>
        )}
      </div>
      <details className="text-sm">
        <summary className="text-primario cursor-pointer font-medium">Ver como tabla</summary>
        <div className="mt-2 max-h-64 overflow-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="text-texto-suave">
                <th className="py-1">Periodo</th>
                <th className="py-1 text-right">Ingresos</th>
                <th className="py-1 text-right">Completadas</th>
                <th className="py-1 text-right">Canceladas</th>
              </tr>
            </thead>
            <tbody>
              {puntos.map((p) => (
                <tr key={p.fecha} className="border-borde border-t">
                  <td className="py-1">{p.fecha}</td>
                  <td className="py-1 text-right">{formatearCOP(p.ingresos)}</td>
                  <td className="py-1 text-right">{p.completadas}</td>
                  <td className="py-1 text-right">{p.canceladas}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </figure>
  )
}

/** Mapa de calor día × hora de una sola tonalidad (más oscuro = más reservas). */
export function MapaHorasPico({ datos }: { datos: HorasPico }) {
  const [activa, setActiva] = useState<{ dia: number; hora: number; n: number } | null>(null)
  const porClave = new Map(datos.celdas.map((c) => [`${c.diaSemana}-${c.hora}`, c.reservas]))
  const horas = datos.celdas.length ? datos.celdas.map((c) => c.hora) : [6, 22]
  const desde = Math.min(...horas)
  const hasta = Math.max(...horas)
  const lista = Array.from({ length: hasta - desde + 1 }, (_, i) => desde + i)

  return (
    <figure className="flex flex-col gap-2">
      <figcaption className="flex items-baseline justify-between gap-2">
        <span className="font-titulo text-xl font-extrabold uppercase">Horas pico</span>
        <span className="text-texto-suave text-xs" role="status">
          {activa
            ? `${DIAS[activa.dia - 1]} ${activa.hora}:00 · ${activa.n} reservas`
            : 'Pasa el cursor sobre una celda'}
        </span>
      </figcaption>
      {datos.celdas.length === 0 ? (
        <p className="text-texto-suave text-sm">No hay reservas en el periodo.</p>
      ) : (
        <div className="overflow-x-auto">
          <table
            className="border-separate border-spacing-0.5 text-[10px]"
            aria-label="Reservas por día de la semana y hora"
          >
            <thead>
              <tr>
                <th />
                {lista.map((h) => (
                  <th key={h} scope="col" className="text-texto-suave w-7 font-normal">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {DIAS.map((d, i) => (
                <tr key={d}>
                  <th scope="row" className="text-texto-suave pr-2 text-right font-normal">
                    {d}
                  </th>
                  {lista.map((h) => {
                    const n = porClave.get(`${i + 1}-${h}`) ?? 0
                    return (
                      <td
                        key={h}
                        onMouseEnter={() => setActiva({ dia: i + 1, hora: h, n })}
                        onMouseLeave={() => setActiva(null)}
                        className="bg-primario size-7 rounded text-center"
                        style={{ opacity: n === 0 ? 0.07 : 0.25 + 0.75 * (n / datos.maximo) }}
                        title={`${d} ${h}:00 · ${n} reservas`}
                      >
                        <span className="sr-only">{n}</span>
                      </td>
                    )
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </figure>
  )
}
