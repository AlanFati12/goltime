import { useRef } from 'react'

import { Boton } from '@/componentes/ui/Boton'
import { optimizarFoto } from '@/lib/imagenes'

import type { Foto } from './tipos'

interface PropsGestor {
  fotos: Foto[]
  subiendo?: boolean
  alSubir: (archivo: File) => void
  alQuitar: (foto: Foto) => void
}

/** Subida y borrado de fotos (JPG, PNG o WebP, hasta 10). Antes de subir se reducen a 1600 px y se recomprimen. */
export function GestorFotos({ fotos, subiendo, alSubir, alQuitar }: PropsGestor) {
  const entrada = useRef<HTMLInputElement>(null)
  return (
    <div className="flex flex-col gap-3">
      {fotos.length === 0 && <p className="text-texto-suave text-sm">Aún no hay fotos.</p>}
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {fotos.map((f) => (
          <li key={f.id} className="border-borde rounded-boton relative overflow-hidden border">
            <img src={f.url} alt="" loading="lazy" className="aspect-video w-full object-cover" />
            <button
              type="button"
              onClick={() => alQuitar(f)}
              aria-label="Quitar foto"
              className="absolute top-1 right-1 rounded-full bg-black/60 px-2 text-sm text-white hover:bg-black/80"
            >
              ×
            </button>
          </li>
        ))}
      </ul>
      <input
        ref={entrada}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        className="sr-only"
        aria-label="Elegir foto"
        onChange={async (e) => {
          const archivo = e.target.files?.[0]
          e.target.value = ''
          // Se reduce y recomprime en el navegador antes de subir (ver lib/imagenes.ts).
          if (archivo) alSubir(await optimizarFoto(archivo))
        }}
      />
      <Boton
        variante="secundario"
        className="self-start"
        disabled={subiendo || fotos.length >= 10}
        onClick={() => entrada.current?.click()}
      >
        {subiendo ? 'Subiendo…' : 'Agregar foto'}
      </Boton>
    </div>
  )
}
