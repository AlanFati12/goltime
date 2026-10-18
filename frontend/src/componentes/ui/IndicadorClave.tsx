import { cn } from '@/lib/utils'
import { fortalezaClave } from '@/lib/validaciones'

const colores = ['bg-borde', 'bg-peligro', 'bg-alerta', 'bg-primario', 'bg-primario-oscuro']

/** Barra de fortaleza de la contraseña. */
export function IndicadorClave({ clave }: { clave: string }) {
  const { puntaje, etiqueta } = fortalezaClave(clave)
  if (!clave) return null
  return (
    <div aria-live="polite" className="flex items-center gap-2">
      <div className="flex flex-1 gap-1" aria-hidden>
        {[1, 2, 3, 4].map((i) => (
          <span
            key={i}
            className={cn(
              'h-1.5 flex-1 rounded-full',
              i <= puntaje ? colores[puntaje] : 'bg-borde',
            )}
          />
        ))}
      </div>
      <span className="text-texto-suave text-xs">{etiqueta}</span>
    </div>
  )
}
