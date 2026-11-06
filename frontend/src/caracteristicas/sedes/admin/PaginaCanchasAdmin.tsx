import { Flag, Plus } from 'lucide-react'
import { useState } from 'react'

import { Alerta } from '@/componentes/ui/Alerta'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP } from '@/lib/formato'

import { ETIQUETA_ESTADO } from '../constantes'
import { useCanchasAdmin, useSedesAdmin } from '../consultas'
import type { EstadoCancha } from '../tipos'

const TONO: Record<EstadoCancha, 'verde' | 'ambar' | 'rojo'> = {
  ACTIVA: 'verde',
  MANTENIMIENTO: 'ambar',
  INACTIVA: 'rojo',
}

export function PaginaCanchasAdmin() {
  const [sedeId, setSedeId] = useState<number | null>(null)
  const sedes = useSedesAdmin()
  const { data, isPending, isError, error } = useCanchasAdmin(sedeId)
  const sedeParaNueva = sedeId ?? sedes.data?.[0]?.id

  return (
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Canchas"
        descripcion="Modalidad, precio base, tarifas y estado de cada cancha."
        acciones={
          sedeParaNueva && (
            <EnlaceBoton to={`/admin/canchas/nueva?sedeId=${sedeParaNueva}`}>
              <Plus aria-hidden />
              Nueva cancha
            </EnlaceBoton>
          )
        }
      />

      <Tarjeta className="p-4 sm:max-w-sm sm:p-4">
        <CampoSelect
          etiqueta="Sede"
          value={sedeId ?? ''}
          onChange={(e) => setSedeId(e.target.value ? Number(e.target.value) : null)}
        >
          <option value="">Todas las sedes</option>
          {sedes.data?.map((s) => (
            <option key={s.id} value={s.id}>
              {s.nombre}
            </option>
          ))}
        </CampoSelect>
      </Tarjeta>

      {isPending && <Cargando texto="Cargando canchas…" variante="tarjetas" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && <EstadoVacio icono={Flag} titulo="No hay canchas en esta selección" />}
      {data && data.length > 0 && (
        <Tarjeta className="overflow-x-auto p-0 sm:p-0">
          <table className="tabla">
            <thead>
              <tr>
                <th>Cancha</th>
                <th>Sede</th>
                <th>Precio base</th>
                <th>Estado</th>
                <th className="text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {data.map((c) => (
                <tr key={c.id}>
                  <td>
                    <div className="font-medium">{c.nombre}</div>
                    <div className="mt-1 flex flex-wrap gap-1">
                      <Insignia tono="verde">{c.modalidad}</Insignia>
                      {c.techada && <Insignia>Techada</Insignia>}
                      {c.esCompuesta && <Insignia tono="ambar">Compuesta</Insignia>}
                    </div>
                  </td>
                  <td>{c.sedeNombre}</td>
                  <td>{formatearCOP(c.precioBaseHora)}</td>
                  <td>
                    <Insignia tono={TONO[c.estado]}>{ETIQUETA_ESTADO[c.estado]}</Insignia>
                  </td>
                  <td className="text-right">
                    <EnlaceBoton to={`/admin/canchas/${c.id}`} tamano="sm" variante="secundario">
                      Editar
                    </EnlaceBoton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Tarjeta>
      )}
    </div>
  )
}
