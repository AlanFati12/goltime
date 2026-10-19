import { useId, type InputHTMLAttributes, type Ref, type TextareaHTMLAttributes } from 'react'

import { cn } from '@/lib/utils'

export const estiloEntrada =
  'border-borde bg-superficie text-texto placeholder:text-texto-suave/70 w-full rounded-boton border px-3.5 py-2 text-[0.95rem] transition-[border-color,box-shadow] duration-200 hover:border-texto-suave/40 focus-visible:border-primario focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-volt/25 disabled:bg-tenue disabled:text-texto-suave aria-[invalid=true]:border-peligro aria-[invalid=true]:focus-visible:ring-peligro/15'

const estiloEtiqueta = 'text-texto text-sm font-semibold'

interface PropsBase {
  etiqueta: string
  error?: string
  ayuda?: string
}

export function Campo({
  etiqueta,
  error,
  ayuda,
  className,
  ref,
  ...resto
}: PropsBase & InputHTMLAttributes<HTMLInputElement> & { ref?: Ref<HTMLInputElement> }) {
  const id = useId()
  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      <label htmlFor={id} className={estiloEtiqueta}>
        {etiqueta}
      </label>
      <input
        id={id}
        ref={ref}
        className={cn(estiloEntrada, 'h-11')}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : ayuda ? `${id}-ayuda` : undefined}
        {...resto}
      />
      {ayuda && !error && (
        <p id={`${id}-ayuda`} className="text-texto-suave text-xs">
          {ayuda}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} role="alert" className="text-peligro text-xs">
          {error}
        </p>
      )}
    </div>
  )
}

export function CampoTexto({
  etiqueta,
  error,
  className,
  ref,
  ...resto
}: PropsBase & TextareaHTMLAttributes<HTMLTextAreaElement> & { ref?: Ref<HTMLTextAreaElement> }) {
  const id = useId()
  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      <label htmlFor={id} className={estiloEtiqueta}>
        {etiqueta}
      </label>
      <textarea
        id={id}
        ref={ref}
        rows={3}
        className={estiloEntrada}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        {...resto}
      />
      {error && (
        <p id={`${id}-error`} role="alert" className="text-peligro text-xs">
          {error}
        </p>
      )}
    </div>
  )
}

export function Casilla({
  etiqueta,
  error,
  ref,
  ...resto
}: { etiqueta: React.ReactNode; error?: string } & Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'type'
> & {
    ref?: Ref<HTMLInputElement>
  }) {
  const id = useId()
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="flex cursor-pointer items-start gap-2.5 text-sm">
        <input
          id={id}
          ref={ref}
          type="checkbox"
          className="accent-primario mt-0.5 size-4 cursor-pointer"
          {...resto}
        />
        <span>{etiqueta}</span>
      </label>
      {error && (
        <p role="alert" className="text-peligro text-xs">
          {error}
        </p>
      )}
    </div>
  )
}
