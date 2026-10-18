import { Link } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearFechaHora } from '@/lib/formato'

import { useMisPremios } from './consultas'

const ETIQUETA = {
  DISPONIBLE: 'Disponible',
  USADO: 'Usado',
  VENCIDO: 'Vencido',
  REVOCADO: 'Revocado',
} as const
const TONO = { DISPONIBLE: 'verde', USADO: 'gris', VENCIDO: 'ambar', REVOCADO: 'rojo' } as const

/** Progreso del cliente hacia sus premios de fidelidad y los premios que ya ganó. */
export function PaginaMisPremios() {
  const { data, isPending, isError, error } = useMisPremios()
  const disponibles = data?.premios.filter((p) => p.estado === 'DISPONIBLE') ?? []

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-4 p-4 sm:p-6">
      <h1 className="text-3xl font-bold">Mis premios</h1>
      {isPending && <Cargando texto="Cargando tus premios…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && data.progreso.length === 0 && (
        <Tarjeta className="text-texto-suave text-sm">
          Por ahora no hay premios de fidelidad activos. Vuelve pronto.
        </Tarjeta>
      )}
      {data?.progreso.map((p) => (
        <Tarjeta key={p.regla.id} className="flex flex-col gap-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg font-semibold">{p.regla.nombre}</h2>
            <Insignia tono="verde">{p.regla.beneficio} de descuento</Insignia>
          </div>
          <div
            role="progressbar"
            aria-valuemin={0}
            aria-valuemax={p.regla.reservasRequeridas}
            aria-valuenow={p.enCiclo}
            aria-label={`Progreso hacia ${p.regla.nombre}`}
            className="bg-borde h-3 overflow-hidden rounded-full"
          >
            <div
              className="bg-primario h-full"
              style={{ width: `${(100 * p.enCiclo) / p.regla.reservasRequeridas}%` }}
            />
          </div>
          <p className="text-sm">
            {p.enCiclo} de {p.regla.reservasRequeridas} reservas completadas.{' '}
            <strong>
              {p.faltan === 1 ? 'Te falta 1 reserva' : `Te faltan ${p.faltan} reservas`}
            </strong>{' '}
            para ganar este premio.
          </p>
        </Tarjeta>
      ))}

      {disponibles.length > 0 && (
        <Alerta tipo="exito">
          Tienes {disponibles.length}{' '}
          {disponibles.length === 1 ? 'premio disponible' : 'premios disponibles'}. Elígelo al
          solicitar tu próxima reserva.
          <span className="mt-2 block">
            <EnlaceBoton to="/reservar" tamano="sm">
              Reservar ahora
            </EnlaceBoton>
          </span>
        </Alerta>
      )}

      {data && data.premios.length > 0 && (
        <Tarjeta className="flex flex-col gap-2">
          <h2 className="text-lg font-semibold">Premios ganados</h2>
          <ul className="flex flex-col gap-2 text-sm">
            {data.premios.map((p) => (
              <li
                key={p.id}
                className="border-borde flex flex-wrap items-center justify-between gap-2 border-b pb-2"
              >
                <span>
                  <span className="font-medium">{p.nombre}</span> · {p.beneficio}
                  <span className="text-texto-suave block text-xs">
                    Ganado {formatearFechaHora(p.otorgadoEn)} · vence{' '}
                    {formatearFechaHora(p.venceEn)}
                  </span>
                </span>
                <span className="flex items-center gap-2">
                  <Insignia tono={TONO[p.estado]}>{ETIQUETA[p.estado]}</Insignia>
                  {p.reservaUsoId && (
                    <Link
                      to={`/mis-reservas/${p.reservaUsoId}`}
                      className="text-primario text-xs hover:underline"
                    >
                      Ver reserva
                    </Link>
                  )}
                </span>
              </li>
            ))}
          </ul>
        </Tarjeta>
      )}
    </div>
  )
}
