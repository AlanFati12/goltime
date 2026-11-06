import { CalendarCheck, Gift, Trophy } from 'lucide-react'
import type { ReactNode } from 'react'

import { cn } from '@/lib/utils'
import { ImagenSitio } from '@/componentes/ui/ImagenSitio'

const VENTAJAS = [
  { icono: CalendarCheck, texto: 'Reserva en línea y sigue el estado de tus solicitudes.' },
  { icono: Trophy, texto: 'Inscribe a tu equipo en los torneos de la sede.' },
  { icono: Gift, texto: 'Acumula partidos y gana premios para tus reservas.' },
]

/**
 * Marco común de las pantallas de acceso: a la izquierda la foto del reflector con lo que se gana
 * al tener cuenta, a la derecha el formulario. En móvil la foto queda como franja superior.
 */
export function TarjetaAcceso({
  titulo,
  descripcion,
  ancho = 'md',
  pie,
  children,
}: {
  titulo?: string
  descripcion?: ReactNode
  ancho?: 'md' | 'lg'
  pie?: ReactNode
  children: ReactNode
}) {
  return (
    <div className="grid min-h-[calc(100svh-72px)] lg:grid-cols-[1fr_1.05fr]">
      <aside className="bg-noche relative isolate flex min-h-56 flex-col justify-end overflow-hidden p-6 text-white sm:p-10 lg:min-h-0 lg:p-14">
        <ImagenSitio
          src="/img/reflector.webp"
          alt=""
          prioridad
          className="animate-acercar absolute inset-0 -z-10 size-full object-cover opacity-80 motion-reduce:animate-none"
        />
        <div className="from-noche via-noche/70 absolute inset-0 -z-10 bg-gradient-to-t to-transparent" />
        <p className="font-titulo animate-subir max-w-md text-[clamp(2.6rem,5vw,4.75rem)] leading-[0.88] font-extrabold uppercase [animation-delay:150ms]">
          Tu partido
          <br />
          <span className="text-volt-vivo">empieza aquí</span>
        </p>
        <ul className="mt-8 hidden flex-col gap-4 lg:flex">
          {VENTAJAS.map((v, i) => (
            <li
              key={v.texto}
              className="animate-subir flex items-center gap-3 text-white/80"
              style={{ animationDelay: `${300 + i * 90}ms` }}
            >
              <span className="bg-volt/15 text-volt-vivo flex size-9 shrink-0 items-center justify-center rounded-full">
                <v.icono className="size-4" aria-hidden />
              </span>
              {v.texto}
            </li>
          ))}
        </ul>
      </aside>

      <div className="flex items-center justify-center px-4 py-12 sm:px-8 lg:py-16">
        <div
          className={cn(
            'animate-subir flex w-full flex-col gap-7',
            ancho === 'lg' ? 'max-w-xl' : 'max-w-md',
          )}
        >
          {titulo && (
            <div>
              <h1 className="text-[2.6rem] leading-[0.95] font-extrabold uppercase">{titulo}</h1>
              {descripcion && <p className="text-texto-suave mt-2">{descripcion}</p>}
            </div>
          )}
          {children}
          {pie && <p className="text-texto-suave text-sm">{pie}</p>}
        </div>
      </div>
    </div>
  )
}
