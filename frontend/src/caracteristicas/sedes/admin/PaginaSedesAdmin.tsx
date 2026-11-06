import { Building2, Plus } from 'lucide-react'
import { useState } from 'react'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { mensajeDeError } from '@/lib/api'

import { ModalCambioEstado } from '../ModalCambioEstado'
import { useEstado, useSedesAdmin } from '../consultas'
import type { Sede } from '../tipos'

export function PaginaSedesAdmin() {
  const { data, isPending, isError, error } = useSedesAdmin()
  const estado = useEstado()
  const [objetivo, setObjetivo] = useState<Sede | null>(null)

  return (
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Sedes"
        descripcion="Ubicaciones del negocio, con su horario, servicios y canchas."
        acciones={
          <EnlaceBoton to="/admin/sedes/nueva">
            <Plus aria-hidden />
            Nueva sede
          </EnlaceBoton>
        }
      />
      {isPending && <Cargando texto="Cargando sedes…" variante="tarjetas" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <EstadoVacio icono={Building2} titulo="Aún no hay sedes" descripcion="Crea la primera." />
      )}
      {data && data.length > 0 && (
        <Tarjeta className="overflow-x-auto p-0 sm:p-0">
          <table className="tabla">
            <thead>
              <tr>
                <th>Sede</th>
                <th>Canchas</th>
                <th>Estado</th>
                <th className="text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {data.map((s) => (
                <tr key={s.id}>
                  <td>
                    <div className="font-medium">{s.nombre}</div>
                    <div className="text-texto-suave text-xs">
                      {s.direccion} · {s.ciudad}
                    </div>
                  </td>
                  <td>{s.canchas.length}</td>
                  <td>
                    {s.activa ? (
                      <Insignia tono="verde">Activa</Insignia>
                    ) : (
                      <Insignia tono="rojo">Inactiva</Insignia>
                    )}
                  </td>
                  <td>
                    <div className="flex justify-end gap-2">
                      <EnlaceBoton to={`/admin/sedes/${s.id}`} tamano="sm" variante="secundario">
                        Editar
                      </EnlaceBoton>
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
