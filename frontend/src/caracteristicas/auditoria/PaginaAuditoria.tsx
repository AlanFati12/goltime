import { useState } from 'react'

import {
  useAuditoria,
  useFiltrosAuditoria,
  type FiltrosAuditoria,
} from '@/caracteristicas/reservas/admin/consultas'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearFechaHora } from '@/lib/formato'

const INICIALES: FiltrosAuditoria = { entidad: '', accion: '', desde: '', hasta: '', pagina: 0 }

/** "CREAR_SEDE" -> "Crear sede". */
function legible(accion: string) {
  const texto = accion.toLowerCase().replaceAll('_', ' ')
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

export function PaginaAuditoria() {
  const [filtros, setFiltros] = useState<FiltrosAuditoria>(INICIALES)
  const [abierto, setAbierto] = useState<number | null>(null)
  const opciones = useFiltrosAuditoria()
  const { data, isPending, isError, error, isFetching } = useAuditoria(filtros)

  const cambiar = (parcial: Partial<FiltrosAuditoria>) =>
    setFiltros((f) => ({ ...f, pagina: 0, ...parcial }))

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-2xl font-bold sm:text-3xl">Auditoría</h1>
        <p className="text-texto-suave text-sm">
          Quién hizo qué sobre cualquier dato del negocio, de lo más reciente a lo más antiguo.
        </p>
      </div>

      <Tarjeta className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
        <CampoSelect
          etiqueta="Entidad"
          value={filtros.entidad}
          onChange={(e) => cambiar({ entidad: e.target.value })}
        >
          <option value="">Todas</option>
          {opciones.data?.entidades.map((e) => (
            <option key={e}>{e}</option>
          ))}
        </CampoSelect>
        <CampoSelect
          etiqueta="Acción"
          value={filtros.accion}
          onChange={(e) => cambiar({ accion: e.target.value })}
        >
          <option value="">Todas</option>
          {opciones.data?.acciones.map((a) => (
            <option key={a} value={a}>
              {legible(a)}
            </option>
          ))}
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
        <div className="flex items-end">
          <Boton variante="secundario" onClick={() => setFiltros(INICIALES)}>
            Limpiar filtros
          </Boton>
        </div>
      </Tarjeta>

      {isPending && <Cargando texto="Cargando bitácora…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.contenido.length === 0 && (
        <Tarjeta className="text-texto-suave text-center text-sm">
          No hay registros con esos filtros.
        </Tarjeta>
      )}

      {data && data.contenido.length > 0 && (
        <Tarjeta className={isFetching ? 'overflow-x-auto p-0 opacity-60' : 'overflow-x-auto p-0'}>
          <table className="w-full text-left text-sm">
            <thead className="border-borde text-texto-suave border-b text-xs uppercase">
              <tr>
                <th className="px-4 py-3">Cuándo</th>
                <th className="px-4 py-3">Quién</th>
                <th className="px-4 py-3">Qué</th>
                <th className="px-4 py-3">Motivo</th>
                <th className="px-4 py-3 text-right">Detalle</th>
              </tr>
            </thead>
            <tbody>
              {data.contenido.map((r) => (
                <tr key={r.id} className="border-borde border-b align-top last:border-0">
                  <td className="px-4 py-3 whitespace-nowrap capitalize">
                    {formatearFechaHora(r.fecha)}
                  </td>
                  <td className="px-4 py-3">
                    <div>
                      {r.actorNombre ?? (r.actorRol === 'SISTEMA' ? 'Sistema' : 'Desconocido')}
                    </div>
                    <Insignia
                      tono={
                        r.actorRol === 'ADMIN'
                          ? 'verde'
                          : r.actorRol === 'SISTEMA'
                            ? 'gris'
                            : 'ambar'
                      }
                    >
                      {r.actorRol.toLowerCase()}
                    </Insignia>
                  </td>
                  <td className="px-4 py-3">
                    <div className="font-medium">{legible(r.accion)}</div>
                    <div className="text-texto-suave text-xs">
                      {r.entidad}
                      {r.entidadId ? ` #${r.entidadId}` : ''}
                    </div>
                  </td>
                  <td className="px-4 py-3">{r.motivo ?? '—'}</td>
                  <td className="px-4 py-3 text-right">
                    {r.cambios && (
                      <Boton
                        tamano="sm"
                        variante="fantasma"
                        aria-expanded={abierto === r.id}
                        onClick={() => setAbierto(abierto === r.id ? null : r.id)}
                      >
                        {abierto === r.id ? 'Ocultar' : 'Ver'}
                      </Boton>
                    )}
                    {abierto === r.id && (
                      <pre className="bg-fondo border-borde rounded-boton mt-2 max-w-72 overflow-x-auto border p-2 text-left text-xs">
                        {JSON.stringify(r.cambios, null, 2)}
                      </pre>
                    )}
                    {r.ip && abierto === r.id && (
                      <div className="text-texto-suave mt-1 text-xs">IP {r.ip}</div>
                    )}
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
            Página {data.pagina + 1} de {data.totalPaginas} · {data.totalElementos} registros
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
    </div>
  )
}
