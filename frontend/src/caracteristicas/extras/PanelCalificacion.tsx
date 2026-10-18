import { useState } from 'react'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Boton } from '@/componentes/ui/Boton'
import { CampoTexto } from '@/componentes/ui/Campo'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { cn } from '@/lib/utils'

import { useCalificar, useEstadoCalificacion } from './consultas'

/** Calificación de una reserva jugada: de 1 a 5 estrellas y un comentario opcional; se puede corregir. */
export function PanelCalificacion({ reservaId }: { reservaId: number }) {
  const estado = useEstadoCalificacion(reservaId)
  const calificar = useCalificar(reservaId)
  const actual = estado.data?.calificacion
  const [estrellas, setEstrellas] = useState<number | null>(null)
  const [comentario, setComentario] = useState<string | null>(null)
  if (!estado.data?.puedeCalificar) return null

  const valor = estrellas ?? actual?.estrellas ?? 0
  const texto = comentario ?? actual?.comentario ?? ''

  return (
    <Tarjeta className="flex flex-col gap-3 print:hidden">
      <h2 className="text-lg font-semibold">
        {actual ? 'Tu calificación' : '¿Cómo estuvo la cancha?'}
      </h2>
      <div role="radiogroup" aria-label="Calificación en estrellas" className="flex gap-1">
        {[1, 2, 3, 4, 5].map((i) => (
          <button
            key={i}
            type="button"
            role="radio"
            aria-checked={valor === i}
            aria-label={`${i} ${i === 1 ? 'estrella' : 'estrellas'}`}
            onClick={() => setEstrellas(i)}
            className={cn('text-3xl', i <= valor ? 'text-acento' : 'text-borde')}
          >
            ★
          </button>
        ))}
      </div>
      <CampoTexto
        etiqueta="Comentario (opcional)"
        value={texto}
        maxLength={500}
        onChange={(e) => setComentario(e.target.value)}
      />
      <div className="flex justify-end">
        <Boton
          disabled={valor === 0 || calificar.isPending}
          onClick={() =>
            calificar.mutate(
              { estrellas: valor, comentario: texto.trim() || undefined },
              {
                onSuccess: () => avisar.exito('¡Gracias por tu calificación!'),
                onError: (e) => avisar.error(mensajeDeError(e)),
              },
            )
          }
        >
          {actual ? 'Actualizar calificación' : 'Enviar calificación'}
        </Boton>
      </div>
    </Tarjeta>
  )
}
