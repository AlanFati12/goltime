import { ChevronDown } from 'lucide-react'
import { useId, type Ref, type SelectHTMLAttributes } from 'react'

import { cn } from '@/lib/utils'

import { estiloEntrada } from './Campo'

/** Selector con etiqueta y mensaje de error, con el mismo aspecto que {@link Campo}. */
export function CampoSelect({
  etiqueta,
  error,
  className,
  children,
  ref,
  ...resto
}: { etiqueta: string; error?: string } & SelectHTMLAttributes<HTMLSelectElement> & {
    ref?: Ref<HTMLSelectElement>
  }) {
  const id = useId()
  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      <label htmlFor={id} className="text-texto text-sm font-semibold">
        {etiqueta}
      </label>
      <div className="relative">
        <select
          id={id}
          ref={ref}
          aria-invalid={error ? true : undefined}
          className={cn(estiloEntrada, 'h-11 cursor-pointer appearance-none pr-9')}
          {...resto}
        >
          {children}
        </select>
        <ChevronDown
          className="text-texto-suave pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2"
          aria-hidden
        />
      </div>
      {error && (
        <p role="alert" className="text-peligro text-xs">
          {error}
        </p>
      )}
    </div>
  )
}
