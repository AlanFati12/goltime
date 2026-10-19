import { CircleCheck, Download, Plus, SearchX } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'

import { useSedesAdmin } from '@/caracteristicas/sedes/consultas'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Insignia } from '@/componentes/ui/Insignia'
import { Plegable } from '@/componentes/ui/Plegable'
import { ModalMotivo } from '@/componentes/ui/ModalMotivo'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { Paginacion } from '@/componentes/ui/Paginacion'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearFechaHora } from '@/lib/formato'

import { InsigniaEstadoReserva } from '../InsigniaEstadoReserva'
import {
  FILTROS_RESERVAS_INICIALES,
  paramsDeFiltros,
  useAccionesReservaAdmin,
  useReservasAdmin,
  type FiltrosReservasAdmin,
} from '../consultas'
import type { EstadoReserva, Reserva } from '../tipos'
import { ETIQUETA_ESTADO_RESERVA, ETIQUETA_PAGO, rangoHorario } from '../utilidades'
import { exportarReservasCsv } from './consultas'

type Accion = { tipo: 'rechazar' | 'cancelar'; reserva: Reserva }

export function PaginaReservasAdmin() {
  const [filtros, setFiltros] = useState<FiltrosReservasAdmin>(FILTROS_RESERVAS_INICIALES)
  const [accion, setAccion] = useState<Accion | null>(null)
  const [exportando, setExportando] = useState(false)
  const sedes = useSedesAdmin()
  const { data, isPending, isError, error, isFetching } = useReservasAdmin(filtros)
  const { aprobar, rechazar, cancelar } = useAccionesReservaAdmin()

  const avanzados = [
    filtros.sedeId,
    filtros.estadoPago,
    filtros.origen,
    filtros.desde,
    filtros.hasta,
  ].filter(Boolean).length
  const hayFiltros = avanzados > 0 || !!filtros.busqueda || filtros.estado !== 'PENDIENTE'

  const cambiar = (parcial: Partial<FiltrosReservasAdmin>) =>
    setFiltros((f) => ({ ...f, pagina: 0, ...parcial }))

  const exportar = async () => {
    setExportando(true)
    try {
      await exportarReservasCsv(paramsDeFiltros(filtros))
    } catch (e) {
      avisar.error(mensajeDeError(e))
    } finally {
      setExportando(false)
    }
  }

  const aprobarReserva = (r: Reserva) =>
    aprobar.mutate(r.id, {
      onSuccess: () => avisar.exito(`Reserva ${r.codigo} confirmada.`),
      onError: (e) => avisar.error(mensajeDeError(e)),
    })

  return (
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Reservas"
        descripcion="Aprueba las solicitudes y consulta el historial."
        acciones={
          <>
            <Boton variante="secundario" onClick={exportar} disabled={exportando}>
              <Download aria-hidden />
              {exportando ? 'Exportando…' : 'Exportar CSV'}
            </Boton>
            <EnlaceBoton to="/admin/reservas/nueva">
              <Plus aria-hidden />
              Nueva reserva
            </EnlaceBoton>
          </>
        }
      />

      <Tarjeta className="flex flex-col gap-4 p-4 sm:p-4">
        <div className="flex flex-wrap items-end gap-3">
          <Campo
            className="min-w-56 flex-1"
            etiqueta="Buscar"
            type="search"
            placeholder="Código, nombre o correo del cliente"
            value={filtros.busqueda}
            onChange={(e) => cambiar({ busqueda: e.target.value })}
          />
          <CampoSelect
            className="w-56"
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
        </div>
        <div className="flex items-center justify-between gap-3">
          <Plegable
            titulo="Más filtros"
            resumen={avanzados > 0 ? `(${avanzados} activos)` : undefined}
            abiertoInicial={avanzados > 0}
          >
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
              <CampoSelect
                etiqueta="Sede"
                value={filtros.sedeId}
                onChange={(e) => cambiar({ sedeId: e.target.value })}
              >
                <option value="">Todas</option>
                {sedes.data?.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.nombre}
                  </option>
                ))}
              </CampoSelect>
              <CampoSelect
                etiqueta="Estado de pago"
                value={filtros.estadoPago}
                onChange={(e) => cambiar({ estadoPago: e.target.value })}
              >
                <option value="">Todos</option>
                {Object.entries(ETIQUETA_PAGO).map(([valor, texto]) => (
                  <option key={valor} value={valor}>
                    {texto}
                  </option>
                ))}
              </CampoSelect>
              <CampoSelect
                etiqueta="Origen"
                value={filtros.origen}
                onChange={(e) => cambiar({ origen: e.target.value })}
              >
                <option value="">Todos</option>
                <option value="WEB">Web</option>
                <option value="ADMIN">Administrador</option>
              </CampoSelect>
              <Campo
                etiqueta="Desde"
                type="date"
                value={filtros.desde}
                onChange={(e) => cambiar({ desde: e.target.value })}
              />
              <Campo
                etiqueta="Hasta"
                type="date"
                value={filtros.hasta}
                onChange={(e) => cambiar({ hasta: e.target.value })}
              />
            </div>
          </Plegable>
          {hayFiltros && (
            <button
              type="button"
              className="text-texto-suave hover:text-texto inline-flex shrink-0 items-center gap-1 self-start text-sm font-medium"
              onClick={() => setFiltros({ ...FILTROS_RESERVAS_INICIALES, estado: '' })}
            >
              Quitar filtros
            </button>
          )}
        </div>
      </Tarjeta>

      {isPending && <Cargando texto="Cargando reservas…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.contenido.length === 0 && (
        <EstadoVacio
          icono={filtros.estado === 'PENDIENTE' && !filtros.busqueda ? CircleCheck : SearchX}
          titulo={
            filtros.estado === 'PENDIENTE' && !filtros.busqueda
              ? 'No hay solicitudes pendientes. ¡Todo al día!'
              : 'No hay reservas con esos filtros.'
          }
        />
      )}

      {data && data.contenido.length > 0 && (
        <Tarjeta className="overflow-x-auto p-0 sm:p-0">
          <table className="tabla">
            <thead>
              <tr>
                <th>Reserva</th>
                <th>Cliente</th>
                <th>Horario</th>
                <th>Valor</th>
                <th>Estado</th>
                <th className="text-right">Acciones</th>
              </tr>
            </thead>
            <tbody className={isFetching ? 'opacity-60' : undefined}>
              {data.contenido.map((r) => (
                <tr key={r.id} className="align-top">
                  <td>
                    <Link
                      to={`/admin/reservas/${r.id}`}
                      className="hover:text-primario cifras font-semibold tracking-wide"
                    >
                      {r.codigo}
                    </Link>
                    <div className="text-texto-suave text-xs">
                      {r.sede.nombre} · {r.cancha.nombre}
                    </div>
                  </td>
                  <td>
                    <div>{r.cliente.nombre}</div>
                    <div className="text-texto-suave text-xs">
                      {r.cliente.telefono ?? r.cliente.correo}
                    </div>
                  </td>
                  <td>
                    <div className="capitalize">{formatearFechaHora(r.inicio)}</div>
                    <div className="text-texto-suave text-xs">{rangoHorario(r.inicio, r.fin)}</div>
                  </td>
                  <td>
                    <div>{formatearCOP(r.precioFinal)}</div>
                    <div className="text-texto-suave text-xs">
                      {ETIQUETA_PAGO[r.estadoPago] ?? r.estadoPago}
                    </div>
                  </td>
                  <td>
                    <InsigniaEstadoReserva estado={r.estado} />
                    {r.origen === 'ADMIN' && <Insignia className="ml-1">Admin</Insignia>}
                    {r.estado === 'PENDIENTE' && r.expiraEn && (
                      <div className="text-texto-suave mt-1 text-xs">
                        Expira {formatearFechaHora(r.expiraEn)}
                      </div>
                    )}
                  </td>
                  <td>
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
                      <EnlaceBoton to={`/admin/reservas/${r.id}`} tamano="sm" variante="fantasma">
                        Ver
                      </EnlaceBoton>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Tarjeta>
      )}

      {data && (
        <Paginacion
          pagina={data.pagina}
          totalPaginas={data.totalPaginas}
          totalElementos={data.totalElementos}
          unidad="reservas"
          alCambiar={(pagina) => setFiltros((f) => ({ ...f, pagina }))}
        />
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
