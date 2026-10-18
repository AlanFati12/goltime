import { Plus, Trophy } from 'lucide-react'
import { useState } from 'react'

import { Alerta } from '@/componentes/ui/Alerta'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { mensajeDeError } from '@/lib/api'

import { useTorneos } from '../consultas'
import { ETIQUETA_ESTADO_TORNEO } from '../etiquetas'
import { TarjetaTorneo } from '../PaginaTorneos'
import type { EstadoTorneo } from '../tipos'

export function PaginaTorneosAdmin() {
  const [estado, setEstado] = useState<EstadoTorneo | ''>('')
  const { data, isPending, isError, error } = useTorneos(true, estado || undefined)
  return (
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Torneos"
        descripcion="Ligas y copas: inscripciones, fixture, resultados y posiciones."
        acciones={
          <EnlaceBoton to="/admin/torneos/nuevo">
            <Plus aria-hidden />
            Nuevo torneo
          </EnlaceBoton>
        }
      />
      <Tarjeta className="flex items-end gap-4 p-4 sm:p-4">
        <CampoSelect
          className="min-w-48"
          etiqueta="Estado"
          value={estado}
          onChange={(e) => setEstado(e.target.value as EstadoTorneo | '')}
        >
          <option value="">Todos</option>
          {(Object.keys(ETIQUETA_ESTADO_TORNEO) as EstadoTorneo[]).map((e) => (
            <option key={e} value={e}>
              {ETIQUETA_ESTADO_TORNEO[e]}
            </option>
          ))}
        </CampoSelect>
      </Tarjeta>
      {isPending && <Cargando texto="Cargando torneos…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && <EstadoVacio icono={Trophy} titulo="No hay torneos" />}
      <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {data?.map((t) => (
          <li key={t.id}>
            <TarjetaTorneo t={t} ruta={`/admin/torneos/${t.id}`} />
          </li>
        ))}
      </ul>
    </div>
  )
}
