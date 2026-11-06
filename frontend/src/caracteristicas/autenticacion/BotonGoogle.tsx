import { useEffect, useRef, useState } from 'react'

import { Alerta } from '@/componentes/ui/Alerta'

/** Parte de la librería «Google Identity Services» que se usa aquí. */
interface IdentidadGoogle {
  accounts: {
    id: {
      initialize: (config: {
        client_id: string
        callback: (respuesta: { credential: string }) => void
        ux_mode?: 'popup' | 'redirect'
        auto_select?: boolean
      }) => void
      renderButton: (elemento: HTMLElement, opciones: Record<string, unknown>) => void
    }
  }
}

declare global {
  interface Window {
    google?: IdentidadGoogle
  }
}

const URL_LIBRERIA = 'https://accounts.google.com/gsi/client'
let promesaLibreria: Promise<void> | null = null

/** Descarga la librería de Google una sola vez; si no se puede (bloqueador, sin internet), falla. */
function cargarLibreria(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve()
  promesaLibreria ??= new Promise<void>((resolver, rechazar) => {
    const script = document.createElement('script')
    script.src = URL_LIBRERIA
    script.async = true
    script.onload = () => resolver()
    script.onerror = () => {
      promesaLibreria = null
      script.remove()
      rechazar(new Error('No se pudo cargar Google'))
    }
    document.head.appendChild(script)
  })
  return promesaLibreria
}

/**
 * El botón oficial «Continuar con Google». Al elegir una cuenta, Google devuelve un token de identidad firmado que
 * se entrega a `alCredencial`; el servidor lo verifica. La librería de Google se descarga solo en esta pantalla.
 */
export function BotonGoogle({
  clientId,
  alCredencial,
}: {
  clientId: string
  alCredencial: (credencial: string) => void
}) {
  const contenedor = useRef<HTMLDivElement>(null)
  const [falla, setFalla] = useState(false)
  // El botón se dibuja una sola vez; el callback siempre llama a la versión más reciente.
  const ultimoCallback = useRef(alCredencial)
  useEffect(() => {
    ultimoCallback.current = alCredencial
  }, [alCredencial])

  useEffect(() => {
    let activo = true
    cargarLibreria()
      .then(() => {
        const google = window.google
        if (!activo || !google || !contenedor.current) return
        google.accounts.id.initialize({
          client_id: clientId,
          callback: (r) => ultimoCallback.current(r.credential),
          ux_mode: 'popup',
          auto_select: false,
        })
        google.accounts.id.renderButton(contenedor.current, {
          theme: 'outline',
          size: 'large',
          shape: 'pill',
          text: 'continue_with',
          logo_alignment: 'left',
          locale: 'es',
          width: Math.min(contenedor.current.clientWidth || 320, 400),
        })
      })
      .catch(() => activo && setFalla(true))
    return () => {
      activo = false
    }
  }, [clientId])

  if (falla) {
    return (
      <Alerta tipo="error">
        No pudimos cargar el ingreso con Google. Revisa tu conexión o desactiva el bloqueador de
        anuncios para esta página e intenta de nuevo.
      </Alerta>
    )
  }
  return <div ref={contenedor} className="flex min-h-11 w-full justify-center" />
}
