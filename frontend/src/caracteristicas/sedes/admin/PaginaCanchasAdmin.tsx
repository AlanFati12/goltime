import { useState } from 'react'
import { Link } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
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
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-2">
        <h1 className="text-2xl font-bold sm:text-3xl">Canchas</h1>
        {sedeParaNueva && (
          <EnlaceBoton to={`/admin/canchas/nueva?sedeId=${sedeParaNueva}`}>
            Nueva cancha
          </EnlaceBoton>
        )}
      </div>

      <Tarjeta className="sm:max-w-sm">
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

      {isPending && <Cargando texto="Cargando canchas…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <Tarjeta className="text-texto-suave text-center text-sm">
          No hay canchas en esta selección.
        </Tarjeta>
      )}
      {data && data.length > 0 && (
        <Tarjeta className="overflow-x-auto p-0">
          <table className="w-full text-left text-sm">
            <thead className="border-borde text-texto-suave border-b text-xs uppercase">
              <tr>
                <th className="px-4 py-3">Cancha</th>
                <th className="px-4 py-3">Sede</th>
                <th className="px-4 py-3">Precio base</th>
                <th className="px-4 py-3">Estado</th>
                <th className="px-4 py-3 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {data.map((c) => (
                <tr key={c.id} className="border-borde border-b last:border-0">
                  <td className="px-4 py-3">
                    <div className="font-medium">{c.nombre}</div>
                    <div className="mt-1 flex flex-wrap gap-1">
                      <Insignia tono="verde">{c.modalidad}</Insignia>
                      {c.techada && <Insignia>Techada</Insignia>}
                      {c.esCompuesta && <Insignia tono="ambar">Compuesta</Insignia>}
                    </div>
                  </td>
                  <td className="px-4 py-3">{c.sedeNombre}</td>
                  <td className="px-4 py-3">{formatearCOP(c.precioBaseHora)}</td>
                  <td className="px-4 py-3">
                    <Insignia tono={TONO[c.estado]}>{ETIQUETA_ESTADO[c.estado]}</Insignia>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Link to={`/admin/canchas/${c.id}`}>
                      <Boton tamano="sm" variante="secundario" tabIndex={-1}>
                        Editar
                      </Boton>
                    </Link>
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
