import { Link } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
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
        <Tarjeta className="text-texto-suave text-center text-sm">
          Todavía no hay sedes disponibles.
        </Tarjeta>
      )}
      <div className="grid gap-5 md:grid-cols-2">
        {data?.map((s) => {
          const modalidades = [...new Set(s.canchas.map((c) => c.modalidad))]
          return (
            <Link key={s.id} to={`/sedes/${s.id}`}>
              <Tarjeta className="hover:border-primario flex h-full flex-col gap-3 p-4 transition-colors sm:p-4">
                <Galeria fotos={s.fotos.slice(0, 1)} />
                <h2 className="text-xl font-semibold">{s.nombre}</h2>
                <p className="text-texto-suave text-sm">
                  {s.direccion} · {s.ciudad}
                </p>
                <div className="flex flex-wrap gap-1">
                  {modalidades.map((m) => (
                    <Insignia key={m} tono="verde">
                      {m}
                    </Insignia>
                  ))}
                  <Insignia>{s.canchas.length} canchas</Insignia>
                </div>
              </Tarjeta>
            </Link>
          )
        })}
      </div>
    </div>
  )
}
