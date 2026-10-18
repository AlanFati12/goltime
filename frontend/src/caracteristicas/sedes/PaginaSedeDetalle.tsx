import { MapPin, MessageCircle, Phone } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { Insignia } from '@/componentes/ui/Insignia'
import { Migas } from '@/componentes/ui/Migas'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { codigoDeError, mensajeDeError } from '@/lib/api'

import { Galeria } from './Galeria'
import { HorarioTabla } from './HorarioTabla'
import { TarjetaCancha } from './TarjetaCancha'
import { useSedePublica } from './consultas'

export function PaginaSedeDetalle() {
  const id = Number(useParams().id)
  const { data: sede, isPending, isError, error } = useSedePublica(id)

  if (isPending) return <Cargando texto="Cargando sede…" />
  if (isError) {
    return (
      <div className="flex flex-col gap-3">
        <Alerta tipo="error">
          {codigoDeError(error) === 'RECURSO_NO_ENCONTRADO'
            ? 'Esta sede no existe o no está disponible.'
            : mensajeDeError(error)}
        </Alerta>
        <Link to="/sedes" className="enlace text-sm">
          Ver todas las sedes
        </Link>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <Migas items={[{ texto: 'Sedes', a: '/sedes' }, { texto: sede.nombre }]} />
      <div className="grid gap-6 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <Galeria fotos={sede.fotos} />
        <Tarjeta className="flex flex-col gap-4">
          <div>
            <h1 className="text-2xl font-bold sm:text-[1.75rem]">{sede.nombre}</h1>
            <p className="text-texto-suave mt-1 flex items-center gap-1.5 text-sm">
              <MapPin className="size-4 shrink-0" aria-hidden />
              {sede.direccion} · {sede.ciudad}
            </p>
          </div>
          {sede.descripcion && <p className="text-texto-suave text-sm">{sede.descripcion}</p>}
          <div className="flex flex-wrap gap-1.5">
            {sede.servicios.map((s) => (
              <Insignia key={s} tono="verde">
                {s}
              </Insignia>
            ))}
          </div>
          {(sede.telefono || sede.whatsapp) && (
            <div className="flex flex-wrap gap-2">
              {sede.telefono && (
                <a
                  href={`tel:${sede.telefono}`}
                  className="border-borde hover:bg-tenue rounded-boton inline-flex items-center gap-2 border px-3 py-1.5 text-sm"
                >
                  <Phone className="text-texto-suave size-4" aria-hidden />
                  {sede.telefono}
                </a>
              )}
              {sede.whatsapp && (
                <a
                  className="border-borde hover:bg-tenue rounded-boton inline-flex items-center gap-2 border px-3 py-1.5 text-sm"
                  href={`https://wa.me/${sede.whatsapp.replace(/\D/g, '')}`}
                  target="_blank"
                  rel="noreferrer"
                >
                  <MessageCircle className="size-4 text-emerald-600" aria-hidden />
                  WhatsApp
                </a>
              )}
            </div>
          )}
          <div className="border-borde border-t pt-4">
            <h2 className="mb-2 text-sm font-semibold">Horario de atención</h2>
            <HorarioTabla dias={sede.horario} />
          </div>
        </Tarjeta>
      </div>
      <section aria-labelledby="titulo-canchas">
        <h2 id="titulo-canchas" className="mb-4 text-xl font-bold">
          Canchas <span className="text-texto-suave font-normal">({sede.canchas.length})</span>
        </h2>
        {sede.canchas.length === 0 && (
          <EstadoVacio titulo="Esta sede aún no tiene canchas disponibles" />
        )}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {sede.canchas.map((c) => (
            <TarjetaCancha key={c.id} cancha={c} />
          ))}
        </div>
      </section>
    </div>
  )
}
