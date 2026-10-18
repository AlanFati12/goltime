import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearDia } from '@/lib/formato'

import { usePromocionesPublicas } from './consultas'

const DIAS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo']
const hora = (m: number) =>
  `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`

/** Vitrina pública de promociones vigentes. */
export function PaginaPromociones() {
  const { data, isPending, isError, error } = usePromocionesPublicas()
  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-4 p-4 sm:p-6">
      <h1 className="text-3xl font-bold">Promociones</h1>
      <p className="text-texto-suave">
        Escribe el código al solicitar tu reserva. Se aplica un solo descuento por reserva.
      </p>
      {isPending && <Cargando texto="Cargando promociones…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <Tarjeta className="text-texto-suave text-center text-sm">
          No hay promociones vigentes por ahora.
        </Tarjeta>
      )}
      <ul className="grid gap-4 sm:grid-cols-2">
        {data?.map((p) => (
          <li key={p.id}>
            <Tarjeta className="border-acento flex h-full flex-col gap-2 border-2">
              <div className="flex flex-wrap items-center gap-2">
                <Insignia tono="verde">{p.descuentoTexto} de descuento</Insignia>
                {p.soloPrimeraReserva && <Insignia tono="ambar">Primera reserva</Insignia>}
              </div>
              <h2 className="text-lg font-semibold">{p.nombre}</h2>
              {p.descripcion && <p className="text-sm">{p.descripcion}</p>}
              <p className="text-texto-suave text-xs capitalize">
                {formatearDia(p.fechaDesde)} → {formatearDia(p.fechaHasta)}
                {p.diasSemana.length > 0 && ` · ${p.diasSemana.map((d) => DIAS[d - 1]).join(', ')}`}
                {p.minutoInicio != null &&
                  p.minutoFin != null &&
                  ` · ${hora(p.minutoInicio)}–${hora(p.minutoFin)}`}
              </p>
              {p.codigo ? (
                <p className="bg-acento/30 rounded-boton mt-auto px-3 py-2 text-center font-mono text-lg font-bold tracking-widest">
                  {p.codigo}
                </p>
              ) : (
                <p className="text-primario-oscuro mt-auto text-sm font-medium">
                  Se aplica sola al reservar.
                </p>
              )}
            </Tarjeta>
          </li>
        ))}
      </ul>
      <EnlaceBoton to="/reservar" tamano="lg" className="self-start">
        Reservar una cancha
      </EnlaceBoton>
    </div>
  )
}
