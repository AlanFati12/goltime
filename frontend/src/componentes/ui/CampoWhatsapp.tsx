import { ChevronDown } from 'lucide-react'
import { useId, useState } from 'react'

import { INDICATIVOS, separarTelefono } from '@/lib/telefono'
import { cn } from '@/lib/utils'

import { estiloEntrada } from './Campo'

interface Props {
  etiqueta?: string
  /** Número completo con indicativo (+573001234567) o lo que haya guardado. */
  value: string
  onChange: (valor: string) => void
  onBlur?: () => void
  error?: string
  ayuda?: string
  className?: string
}

/**
 * Celular de WhatsApp: indicativo del país + número. Siempre entrega el número completo con
 * indicativo, para que el mismo celular no quede guardado de dos formas distintas.
 */
export function CampoWhatsapp({
  etiqueta = 'WhatsApp',
  value,
  onChange,
  onBlur,
  error,
  ayuda,
  className,
}: Props) {
  const id = useId()
  const separado = separarTelefono(value)
  // El indicativo elegido se recuerda aunque el número esté vacío; si el valor ya trae uno, manda ese.
  const [elegido, setIndicativo] = useState(separado.indicativo)
  const indicativo = value?.startsWith('+') ? separado.indicativo : elegido
  const numero = value ? separado.numero : ''
  // Lo que se lleva tecleado de un indicativo (+5, +59…) mientras aún no coincide con ningún país.
  const [borrador, setBorrador] = useState<string | null>(null)

  const emitir = (ind: string, num: string) => onChange(num ? `+${ind}${num}` : '')

  const alEscribir = (texto: string) => {
    // Si escriben o pegan el número con indicativo (+57 300…), se reconoce el país.
    if (texto.trim().startsWith('+')) {
      const digitos = texto.replace(/\D/g, '')
      const pegado = separarTelefono(texto)
      if (!digitos.startsWith(pegado.indicativo)) {
        setBorrador(texto.trim())
        onChange('')
        return
      }
      setBorrador(null)
      setIndicativo(pegado.indicativo)
      emitir(pegado.indicativo, pegado.numero.replace(/\D/g, ''))
      return
    }
    setBorrador(null)
    emitir(indicativo, texto.replace(/\D/g, ''))
  }

  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      <label htmlFor={id} className="text-texto text-sm font-semibold">
        {etiqueta}
      </label>
      <div className="flex gap-2">
        <div className="relative shrink-0">
          <select
            aria-label="Indicativo del país"
            value={indicativo}
            onChange={(e) => {
              setIndicativo(e.target.value)
              emitir(e.target.value, numero)
            }}
            className={cn(estiloEntrada, 'h-11 w-[6.5rem] cursor-pointer appearance-none pr-7')}
          >
            {INDICATIVOS.map((i) => (
              <option key={i.codigo} value={i.codigo} title={i.pais}>
                {i.bandera} +{i.codigo}
              </option>
            ))}
          </select>
          <ChevronDown
            className="text-texto-suave pointer-events-none absolute top-1/2 right-2 size-4 -translate-y-1/2"
            aria-hidden
          />
        </div>
        <input
          id={id}
          type="tel"
          inputMode="tel"
          autoComplete="tel-national"
          placeholder={indicativo === '57' ? '300 123 4567' : 'Número'}
          value={borrador ?? numero}
          onChange={(e) => alEscribir(e.target.value)}
          onBlur={onBlur}
          className={cn(estiloEntrada, 'h-11 min-w-0 flex-1')}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? `${id}-error` : ayuda ? `${id}-ayuda` : undefined}
        />
      </div>
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
