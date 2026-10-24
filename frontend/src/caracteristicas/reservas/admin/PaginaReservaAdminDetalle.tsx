import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { ModalMotivo } from '@/componentes/ui/ModalMotivo'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { Migas } from '@/componentes/ui/Migas'
import { codigoDeError, mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearFechaHora } from '@/lib/formato'
import { formatearTelefono } from '@/lib/telefono'

import { InsigniaEstadoReserva } from '../InsigniaEstadoReserva'
import { ModalReprogramar } from '../ModalReprogramar'
import { useAccionesReservaAdmin } from '../consultas'
import { ETIQUETA_PAGO, describirEntrada, rangoHorario, textoDuracion } from '../utilidades'
import { ModalPago, ModalReembolso } from './ModalesPago'
import { useAccionesPago, usePagos, useReservaAdmin } from './consultas'

type Dialogo = 'pago' | 'reembolso' | 'reprogramar' | 'rechazar' | 'cancelar' | null

const TONO_PAGO: Record<string, 'verde' | 'ambar' | 'rojo' | 'gris'> = {
  PENDIENTE: 'gris',
  ABONADO: 'ambar',
  PAGADO: 'verde',
  REEMBOLSO_PENDIENTE: 'rojo',
  REEMBOLSADO: 'gris',
}

export function PaginaReservaAdminDetalle() {
  const id = Number(useParams().id)
  const { data: r, isPending, isError, error } = useReservaAdmin(id)
  const pagos = usePagos(id)
  const { aprobar, rechazar, cancelar } = useAccionesReservaAdmin()
  const { marcar } = useAccionesPago()
  const [dialogo, setDialogo] = useState<Dialogo>(null)

  if (isPending) return <Cargando texto="Cargando reserva…" />
  if (isError) {
    return (
      <div className="flex flex-col gap-3">
        <Alerta tipo="error">
          {codigoDeError(error) === 'RECURSO_NO_ENCONTRADO'
            ? 'Esta reserva no existe.'
            : mensajeDeError(error)}
        </Alerta>
        <Link to="/admin/reservas" className="enlace text-sm">
          Volver a reservas
        </Link>
      </div>
    )
  }

  const saldo = Math.max(0, r.precioFinal - r.montoPagado)
  const yaEmpezo = new Date(r.inicio).getTime() <= Date.now()
  const yaTermino = new Date(r.fin).getTime() <= Date.now()
  const puedePagar = ['CONFIRMADA', 'COMPLETADA', 'NO_ASISTIO'].includes(r.estado) && saldo > 0
  const mostrador = r.cliente.id === null

  const marcarEstado = (estado: 'COMPLETADA' | 'NO_ASISTIO') =>
    marcar.mutate(
      { id: r.id, estado },
      {
        onSuccess: () =>
          avisar.exito(
            estado === 'COMPLETADA' ? 'Marcada como completada.' : 'Marcada como no asistió.',
          ),
        onError: (e) => avisar.error(mensajeDeError(e)),
      },
    )

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-6">
      <Migas items={[{ texto: 'Reservas', a: '/admin/reservas' }, { texto: r.codigo }]} />

      <div className="grid gap-5 lg:grid-cols-[3fr_2fr]">
        <Tarjeta className="flex flex-col gap-4">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <h1 className="cifras text-2xl font-bold font-semibold tracking-wide">{r.codigo}</h1>
            <div className="flex gap-1">
              <Insignia tono={r.origen === 'ADMIN' ? 'verde' : 'gris'}>
                {r.origen === 'ADMIN' ? 'Creada por admin' : 'Solicitada en la web'}
              </Insignia>
              <InsigniaEstadoReserva estado={r.estado} />
            </div>
          </div>
          {r.estado === 'PENDIENTE' && r.expiraEn && (
            <Alerta tipo="aviso">
              Esperando tu respuesta. Expira el {formatearFechaHora(r.expiraEn)}.
            </Alerta>
          )}
          {r.estado === 'RECHAZADA' && r.motivoRechazo && (
            <Alerta tipo="info">Rechazada. Motivo: {r.motivoRechazo}</Alerta>
          )}
          {r.estado === 'CANCELADA' && (
            <Alerta tipo="info">
              Cancelada por{' '}
              {r.canceladaPorRol === 'ADMIN'
                ? 'un administrador'
                : r.canceladaPorRol === 'CLIENTE'
                  ? 'el cliente'
                  : 'el sistema'}
              {r.motivoCancelacion ? `. Motivo: ${r.motivoCancelacion}` : '.'}
              {r.cancelacionTardia && ' Fue una cancelación tardía.'}
            </Alerta>
          )}
          <dl className="grid grid-cols-[8rem_1fr] gap-x-3 gap-y-2 text-sm">
            <dt className="text-texto-suave">Cliente</dt>
            <dd>
              {r.cliente.nombre} {mostrador && <Insignia>De mostrador</Insignia>}
              <div className="text-texto-suave text-xs">
                {[formatearTelefono(r.cliente.telefono), r.cliente.correo]
                  .filter(Boolean)
                  .join(' · ')}
              </div>
            </dd>
            <dt className="text-texto-suave">Sede y cancha</dt>
            <dd>
              {r.sede.nombre} · {r.cancha.nombre} ({r.cancha.modalidad})
            </dd>
            <dt className="text-texto-suave">Horario</dt>
            <dd className="capitalize">
              {formatearFechaHora(r.inicio)} · {rangoHorario(r.inicio, r.fin)} (
              {textoDuracion(r.duracionMin)})
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
            <dt className="text-texto-suave">Reprogramaciones</dt>
            <dd>{r.vecesReprogramada}</dd>
          </dl>
        </Tarjeta>

        <Tarjeta className="flex flex-col gap-3">
          <h2 className="text-xl font-extrabold uppercase">Pagos</h2>
          <div className="flex items-center justify-between">
            <span className="text-sm">
              Valor <strong>{formatearCOP(r.precioFinal)}</strong>
            </span>
            <Insignia tono={TONO_PAGO[r.estadoPago] ?? 'gris'}>
              {ETIQUETA_PAGO[r.estadoPago] ?? r.estadoPago}
            </Insignia>
          </div>
          <p className="text-texto-suave text-sm">
            Pagado {formatearCOP(r.montoPagado)} · saldo {formatearCOP(saldo)}
          </p>
          {pagos.data && pagos.data.length > 0 && (
            <ul className="divide-borde divide-y text-sm">
              {pagos.data.map((p) => (
                <li key={p.id} className="flex justify-between gap-2 py-2">
                  <span>
                    <span className={p.tipo === 'REEMBOLSO' ? 'text-peligro' : undefined}>
                      {p.tipo === 'REEMBOLSO' ? 'Reembolso' : p.tipo === 'ABONO' ? 'Abono' : 'Pago'}
                    </span>{' '}
                    · {p.metodo.toLowerCase()}
                    <span className="text-texto-suave block text-xs">
                      {formatearFechaHora(p.fecha)} · {p.registradoPor}
                      {p.referencia ? ` · ref. ${p.referencia}` : ''}
                    </span>
                  </span>
                  <strong className={p.tipo === 'REEMBOLSO' ? 'text-peligro' : undefined}>
                    {p.tipo === 'REEMBOLSO' ? '−' : ''}
                    {formatearCOP(p.monto)}
                  </strong>
                </li>
              ))}
            </ul>
          )}
          {pagos.data?.length === 0 && (
            <p className="text-texto-suave text-sm">Aún no hay pagos registrados.</p>
          )}
          <div className="flex flex-wrap gap-2">
            {puedePagar && <Boton onClick={() => setDialogo('pago')}>Registrar pago</Boton>}
            {r.estadoPago === 'REEMBOLSO_PENDIENTE' && (
              <Boton variante="peligro" onClick={() => setDialogo('reembolso')}>
                Registrar reembolso
              </Boton>
            )}
          </div>
        </Tarjeta>
      </div>

      <div className="flex flex-wrap gap-2">
        {r.estado === 'PENDIENTE' && (
          <>
            <Boton
              disabled={aprobar.isPending}
              onClick={() =>
                aprobar.mutate(r.id, {
                  onSuccess: () => avisar.exito(`Reserva ${r.codigo} confirmada.`),
                  onError: (e) => avisar.error(mensajeDeError(e)),
                })
              }
            >
              Aprobar
            </Boton>
            <Boton variante="secundario" onClick={() => setDialogo('rechazar')}>
              Rechazar
            </Boton>
          </>
        )}
        {(r.estado === 'PENDIENTE' || r.estado === 'CONFIRMADA') && !yaEmpezo && (
          <>
            <Boton variante="secundario" onClick={() => setDialogo('reprogramar')}>
              Reprogramar
            </Boton>
            <Boton variante="secundario" onClick={() => setDialogo('cancelar')}>
              Cancelar reserva
            </Boton>
          </>
        )}
        {r.estado === 'CONFIRMADA' && yaTermino && (
          <Boton
            variante="secundario"
            disabled={marcar.isPending}
            onClick={() => marcarEstado('COMPLETADA')}
          >
            Marcar como completada
          </Boton>
        )}
        {(r.estado === 'CONFIRMADA' || r.estado === 'COMPLETADA') && yaEmpezo && (
          <Boton
            variante="secundario"
            disabled={marcar.isPending}
            onClick={() => marcarEstado('NO_ASISTIO')}
          >
            Marcar como no asistió
          </Boton>
        )}
      </div>

      <Tarjeta>
        <h2 className="mb-3 text-base font-semibold">Historial</h2>
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

      {dialogo === 'pago' && <ModalPago reserva={r} alCerrar={() => setDialogo(null)} />}
      {dialogo === 'reembolso' && <ModalReembolso reserva={r} alCerrar={() => setDialogo(null)} />}
      {dialogo === 'reprogramar' && (
        <ModalReprogramar reserva={r} admin alCerrar={() => setDialogo(null)} />
      )}
      {dialogo === 'rechazar' && (
        <ModalMotivo
          titulo="Rechazar solicitud"
          descripcion={`Se liberará el horario y se avisará a ${r.cliente.nombre}.`}
          textoConfirmar="Rechazar"
          alCerrar={() => setDialogo(null)}
          alConfirmar={async (motivo) => {
            try {
              await rechazar.mutateAsync({ id: r.id, motivo })
              avisar.exito('Solicitud rechazada.')
              setDialogo(null)
            } catch (e) {
              avisar.error(mensajeDeError(e))
            }
          }}
        />
      )}
      {dialogo === 'cancelar' && (
        <ModalMotivo
          titulo="Cancelar reserva"
          descripcion={`Se cancelará ${r.codigo} y se avisará al cliente.${r.montoPagado > 0 ? ' Como tiene dinero pagado, quedará un reembolso pendiente.' : ''}`}
          textoConfirmar="Cancelar reserva"
          alCerrar={() => setDialogo(null)}
          alConfirmar={async (motivo) => {
            try {
              await cancelar.mutateAsync({ id: r.id, motivo })
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
