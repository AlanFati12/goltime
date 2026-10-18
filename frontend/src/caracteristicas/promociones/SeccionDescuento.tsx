import { useEffect, useState } from 'react'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP } from '@/lib/formato'

import { useMisPremios, usePrevisualizarDescuento } from './consultas'

export interface SeleccionDescuento {
  codigo?: string
  premioId?: number
}

interface Props {
  canchaId: number
  inicio: string
  fin: string
  /** Informa qué código o premio enviar y si la selección es válida (un código inválido bloquea el envío). */
  alCambiar: (seleccion: SeleccionDescuento, valida: boolean) => void
}

/** Código de promoción y premios de fidelidad en la solicitud de reserva; el precio con descuento lo calcula el servidor. */
export function SeccionDescuento({ canchaId, inicio, fin, alCambiar }: Props) {
  const premios = useMisPremios()
  const [escrito, setEscrito] = useState('')
  const [codigo, setCodigo] = useState<string | undefined>()
  const [premioId, setPremioId] = useState<number | undefined>()
  const disponibles = premios.data?.premios.filter((p) => p.estado === 'DISPONIBLE') ?? []
  const vista = usePrevisualizarDescuento({ canchaId, inicio, fin, codigo, premioId })

  useEffect(() => {
    alCambiar({ codigo, premioId }, !vista.isError && !vista.isPending)
    // alCambiar es estable en el padre; solo reaccionamos a la selección y al resultado
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [codigo, premioId, vista.isError, vista.isPending])

  return (
    <div className="border-borde rounded-boton flex flex-col gap-3 border p-3">
      <p className="text-sm font-medium">¿Tienes un código o un premio?</p>
      <div className="flex items-end gap-2">
        <Campo
          className="flex-1"
          etiqueta="Código de promoción"
          value={escrito}
          onChange={(e) => setEscrito(e.target.value.toUpperCase())}
          maxLength={40}
        />
        <Boton
          variante="secundario"
          disabled={escrito.trim() === (codigo ?? '')}
          onClick={() => setCodigo(escrito.trim() || undefined)}
        >
          {escrito.trim() === '' && codigo ? 'Quitar' : 'Aplicar'}
        </Boton>
      </div>
      {disponibles.length > 0 && (
        <CampoSelect
          etiqueta="Usar un premio de fidelidad"
          value={premioId ?? ''}
          onChange={(e) => setPremioId(e.target.value ? Number(e.target.value) : undefined)}
        >
          <option value="">No usar premio</option>
          {disponibles.map((p) => (
            <option key={p.id} value={p.id}>
              {p.nombre} ({p.beneficio})
            </option>
          ))}
        </CampoSelect>
      )}
      <div aria-live="polite" className="text-sm">
        {vista.isError && <Alerta tipo="error">{mensajeDeError(vista.error)}</Alerta>}
        {vista.data && vista.data.descuento > 0 && (
          <p className="text-primario-oscuro font-medium">
            {vista.data.descripcion}: −{formatearCOP(vista.data.descuento)}. Pagarás{' '}
            <strong>{formatearCOP(vista.data.precioFinal)}</strong>.
          </p>
        )}
        <p className="text-texto-suave text-xs">
          Solo se aplica un descuento por reserva: el de mayor valor.
        </p>
      </div>
    </div>
  )
}
