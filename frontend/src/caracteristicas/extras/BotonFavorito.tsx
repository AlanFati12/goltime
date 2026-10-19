import { Heart } from 'lucide-react'

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
        'border-borde bg-superficie hover:bg-tenue presionable inline-flex size-10 items-center justify-center rounded-full border transition-colors duration-200',
        marcada ? 'text-peligro border-peligro/30' : 'text-texto-suave',
        className,
      )}
    >
      <Heart
        className={cn(
          'size-4 transition-transform duration-300',
          marcada && 'scale-110 fill-current',
        )}
        aria-hidden
      />
    </button>
  )
}
