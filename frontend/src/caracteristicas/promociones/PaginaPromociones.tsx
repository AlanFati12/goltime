import { TicketPercent } from 'lucide-react'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
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
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Promociones"
        descripcion="Escribe el código al solicitar tu reserva. Se aplica un solo descuento por reserva."
        acciones={<EnlaceBoton to="/reservar">Reservar una cancha</EnlaceBoton>}
      />
      {isPending && <Cargando texto="Cargando promociones…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <EstadoVacio icono={TicketPercent} titulo="No hay promociones vigentes por ahora" />
      )}
      <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {data?.map((p) => (
          <li key={p.id}>
            <Tarjeta className="flex h-full flex-col gap-3 overflow-hidden p-0 sm:p-0">
              <div className="flex items-end justify-between gap-2 bg-gradient-to-br from-amber-400 to-orange-500 px-5 py-4 text-white">
                <p className="font-titulo text-3xl font-extrabold">{p.descuentoTexto}</p>
                <TicketPercent className="size-7 opacity-80" aria-hidden />
              </div>
              <div className="flex flex-1 flex-col gap-2 px-5 pb-5">
                {p.soloPrimeraReserva && (
                  <Insignia tono="ambar" className="self-start">
                    Primera reserva
                  </Insignia>
                )}
                <h2 className="text-base font-semibold">{p.nombre}</h2>
                {p.descripcion && <p className="text-texto-suave text-sm">{p.descripcion}</p>}
                <p className="text-texto-suave text-xs capitalize">
                  {formatearDia(p.fechaDesde)} → {formatearDia(p.fechaHasta)}
                  {p.diasSemana.length > 0 &&
                    ` · ${p.diasSemana.map((d) => DIAS[d - 1]).join(', ')}`}
                  {p.minutoInicio != null &&
                    p.minutoFin != null &&
                    ` · ${hora(p.minutoInicio)}–${hora(p.minutoFin)}`}
                </p>
                {p.codigo ? (
                  <p className="border-acento/60 rounded-boton mt-auto border-2 border-dashed bg-amber-50 px-3 py-2 text-center font-mono text-lg font-bold tracking-widest">
                    {p.codigo}
                  </p>
                ) : (
                  <p className="text-primario-oscuro mt-auto text-sm font-medium">
                    Se aplica sola al reservar.
                  </p>
                )}
              </div>
            </Tarjeta>
          </li>
        ))}
      </ul>
    </div>
  )
}
