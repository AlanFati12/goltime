import { Link, useParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
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
        <Link to="/sedes" className="text-primario text-sm underline">
          Ver todas las sedes
        </Link>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <nav aria-label="Ruta" className="text-texto-suave text-sm">
        <Link to="/sedes" className="hover:underline">
          Sedes
        </Link>{' '}
        / {sede.nombre}
      </nav>
      <div className="grid gap-6 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <Galeria fotos={sede.fotos} />
        <Tarjeta className="flex flex-col gap-4">
          <div>
            <h1 className="text-2xl font-bold sm:text-3xl">{sede.nombre}</h1>
            <p className="text-texto-suave text-sm">
              {sede.direccion} · {sede.ciudad}
            </p>
          </div>
          {sede.descripcion && <p className="text-sm">{sede.descripcion}</p>}
          <div className="flex flex-wrap gap-1">
            {sede.servicios.map((s) => (
              <Insignia key={s} tono="verde">
                {s}
              </Insignia>
            ))}
          </div>
          <div className="text-sm">
            {sede.telefono && <p>Teléfono: {sede.telefono}</p>}
            {sede.whatsapp && (
              <p>
                WhatsApp:{' '}
                <a
                  className="text-primario underline"
                  href={`https://wa.me/${sede.whatsapp.replace(/\D/g, '')}`}
                  target="_blank"
                  rel="noreferrer"
                >
                  {sede.whatsapp}
                </a>
              </p>
            )}
          </div>
          <div>
            <h2 className="mb-2 text-sm font-semibold">Horario de atención</h2>
            <HorarioTabla dias={sede.horario} />
          </div>
        </Tarjeta>
      </div>
      <section aria-labelledby="titulo-canchas">
        <h2 id="titulo-canchas" className="mb-3 text-xl font-semibold">
          Canchas
        </h2>
        {sede.canchas.length === 0 && (
          <Tarjeta className="text-texto-suave text-center text-sm">
            Esta sede aún no tiene canchas disponibles.
          </Tarjeta>
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
