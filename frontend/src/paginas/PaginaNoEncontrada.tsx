import { Compass } from 'lucide-react'

import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'

export function PaginaNoEncontrada() {
  return (
    <section className="flex flex-col items-center gap-4 py-20 text-center">
      <span className="bg-tenue text-texto-suave flex size-14 items-center justify-center rounded-full">
        <Compass className="size-7" aria-hidden />
      </span>
      <h1 className="text-2xl font-bold sm:text-3xl">Página no encontrada</h1>
      <p className="text-texto-suave">La dirección que buscas no existe o fue movida.</p>
      <EnlaceBoton to="/" variante="secundario">
        Volver al inicio
      </EnlaceBoton>
    </section>
  )
}
