import { useParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { mensajeDeError } from '@/lib/api'

import { useDetallePartido } from './consultas'
import { EncabezadoPartido } from './EncabezadoPartido'

export function PaginaPartido() {
  const id = Number(useParams().id)
  const { data, isPending, isError, error } = useDetallePartido(id, false)
  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6">
      <h1 className="sr-only">Partido</h1>
      {isPending && <Cargando texto="Cargando partido…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && <EncabezadoPartido detalle={data} />}
    </div>
  )
}
