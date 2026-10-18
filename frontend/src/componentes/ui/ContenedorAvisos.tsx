import { CircleAlert, CircleCheck, X } from 'lucide-react'

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
          className="border-borde bg-superficie rounded-tarjeta shadow-flotante flex items-start gap-3 border px-4 py-3 text-sm"
        >
          {a.tipo === 'exito' ? (
            <CircleCheck className="text-exito mt-px size-5 shrink-0" aria-hidden />
          ) : (
            <CircleAlert className="text-peligro mt-px size-5 shrink-0" aria-hidden />
          )}
          <span className="flex-1">{a.texto}</span>
          <button
            type="button"
            onClick={() => quitar(a.id)}
            aria-label="Cerrar aviso"
            className="text-texto-suave hover:text-texto"
          >
            <X className="size-4" aria-hidden />
          </button>
        </div>
      ))}
    </div>
  )
}
