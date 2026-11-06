import { cn } from '@/lib/utils'

import { useBotWhatsapp } from './consultas'

/** El glifo de WhatsApp: globo de chat con un teléfono adentro. */
export function IconoWhatsapp({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden fill="none">
      <path
        d="M12 2.6a9.4 9.4 0 0 0-8 14.3L2.8 21.4l4.6-1.2A9.4 9.4 0 1 0 12 2.6z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
      <path
        transform="translate(6.1 6) scale(0.5)"
        fill="currentColor"
        d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"
      />
    </svg>
  )
}

/** Botón flotante para reservar y consultar por WhatsApp. No se muestra si el bot no está disponible. */
export function BotonWhatsapp({ className }: { className?: string }) {
  const { data } = useBotWhatsapp()
  if (!data?.disponible || !data.enlace) return null
  return (
    <a
      href={data.enlace}
      target="_blank"
      rel="noopener noreferrer"
      aria-label="Reservar y consultar por WhatsApp"
      className={cn(
        'group fixed right-4 bottom-4 z-30 flex items-center gap-2 rounded-full bg-[#25D366] p-3.5 text-white shadow-lg shadow-black/25 transition-transform duration-200 hover:scale-105 active:scale-95 sm:right-6 sm:bottom-6 print:hidden',
        className,
      )}
    >
      <IconoWhatsapp className="size-7" />
      <span className="hidden pr-1.5 text-sm font-bold sm:inline">Reserva por WhatsApp</span>
    </a>
  )
}
