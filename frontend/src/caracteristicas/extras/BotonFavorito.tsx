import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { mensajeDeError } from '@/lib/api'
import { cn } from '@/lib/utils'

import { useAlternarFavorita, useFavoritas } from './consultas'

/** Corazón para marcar una cancha como favorita (solo clientes con sesión). */
export function BotonFavorito({ canchaId, className }: { canchaId: number; className?: string }) {
  const usuario = useSesion((s) => s.usuario)
  const esCliente = usuario?.rol === 'CLIENTE'
  const favoritas = useFavoritas(esCliente)
  const alternar = useAlternarFavorita()
  if (!esCliente) return null
  const marcada = favoritas.data?.some((f) => f.canchaId === canchaId) ?? false

  return (
    <button
      type="button"
      aria-pressed={marcada}
      aria-label={marcada ? 'Quitar de favoritas' : 'Marcar como favorita'}
      title={marcada ? 'Quitar de favoritas' : 'Marcar como favorita'}
      disabled={alternar.isPending}
      onClick={() =>
        alternar.mutate(
          { canchaId, marcar: !marcada },
          { onError: (e) => avisar.error(mensajeDeError(e)) },
        )
      }
      className={cn(
        'rounded-boton border-borde hover:bg-primario-claro inline-flex size-9 items-center justify-center border text-lg',
        marcada ? 'text-peligro' : 'text-texto-suave',
        className,
      )}
    >
      {marcada ? '♥' : '♡'}
    </button>
  )
}
