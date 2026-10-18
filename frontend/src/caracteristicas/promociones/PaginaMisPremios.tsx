import { Gift } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Insignia } from '@/componentes/ui/Insignia'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { Tarjeta, TituloTarjeta } from '@/componentes/ui/Tarjeta'
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
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Mis premios"
        descripcion="Cada reserva completada te acerca a un descuento."
      />
      {isPending && <Cargando texto="Cargando tus premios…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && data.progreso.length === 0 && (
        <EstadoVacio
          icono={Gift}
          titulo="No hay premios de fidelidad activos"
          descripcion="Vuelve pronto."
        />
      )}
      {data?.progreso.map((p) => (
        <Tarjeta key={p.regla.id} className="flex flex-col gap-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-3">
              <span className="flex size-10 items-center justify-center rounded-xl bg-amber-50 text-amber-600">
                <Gift className="size-5" aria-hidden />
              </span>
              <h2 className="text-base font-semibold">{p.regla.nombre}</h2>
            </div>
            <Insignia tono="verde">{p.regla.beneficio} de descuento</Insignia>
          </div>
          <div
            role="progressbar"
            aria-valuemin={0}
            aria-valuemax={p.regla.reservasRequeridas}
            aria-valuenow={p.enCiclo}
            aria-label={`Progreso hacia ${p.regla.nombre}`}
            className="bg-tenue h-2.5 overflow-hidden rounded-full"
          >
            <div
              className="bg-primario h-full rounded-full transition-all"
              style={{ width: `${(100 * p.enCiclo) / p.regla.reservasRequeridas}%` }}
            />
          </div>
          <p className="text-texto-suave text-sm">
            {p.enCiclo} de {p.regla.reservasRequeridas} reservas completadas.{' '}
            <strong className="text-texto font-semibold">
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
        <Tarjeta className="flex flex-col">
          <TituloTarjeta titulo="Premios ganados" className="mb-2" />
          <ul className="divide-borde flex flex-col divide-y text-sm">
            {data.premios.map((p) => (
              <li key={p.id} className="flex flex-wrap items-center justify-between gap-2 py-3">
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
                    <Link to={`/mis-reservas/${p.reservaUsoId}`} className="enlace text-xs">
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
