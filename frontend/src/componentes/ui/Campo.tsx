import { useId, type InputHTMLAttributes, type Ref, type TextareaHTMLAttributes } from 'react'

import { cn } from '@/lib/utils'

const estiloEntrada =
  'border-borde bg-superficie text-texto placeholder:text-texto-suave/70 focus-visible:outline-primario w-full rounded-boton border px-3 py-2 text-sm aria-[invalid=true]:border-peligro'

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
    <div className={cn('flex flex-col gap-1', className)}>
      <label htmlFor={id} className="text-sm font-medium">
        {etiqueta}
      </label>
      <input
        id={id}
        ref={ref}
        className={estiloEntrada}
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
    <div className={cn('flex flex-col gap-1', className)}>
      <label htmlFor={id} className="text-sm font-medium">
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
      <label htmlFor={id} className="flex items-start gap-2 text-sm">
        <input
          id={id}
          ref={ref}
          type="checkbox"
          className="accent-primario mt-0.5 size-4"
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
