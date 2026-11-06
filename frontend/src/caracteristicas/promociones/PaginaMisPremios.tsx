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
      {isPending && <Cargando texto="Cargando tus premios…" variante="lista" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && data.progreso.length === 0 && (
        <EstadoVacio
          icono={Gift}
          titulo="No hay premios de fidelidad activos"
          descripcion="Vuelve pronto."
        />
      )}
      {data?.progreso.map((p) => (
        <div
          key={p.regla.id}
          className="bg-noche relative isolate flex flex-col gap-4 overflow-hidden rounded-[1.5rem] p-6 text-white sm:p-8"
        >
          <Gift
            className="text-volt/10 absolute -right-6 -bottom-8 -z-10 size-44 -rotate-12"
            aria-hidden
          />
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="text-3xl leading-none font-extrabold uppercase">{p.regla.nombre}</h2>
              <p className="text-volt-vivo mt-1 font-semibold">{p.regla.beneficio} de descuento</p>
            </div>
            <p className="font-titulo cifras text-6xl leading-none font-extrabold">
              {p.enCiclo}
              <span className="text-2xl text-white/40">/{p.regla.reservasRequeridas}</span>
            </p>
          </div>
          <div
            role="progressbar"
            aria-valuemin={0}
            aria-valuemax={p.regla.reservasRequeridas}
            aria-valuenow={p.enCiclo}
            aria-label={`Progreso hacia ${p.regla.nombre}`}
            className="h-3 overflow-hidden rounded-full bg-white/10"
          >
            <div
              className="bg-volt h-full rounded-full shadow-[0_0_16px_var(--color-volt)] transition-[width] duration-1000 ease-[var(--ease-salida)]"
              style={{ width: `${(100 * p.enCiclo) / p.regla.reservasRequeridas}%` }}
            />
          </div>
          <p className="text-sm text-white/65">
            {p.enCiclo} de {p.regla.reservasRequeridas} reservas completadas.{' '}
            <strong className="font-semibold text-white">
              {p.faltan === 1 ? 'Te falta 1 reserva' : `Te faltan ${p.faltan} reservas`}
            </strong>{' '}
            para ganar este premio.
          </p>
        </div>
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
