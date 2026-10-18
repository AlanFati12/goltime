import { descargarCalendario } from '@/caracteristicas/extras/consultas'
import { PanelCalificacion } from '@/caracteristicas/extras/PanelCalificacion'
import { CalendarPlus, CalendarSync, Printer, Share2, X } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { ModalMotivo } from '@/componentes/ui/ModalMotivo'
import { estilosBoton } from '@/componentes/ui/estilosBoton'
import { Migas } from '@/componentes/ui/Migas'
import { Tarjeta, TituloTarjeta } from '@/componentes/ui/Tarjeta'
import { codigoDeError, mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearFechaHora } from '@/lib/formato'

import { BloqueFecha } from './BloqueFecha'
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
        <Link to="/mis-reservas" className="enlace text-sm">
          Volver a mis reservas
        </Link>
      </div>
    )
  }

  const textoWhatsapp = encodeURIComponent(
    `⚽ Reserva ${r.codigo}\n${r.sede.nombre} · ${r.cancha.nombre} (${r.cancha.modalidad})\n${formatearFechaHora(r.inicio)} · ${rangoHorario(r.inicio, r.fin)}${r.nombreEquipo ? `\nEquipo: ${r.nombreEquipo}` : ''}`,
  )

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-6">
      <div className="print:hidden">
        <Migas items={[{ texto: 'Mis reservas', a: '/mis-reservas' }, { texto: r.codigo }]} />
      </div>

      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_18rem]">
        <div className="flex flex-col gap-6">
          <Tarjeta className="flex flex-col gap-5" id="comprobante">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="flex items-center gap-4">
                <BloqueFecha fecha={r.inicio} />
                <div>
                  <p className="text-texto-suave text-xs font-medium tracking-wide uppercase">
                    Comprobante de reserva
                  </p>
                  <h1 className="font-mono text-2xl font-bold">{r.codigo}</h1>
                </div>
              </div>
              <InsigniaEstadoReserva estado={r.estado} />
            </div>

            {r.estado === 'PENDIENTE' && (
              <Alerta tipo="aviso">
                Esperando la aprobación del administrador.
                {r.expiraEn && (
                  <>
                    {' '}
                    Si no responde antes del {formatearFechaHora(r.expiraEn)}, el horario se libera.
                  </>
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

            <dl className="border-borde grid grid-cols-[8rem_1fr] gap-x-3 gap-y-3 border-t pt-5 text-sm">
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
                <strong className="font-semibold">{formatearCOP(r.precioFinal)}</strong> ·{' '}
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

          <PanelCalificacion reservaId={r.id} />
          <Tarjeta className="print:hidden">
            <TituloTarjeta titulo="Historial" />
            <ol className="border-borde ml-1 flex flex-col gap-4 border-l pl-5 text-sm">
              {r.historial?.map((h) => (
                <li key={h.id} className="relative">
                  <span
                    className="bg-superficie border-primario absolute top-1 -left-[1.6rem] size-2.5 rounded-full border-2"
                    aria-hidden
                  />
                  {describirEntrada(h)}
                </li>
              ))}
            </ol>
          </Tarjeta>
        </div>

        <aside className="flex flex-col gap-2 lg:sticky lg:top-24 print:hidden">
          <Tarjeta className="flex flex-col gap-2 p-4 sm:p-4">
            <p className="text-texto-suave px-1 pb-1 text-xs font-medium tracking-wide uppercase">
              Acciones
            </p>
            {r.puedeReprogramar && (
              <Boton onClick={() => setDialogo('reprogramar')}>
                <CalendarSync aria-hidden />
                Reprogramar
              </Boton>
            )}
            <Boton variante="secundario" onClick={() => window.print()}>
              <Printer aria-hidden />
              Imprimir comprobante
            </Boton>
            {(r.estado === 'CONFIRMADA' || r.estado === 'PENDIENTE') && (
              <Boton
                variante="secundario"
                onClick={() =>
                  descargarCalendario(r.id, r.codigo).catch((e) => avisar.error(mensajeDeError(e)))
                }
              >
                <CalendarPlus aria-hidden />
                Agregar al calendario
              </Boton>
            )}
            <a
              href={`https://wa.me/?text=${textoWhatsapp}`}
              target="_blank"
              rel="noreferrer"
              className={estilosBoton({ variante: 'secundario' })}
            >
              <Share2 aria-hidden />
              Compartir por WhatsApp
            </a>
            {r.puedeCancelar && (
              <Boton
                variante="fantasma"
                className="text-peligro hover:bg-peligro/5 hover:text-peligro"
                onClick={() => setDialogo('cancelar')}
              >
                <X aria-hidden />
                Cancelar reserva
              </Boton>
            )}
          </Tarjeta>
          {r.puedeCancelar && !r.puedeReprogramar && r.estado !== 'PENDIENTE' && (
            <p className="text-texto-suave px-1 text-xs">
              Ya no puedes reprogramar (alcanzaste el máximo o falta poco para el partido).
            </p>
          )}
        </aside>
      </div>

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
