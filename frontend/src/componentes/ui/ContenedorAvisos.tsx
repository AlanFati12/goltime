import { cn } from '@/lib/utils'

import { useAvisos } from './almacenAvisos'

export function ContenedorAvisos() {
  const { avisos, quitar } = useAvisos()
  return (
    <div
      className="fixed right-4 bottom-4 z-[60] flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2"
      aria-live="polite"
    >
      {avisos.map((a) => (
        <div
          key={a.id}
          role={a.tipo === 'error' ? 'alert' : 'status'}
          className={cn(
            'rounded-boton flex items-start justify-between gap-3 px-4 py-3 text-sm text-white shadow-lg',
            a.tipo === 'exito' ? 'bg-primario-oscuro' : 'bg-peligro',
          )}
        >
          <span>{a.texto}</span>
          <button
            type="button"
            onClick={() => quitar(a.id)}
            aria-label="Cerrar aviso"
            className="opacity-80 hover:opacity-100"
          >
            ×
          </button>
        </div>
      ))}
    </div>
  )
}
