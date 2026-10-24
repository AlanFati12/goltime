import { MessageCircleCheck, Users } from 'lucide-react'
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
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { Campo } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { mensajeDeError } from '@/lib/api'
import { formatearFechaHora } from '@/lib/formato'
import { formatearTelefono } from '@/lib/telefono'

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
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Clientes"
        descripcion="Busca clientes, desactiva cuentas y habilita la inscripción de equipos."
      />

      <Tarjeta className="flex flex-col gap-3 p-4 sm:flex-row sm:items-end sm:p-4">
        <Campo
          className="flex-1"
          etiqueta="Buscar"
          type="search"
          value={filtros.busqueda}
          onChange={(e) => cambiarFiltro({ busqueda: e.target.value })}
          placeholder="Nombre, correo, celular o documento"
        />
        <CampoSelect
          className="sm:w-48"
          etiqueta="Estado"
          value={filtros.activo}
          onChange={(e) => cambiarFiltro({ activo: e.target.value as FiltrosClientes['activo'] })}
        >
          <option value="todos">Todos</option>
          <option value="activos">Activos</option>
          <option value="inactivos">Desactivados</option>
        </CampoSelect>
      </Tarjeta>

      {isPending && <Cargando texto="Cargando clientes…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && data.contenido.length === 0 && (
        <EstadoVacio icono={Users} titulo="No hay clientes que coincidan con la búsqueda" />
      )}

      {data && data.contenido.length > 0 && (
        <Tarjeta className="overflow-x-auto p-0 sm:p-0">
          <table className="tabla">
            <thead>
              <tr>
                <th>Cliente</th>
                <th>Contacto</th>
                <th>Estado</th>
                <th>Inscribe equipos</th>
                <th className="text-right">Acciones</th>
              </tr>
            </thead>
            <tbody className={isFetching ? 'opacity-60' : undefined}>
              {data.contenido.map((c) => (
                <tr key={c.id}>
                  <td>
                    <div className="font-medium">{nombreCompleto(c)}</div>
                    <div className="text-texto-suave text-xs">
                      Registro: {formatearFechaHora(c.creadoEn)}
                    </div>
                  </td>
                  <td>
                    <div>{c.correo}</div>
                    <div className="text-texto-suave flex items-center gap-1.5 text-xs">
                      {c.telefono ? formatearTelefono(c.telefono) : '—'}
                      {c.telefono && c.telefonoVerificado && (
                        <span title="WhatsApp vinculado al bot">
                          <MessageCircleCheck
                            className="text-primario size-3.5"
                            aria-label="WhatsApp vinculado"
                          />
                        </span>
                      )}
                    </div>
                  </td>
                  <td>
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
                  <td>
                    <Interruptor
                      activo={c.puedeInscribirEquipos}
                      etiqueta={`Permitir a ${nombreCompleto(c)} inscribir equipos a torneos`}
                      deshabilitado={!c.activo || inscripcion.isPending}
                      alCambiar={(valor) => alternarInscripcion(c, valor)}
                    />
                  </td>
                  <td className="text-right">
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
