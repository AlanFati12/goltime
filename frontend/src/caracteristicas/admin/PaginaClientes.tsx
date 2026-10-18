import { useState } from 'react'

import { nombreCompleto, type Usuario } from '@/caracteristicas/autenticacion/almacenSesion'
import { Alerta } from '@/componentes/ui/Alerta'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { Interruptor } from '@/componentes/ui/Interruptor'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { Paginacion } from '@/componentes/ui/Paginacion'
import { mensajeDeError } from '@/lib/api'
import { formatearFechaHora } from '@/lib/formato'

import { useAccionesCliente, useClientes, type FiltrosClientes } from './consultasAdmin'
import { ModalDesactivar } from './ModalDesactivar'

export function PaginaClientes() {
  const [filtros, setFiltros] = useState<FiltrosClientes>({
    busqueda: '',
    activo: 'todos',
    pagina: 0,
  })
  const [aDesactivar, setADesactivar] = useState<Usuario | null>(null)
  const { data, isPending, isError, error, isFetching } = useClientes(filtros)
  const { activar, inscripcion } = useAccionesCliente()

  const cambiarFiltro = (parcial: Partial<FiltrosClientes>) =>
    setFiltros((f) => ({ ...f, pagina: 0, ...parcial }))

  const reactivar = (c: Usuario) =>
    activar.mutate(c.id, {
      onSuccess: () => avisar.exito(`${nombreCompleto(c)} fue reactivado.`),
      onError: (e) => avisar.error(mensajeDeError(e)),
    })

  const alternarInscripcion = (c: Usuario, habilitado: boolean) =>
    inscripcion.mutate(
      { id: c.id, habilitado },
      {
        onSuccess: () =>
          avisar.exito(
            habilitado
              ? 'Inscripción de equipos habilitada.'
              : 'Inscripción de equipos deshabilitada.',
          ),
        onError: (e) => avisar.error(mensajeDeError(e)),
      },
    )

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold sm:text-3xl">Clientes</h1>

      <Tarjeta className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <label className="flex flex-1 flex-col gap-1 text-sm font-medium">
          Buscar
          <input
            type="search"
            value={filtros.busqueda}
            onChange={(e) => cambiarFiltro({ busqueda: e.target.value })}
            placeholder="Nombre, correo, celular o documento"
            className="border-borde bg-superficie rounded-boton border px-3 py-2 font-normal"
          />
        </label>
        <label className="flex flex-col gap-1 text-sm font-medium">
          Estado
          <select
            value={filtros.activo}
            onChange={(e) => cambiarFiltro({ activo: e.target.value as FiltrosClientes['activo'] })}
            className="border-borde bg-superficie rounded-boton border px-3 py-2 font-normal"
          >
            <option value="todos">Todos</option>
            <option value="activos">Activos</option>
            <option value="inactivos">Desactivados</option>
          </select>
        </label>
      </Tarjeta>

      {isPending && <Cargando texto="Cargando clientes…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && data.contenido.length === 0 && (
        <Tarjeta className="text-texto-suave text-center text-sm">
          No hay clientes que coincidan con la búsqueda.
        </Tarjeta>
      )}

      {data && data.contenido.length > 0 && (
        <Tarjeta className="overflow-x-auto p-0">
          <table className="w-full text-left text-sm">
            <thead className="border-borde text-texto-suave border-b text-xs uppercase">
              <tr>
                <th className="px-4 py-3">Cliente</th>
                <th className="px-4 py-3">Contacto</th>
                <th className="px-4 py-3">Estado</th>
                <th className="px-4 py-3">Inscribe equipos</th>
                <th className="px-4 py-3 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody className={isFetching ? 'opacity-60' : undefined}>
              {data.contenido.map((c) => (
                <tr key={c.id} className="border-borde border-b last:border-0">
                  <td className="px-4 py-3">
                    <div className="font-medium">{nombreCompleto(c)}</div>
                    <div className="text-texto-suave text-xs">
                      Registro: {formatearFechaHora(c.creadoEn)}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <div>{c.correo}</div>
                    <div className="text-texto-suave text-xs">{c.telefono ?? '—'}</div>
                  </td>
                  <td className="px-4 py-3">
                    {c.activo ? (
                      <Insignia tono="verde">Activo</Insignia>
                    ) : (
                      <div title={c.motivoDesactivacion ?? undefined}>
                        <Insignia tono="rojo">Desactivado</Insignia>
                        {c.motivoDesactivacion && (
                          <div className="text-texto-suave mt-1 max-w-48 truncate text-xs">
                            {c.motivoDesactivacion}
                          </div>
                        )}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <Interruptor
                      activo={c.puedeInscribirEquipos}
                      etiqueta={`Permitir a ${nombreCompleto(c)} inscribir equipos a torneos`}
                      deshabilitado={!c.activo || inscripcion.isPending}
                      alCambiar={(valor) => alternarInscripcion(c, valor)}
                    />
                  </td>
                  <td className="px-4 py-3 text-right">
                    {c.activo ? (
                      <Boton tamano="sm" variante="secundario" onClick={() => setADesactivar(c)}>
                        Desactivar
                      </Boton>
                    ) : (
                      <Boton
                        tamano="sm"
                        variante="secundario"
                        disabled={activar.isPending}
                        onClick={() => reactivar(c)}
                      >
                        Reactivar
                      </Boton>
                    )}
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
          unidad="clientes"
          alCambiar={(pagina) => setFiltros((f) => ({ ...f, pagina }))}
        />
      )}

      <ModalDesactivar cliente={aDesactivar} alCerrar={() => setADesactivar(null)} />
    </div>
  )
}
