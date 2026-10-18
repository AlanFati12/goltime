import { useState } from 'react'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { ModalMotivo } from '@/componentes/ui/ModalMotivo'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearFechaHora } from '@/lib/formato'

import { InsigniaEstadoReserva } from '../InsigniaEstadoReserva'
import { useAccionesReservaAdmin, useReservasAdmin, type FiltrosReservasAdmin } from '../consultas'
import type { EstadoReserva, Reserva } from '../tipos'
import { ETIQUETA_ESTADO_RESERVA, rangoHorario } from '../utilidades'

type Accion = { tipo: 'rechazar' | 'cancelar'; reserva: Reserva }

export function PaginaReservasAdmin() {
  const [filtros, setFiltros] = useState<FiltrosReservasAdmin>({
    estado: 'PENDIENTE',
    busqueda: '',
    pagina: 0,
  })
  const [accion, setAccion] = useState<Accion | null>(null)
  const { data, isPending, isError, error, isFetching } = useReservasAdmin(filtros)
  const { aprobar, rechazar, cancelar } = useAccionesReservaAdmin()

  const cambiar = (parcial: Partial<FiltrosReservasAdmin>) =>
    setFiltros((f) => ({ ...f, pagina: 0, ...parcial }))

  const aprobarReserva = (r: Reserva) =>
    aprobar.mutate(r.id, {
      onSuccess: () => avisar.exito(`Reserva ${r.codigo} confirmada.`),
      onError: (e) => avisar.error(mensajeDeError(e)),
    })

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">Reservas</h1>

      <Tarjeta className="grid gap-3 sm:grid-cols-[2fr_1fr]">
        <Campo
          etiqueta="Buscar"
          type="search"
          placeholder="Código, nombre o correo del cliente"
          value={filtros.busqueda}
          onChange={(e) => cambiar({ busqueda: e.target.value })}
        />
        <CampoSelect
          etiqueta="Estado"
          value={filtros.estado}
          onChange={(e) => cambiar({ estado: e.target.value as EstadoReserva | '' })}
        >
          <option value="">Todos</option>
          {(Object.keys(ETIQUETA_ESTADO_RESERVA) as EstadoReserva[]).map((e) => (
            <option key={e} value={e}>
              {ETIQUETA_ESTADO_RESERVA[e]}
            </option>
          ))}
        </CampoSelect>
      </Tarjeta>

      {isPending && <Cargando texto="Cargando reservas…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.contenido.length === 0 && (
        <Tarjeta className="text-texto-suave text-center text-sm">
          {filtros.estado === 'PENDIENTE'
            ? 'No hay solicitudes pendientes. ¡Todo al día!'
            : 'No hay reservas con esos filtros.'}
        </Tarjeta>
      )}

      {data && data.contenido.length > 0 && (
        <Tarjeta className="overflow-x-auto p-0">
          <table className="w-full text-left text-sm">
            <thead className="border-borde text-texto-suave border-b text-xs uppercase">
              <tr>
                <th className="px-4 py-3">Reserva</th>
                <th className="px-4 py-3">Cliente</th>
                <th className="px-4 py-3">Horario</th>
                <th className="px-4 py-3">Valor</th>
                <th className="px-4 py-3">Estado</th>
                <th className="px-4 py-3 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody className={isFetching ? 'opacity-60' : undefined}>
              {data.contenido.map((r) => (
                <tr key={r.id} className="border-borde border-b align-top last:border-0">
                  <td className="px-4 py-3">
                    <div className="font-mono font-medium">{r.codigo}</div>
                    <div className="text-texto-suave text-xs">
                      {r.sede.nombre} · {r.cancha.nombre}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <div>{r.cliente.nombre}</div>
                    <div className="text-texto-suave text-xs">
                      {r.cliente.telefono ?? r.cliente.correo}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <div className="capitalize">{formatearFechaHora(r.inicio)}</div>
                    <div className="text-texto-suave text-xs">{rangoHorario(r.inicio, r.fin)}</div>
                  </td>
                  <td className="px-4 py-3">{formatearCOP(r.precioFinal)}</td>
                  <td className="px-4 py-3">
                    <InsigniaEstadoReserva estado={r.estado} />
                    {r.estado === 'PENDIENTE' && r.expiraEn && (
                      <div className="text-texto-suave mt-1 text-xs">
                        Expira {formatearFechaHora(r.expiraEn)}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      {r.estado === 'PENDIENTE' && (
                        <>
                          <Boton
                            tamano="sm"
                            disabled={aprobar.isPending}
                            onClick={() => aprobarReserva(r)}
                          >
                            Aprobar
                          </Boton>
                          <Boton
                            tamano="sm"
                            variante="secundario"
                            onClick={() => setAccion({ tipo: 'rechazar', reserva: r })}
                          >
                            Rechazar
                          </Boton>
                        </>
                      )}
                      {r.estado === 'CONFIRMADA' && r.puedeCancelar && (
                        <Boton
                          tamano="sm"
                          variante="secundario"
                          onClick={() => setAccion({ tipo: 'cancelar', reserva: r })}
                        >
                          Cancelar
                        </Boton>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Tarjeta>
      )}

      {data && data.totalPaginas > 1 && (
        <div className="flex items-center justify-between text-sm">
          <span className="text-texto-suave">
            Página {data.pagina + 1} de {data.totalPaginas} · {data.totalElementos} reservas
          </span>
          <div className="flex gap-2">
            <Boton
              tamano="sm"
              variante="secundario"
              disabled={data.pagina === 0}
              onClick={() => setFiltros((f) => ({ ...f, pagina: f.pagina - 1 }))}
            >
              Anterior
            </Boton>
            <Boton
              tamano="sm"
              variante="secundario"
              disabled={data.pagina + 1 >= data.totalPaginas}
              onClick={() => setFiltros((f) => ({ ...f, pagina: f.pagina + 1 }))}
            >
              Siguiente
            </Boton>
          </div>
        </div>
      )}

      {accion && (
        <ModalMotivo
          titulo={accion.tipo === 'rechazar' ? 'Rechazar solicitud' : 'Cancelar reserva'}
          descripcion={
            accion.tipo === 'rechazar'
              ? `Se liberará el horario de ${accion.reserva.codigo} y se avisará a ${accion.reserva.cliente.nombre}.`
              : `Se cancelará ${accion.reserva.codigo} y se avisará a ${accion.reserva.cliente.nombre}.`
          }
          textoConfirmar={accion.tipo === 'rechazar' ? 'Rechazar' : 'Cancelar reserva'}
          alCerrar={() => setAccion(null)}
          alConfirmar={async (motivo) => {
            try {
              const mutacion = accion.tipo === 'rechazar' ? rechazar : cancelar
              await mutacion.mutateAsync({ id: accion.reserva.id, motivo })
              avisar.exito(
                accion.tipo === 'rechazar' ? 'Solicitud rechazada.' : 'Reserva cancelada.',
              )
              setAccion(null)
            } catch (e) {
              avisar.error(mensajeDeError(e))
            }
          }}
        />
      )}
    </div>
  )
}
