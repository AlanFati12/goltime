import { useState } from 'react'
import { Link } from 'react-router-dom'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'

import { ModalCambioEstado } from '../ModalCambioEstado'
import { useEstado, useSedesAdmin } from '../consultas'
import type { Sede } from '../tipos'

export function PaginaSedesAdmin() {
  const { data, isPending, isError, error } = useSedesAdmin()
  const estado = useEstado()
  const [objetivo, setObjetivo] = useState<Sede | null>(null)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Sedes</h1>
        <EnlaceBoton to="/admin/sedes/nueva">Nueva sede</EnlaceBoton>
      </div>
      {isPending && <Cargando texto="Cargando sedes…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <Tarjeta className="text-texto-suave text-center text-sm">
          Aún no hay sedes. Crea la primera.
        </Tarjeta>
      )}
      {data && data.length > 0 && (
        <Tarjeta className="overflow-x-auto p-0">
          <table className="w-full text-left text-sm">
            <thead className="border-borde text-texto-suave border-b text-xs uppercase">
              <tr>
                <th className="px-4 py-3">Sede</th>
                <th className="px-4 py-3">Canchas</th>
                <th className="px-4 py-3">Estado</th>
                <th className="px-4 py-3 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {data.map((s) => (
                <tr key={s.id} className="border-borde border-b last:border-0">
                  <td className="px-4 py-3">
                    <div className="font-medium">{s.nombre}</div>
                    <div className="text-texto-suave text-xs">
                      {s.direccion} · {s.ciudad}
                    </div>
                  </td>
                  <td className="px-4 py-3">{s.canchas.length}</td>
                  <td className="px-4 py-3">
                    {s.activa ? (
                      <Insignia tono="verde">Activa</Insignia>
                    ) : (
                      <Insignia tono="rojo">Inactiva</Insignia>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-2">
                      <Link to={`/admin/sedes/${s.id}`}>
                        <Boton tamano="sm" variante="secundario" tabIndex={-1}>
                          Editar
                        </Boton>
                      </Link>
                      <Boton tamano="sm" variante="secundario" onClick={() => setObjetivo(s)}>
                        {s.activa ? 'Desactivar' : 'Reactivar'}
                      </Boton>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Tarjeta>
      )}

      {objetivo && (
        <ModalCambioEstado
          abierto
          titulo={objetivo.activa ? 'Desactivar sede' : 'Reactivar sede'}
          descripcion={
            objetivo.activa
              ? `"${objetivo.nombre}" dejará de mostrarse a los clientes y no se podrá reservar en ninguna de sus canchas.`
              : `"${objetivo.nombre}" volverá a mostrarse a los clientes.`
          }
          requiereMotivo={objetivo.activa}
          textoConfirmar={objetivo.activa ? 'Desactivar' : 'Reactivar'}
          tono={objetivo.activa ? 'peligro' : 'primario'}
          previsualizar={() => estado.previsualizarSede(objetivo.id, false)}
          confirmar={async (motivo, confirmarConflictos) => {
            await estado.cambiarSede.mutateAsync({
              id: objetivo.id,
              activa: !objetivo.activa,
              motivo,
              confirmarConflictos,
            })
            avisar.exito(objetivo.activa ? 'Sede desactivada.' : 'Sede reactivada.')
          }}
          alCerrar={() => setObjetivo(null)}
        />
      )}
    </div>
  )
}
