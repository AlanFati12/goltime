import { avisar } from '@/componentes/ui/almacenAvisos'
import { mensajeDeError } from '@/lib/api'

import { useAccionesEspera, useMisEsperas } from './consultas'

/** En un horario ocupado: pide que le avisen al cliente si se libera. */
export function BotonEspera({
  canchaId,
  inicio,
  fin,
}: {
  canchaId: number
  inicio: string
  fin: string
}) {
  const esperas = useMisEsperas()
  const { unirse, salir } = useAccionesEspera()
  const actual = esperas.data?.find(
    (e) => e.canchaId === canchaId && new Date(e.inicio).getTime() === new Date(inicio).getTime(),
  )
  const error = (e: unknown) => avisar.error(mensajeDeError(e))

  if (actual) {
    return (
      <button
        type="button"
        className="text-primario mt-1 text-xs underline"
        onClick={() => salir.mutate(actual.id, { onError: error })}
      >
        {actual.estado === 'NOTIFICADA' ? '¡Se liberó! Quitar aviso' : 'Esperando · quitar aviso'}
      </button>
    )
  }
  return (
    <button
      type="button"
      className="text-primario mt-1 text-xs underline"
      disabled={unirse.isPending}
      onClick={() =>
        unirse.mutate(
          { canchaId, inicio, fin },
          {
            onSuccess: () => avisar.exito('Te avisaremos si este horario se libera.'),
            onError: error,
          },
        )
      }
    >
      Avisarme si se libera
    </button>
  )
}
