import { ArrowRight, MapPin } from 'lucide-react'
import { Link } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'

import { Galeria } from './Galeria'
import { useSedesPublicas } from './consultas'

export function PaginaSedes() {
  const { data, isPending, isError, error } = useSedesPublicas()

  return (
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Sedes y canchas"
        descripcion="Elige una sede para ver sus canchas, horarios y precios."
      />
      {isPending && <Cargando texto="Cargando sedes…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <EstadoVacio icono={MapPin} titulo="Todavía no hay sedes disponibles" />
      )}
      <div className="grid gap-5 md:grid-cols-2">
        {data?.map((s) => {
          const modalidades = [...new Set(s.canchas.map((c) => c.modalidad))]
          return (
            <Link key={s.id} to={`/sedes/${s.id}`} className="group">
              <Tarjeta className="flex h-full flex-col overflow-hidden p-0 transition-all group-hover:-translate-y-0.5 group-hover:shadow-md sm:p-0">
                <Galeria fotos={s.fotos.slice(0, 1)} className="rounded-none" />
                <div className="flex flex-1 flex-col gap-3 p-5">
                  <div className="flex items-start justify-between gap-3">
                    <h2 className="text-lg font-semibold">{s.nombre}</h2>
                    <ArrowRight
                      className="text-texto-suave group-hover:text-primario mt-1 size-4 shrink-0 transition-transform group-hover:translate-x-0.5"
                      aria-hidden
                    />
                  </div>
                  <p className="text-texto-suave flex items-center gap-1.5 text-sm">
                    <MapPin className="size-4 shrink-0" aria-hidden />
                    {s.direccion} · {s.ciudad}
                  </p>
                  <div className="mt-auto flex flex-wrap gap-1.5">
                    {modalidades.map((m) => (
                      <Insignia key={m} tono="verde">
                        {m}
                      </Insignia>
                    ))}
                    <Insignia>{s.canchas.length} canchas</Insignia>
                  </div>
                </div>
              </Tarjeta>
            </Link>
          )
        })}
      </div>
    </div>
  )
}
