import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { ModalMotivo } from '@/componentes/ui/ModalMotivo'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { codigoDeError, mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearFechaHora } from '@/lib/formato'

import { InsigniaEstadoReserva } from './InsigniaEstadoReserva'
import { ModalReprogramar } from './ModalReprogramar'
import { useCancelarReserva, useReserva } from './consultas'
import { ETIQUETA_PAGO, describirEntrada, rangoHorario, textoDuracion } from './utilidades'

export function PaginaReservaDetalle() {
  const id = Number(useParams().id)
  const { data: r, isPending, isError, error } = useReserva(id)
  const cancelar = useCancelarReserva()
  const [dialogo, setDialogo] = useState<'cancelar' | 'reprogramar' | null>(null)

  if (isPending) return <Cargando texto="Cargando reserva…" />
  if (isError) {
    return (
      <div className="flex flex-col gap-3">
        <Alerta tipo="error">
          {codigoDeError(error) === 'RECURSO_NO_ENCONTRADO'
            ? 'Esta reserva no existe.'
            : mensajeDeError(error)}
        </Alerta>
        <Link to="/mis-reservas" className="text-primario text-sm underline">
          Volver a mis reservas
        </Link>
      </div>
    )
  }

  const textoWhatsapp = encodeURIComponent(
    `⚽ Reserva ${r.codigo}\n${r.sede.nombre} · ${r.cancha.nombre} (${r.cancha.modalidad})\n${formatearFechaHora(r.inicio)} · ${rangoHorario(r.inicio, r.fin)}${r.nombreEquipo ? `\nEquipo: ${r.nombreEquipo}` : ''}`,
  )

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-5">
      <nav aria-label="Ruta" className="text-texto-suave text-sm print:hidden">
        <Link to="/mis-reservas" className="hover:underline">
          Mis reservas
        </Link>{' '}
        / {r.codigo}
      </nav>

      <Tarjeta className="flex flex-col gap-4" id="comprobante">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div>
            <p className="text-texto-suave text-xs uppercase">Comprobante de reserva</p>
            <h1 className="font-mono text-2xl font-bold">{r.codigo}</h1>
          </div>
          <InsigniaEstadoReserva estado={r.estado} />
        </div>

        {r.estado === 'PENDIENTE' && (
          <Alerta tipo="aviso">
            Esperando la aprobación del administrador.
            {r.expiraEn && (
              <> Si no responde antes del {formatearFechaHora(r.expiraEn)}, el horario se libera.</>
            )}
          </Alerta>
        )}
        {r.estado === 'RECHAZADA' && r.motivoRechazo && (
          <Alerta tipo="error">Rechazada. Motivo: {r.motivoRechazo}</Alerta>
        )}
        {r.estado === 'CANCELADA' && (
          <Alerta tipo="info">
            Cancelada por {r.canceladaPorRol === 'ADMIN' ? 'el administrador' : 'ti'}
            {r.motivoCancelacion ? `. Motivo: ${r.motivoCancelacion}` : '.'}
            {r.cancelacionTardia && ' Fue una cancelación tardía.'}
          </Alerta>
        )}

        <dl className="grid grid-cols-[8rem_1fr] gap-x-3 gap-y-2 text-sm">
          <dt className="text-texto-suave">Sede</dt>
          <dd>{r.sede.nombre}</dd>
          <dt className="text-texto-suave">Cancha</dt>
          <dd>
            {r.cancha.nombre} ({r.cancha.modalidad})
          </dd>
          <dt className="text-texto-suave">Fecha y hora</dt>
          <dd className="capitalize">
            {formatearFechaHora(r.inicio)} · {rangoHorario(r.inicio, r.fin)} (
            {textoDuracion(r.duracionMin)})
          </dd>
          <dt className="text-texto-suave">Valor</dt>
          <dd>
            <strong>{formatearCOP(r.precioFinal)}</strong> ·{' '}
            {ETIQUETA_PAGO[r.estadoPago] ?? r.estadoPago}
            {r.descuento > 0 && (
              <span className="text-texto-suave"> (descuento {formatearCOP(r.descuento)})</span>
            )}
          </dd>
          {r.nombreEquipo && (
            <>
              <dt className="text-texto-suave">Equipo</dt>
              <dd>{r.nombreEquipo}</dd>
            </>
          )}
          {r.notas && (
            <>
              <dt className="text-texto-suave">Notas</dt>
              <dd>{r.notas}</dd>
            </>
          )}
          {r.vecesReprogramada > 0 && (
            <>
              <dt className="text-texto-suave">Reprogramaciones</dt>
              <dd>{r.vecesReprogramada}</dd>
            </>
          )}
        </dl>
      </Tarjeta>

      <div className="flex flex-wrap gap-2 print:hidden">
        {r.puedeReprogramar && <Boton onClick={() => setDialogo('reprogramar')}>Reprogramar</Boton>}
        {r.puedeCancelar && (
          <Boton variante="secundario" onClick={() => setDialogo('cancelar')}>
            Cancelar reserva
          </Boton>
        )}
        <Boton variante="secundario" onClick={() => window.print()}>
          Imprimir comprobante
        </Boton>
        <a
          href={`https://wa.me/?text=${textoWhatsapp}`}
          target="_blank"
          rel="noreferrer"
          className="border-borde bg-superficie hover:bg-primario-claro rounded-boton inline-flex h-10 items-center border px-4 text-sm font-medium"
        >
          Compartir por WhatsApp
        </a>
      </div>
      {r.puedeCancelar && !r.puedeReprogramar && r.estado !== 'PENDIENTE' && (
        <p className="text-texto-suave text-xs print:hidden">
          Ya no puedes reprogramar (alcanzaste el máximo o falta poco para el partido).
        </p>
      )}

      <Tarjeta className="print:hidden">
        <h2 className="mb-3 text-lg font-semibold">Historial</h2>
        <ol className="border-borde flex flex-col gap-3 border-l pl-4 text-sm">
          {r.historial?.map((h) => (
            <li key={h.id} className="relative">
              <span
                className="bg-primario absolute top-1.5 -left-[1.4rem] size-2 rounded-full"
                aria-hidden
              />
              {describirEntrada(h)}
            </li>
          ))}
        </ol>
      </Tarjeta>

      {dialogo === 'reprogramar' && (
        <ModalReprogramar reserva={r} alCerrar={() => setDialogo(null)} />
      )}
      {dialogo === 'cancelar' && (
        <ModalMotivo
          titulo="Cancelar reserva"
          descripcion={`¿Seguro que quieres cancelar la reserva ${r.codigo}? El horario quedará libre para otros clientes.`}
          advertencia={
            r.cancelacionSeriaTardia
              ? 'Falta poco para el partido: se registrará como cancelación tardía.'
              : undefined
          }
          etiquetaMotivo="Motivo"
          motivoObligatorio={false}
          textoConfirmar="Sí, cancelar"
          alCerrar={() => setDialogo(null)}
          alConfirmar={async (motivo) => {
            try {
              await cancelar.mutateAsync({ id: r.id, motivo: motivo || undefined })
              avisar.exito('Reserva cancelada.')
              setDialogo(null)
            } catch (e) {
              avisar.error(mensajeDeError(e))
            }
          }}
        />
      )}
    </div>
  )
}
