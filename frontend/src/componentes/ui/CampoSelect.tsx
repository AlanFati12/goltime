import { useId, type Ref, type SelectHTMLAttributes } from 'react'

import { cn } from '@/lib/utils'

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
    <div className={cn('flex flex-col gap-1', className)}>
      <label htmlFor={id} className="text-sm font-medium">
        {etiqueta}
      </label>
      <select
        id={id}
        ref={ref}
        aria-invalid={error ? true : undefined}
        className="border-borde bg-superficie text-texto focus-visible:outline-primario rounded-boton w-full border px-3 py-2 text-sm"
        {...resto}
      >
        {children}
      </select>
      {error && (
        <p role="alert" className="text-peligro text-xs">
          {error}
        </p>
      )}
    </div>
  )
}
