import { ArrowLeft } from 'lucide-react'

import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'

/** 404 con el lenguaje de la cancha: la página quedó en fuera de lugar. */
export function PaginaNoEncontrada() {
  return (
    <section className="bg-noche relative isolate flex min-h-[70svh] flex-col items-center justify-center gap-6 overflow-hidden rounded-[2rem] px-6 py-20 text-center text-white">
      <img
        src="/img/arco-niebla.webp"
        alt=""
        className="absolute inset-0 -z-10 size-full object-cover opacity-35"
      />
      <div className="from-noche via-noche/70 absolute inset-0 -z-10 bg-gradient-to-t to-transparent" />
      <p
        className="font-titulo text-volt-vivo animate-subir text-[clamp(7rem,22vw,15rem)] leading-[0.8] font-extrabold"
        aria-hidden
      >
        404
      </p>
      <h1 className="animate-subir text-5xl font-extrabold uppercase [animation-delay:120ms] sm:text-6xl">
        Fuera de lugar
      </h1>
      <p className="animate-subir max-w-md text-lg text-white/70 [animation-delay:200ms]">
        La página que buscas no existe o fue movida. El árbitro ya levantó la bandera.
      </p>
      <EnlaceBoton
        to="/"
        variante="volt"
        tamano="lg"
        className="animate-subir [animation-delay:280ms]"
      >
        <ArrowLeft aria-hidden />
        Volver al inicio
      </EnlaceBoton>
    </section>
  )
}
