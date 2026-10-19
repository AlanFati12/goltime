import { CalendarPlus, Check, MapPin, MessageCircle, Phone } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { InsigniaFoto, PortadaPagina } from '@/componentes/ui/PortadaPagina'
import { Revelar } from '@/componentes/ui/Revelar'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { codigoDeError, mensajeDeError } from '@/lib/api'

import { Galeria } from './Galeria'
import { HorarioTabla } from './HorarioTabla'
import { TarjetaCancha } from './TarjetaCancha'
import { useSedePublica } from './consultas'
import { horarioDeHoy, modalidadesDe } from './horario'

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

  const hoy = horarioDeHoy(sede.horario)

  return (
    <div className="flex flex-col gap-12">
      <PortadaPagina
        titulo={sede.nombre}
        imagen={sede.fotos[0]?.url ?? '/img/cancha-aerea.webp'}
        alt={`Sede ${sede.nombre}`}
        volver={{ a: '/sedes', texto: 'Sedes' }}
        acciones={
          <EnlaceBoton to={`/reservar?sedeId=${sede.id}`} variante="volt" tamano="lg">
            <CalendarPlus aria-hidden />
            Reservar en esta sede
          </EnlaceBoton>
        }
      >
        <p className="flex items-center gap-1.5 text-lg">
          <MapPin className="size-5 shrink-0" aria-hidden />
          {sede.direccion} · {sede.ciudad}
        </p>
        <div className="flex flex-wrap gap-2">
          {modalidadesDe(sede).map((m) => (
            <InsigniaFoto key={m}>{m}</InsigniaFoto>
          ))}
          <InsigniaFoto>
            <span className={hoy.abiertaAhora ? 'text-volt-vivo' : undefined}>{hoy.texto}</span>
          </InsigniaFoto>
        </div>
      </PortadaPagina>

      <div className="grid gap-10 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <section aria-labelledby="titulo-canchas" className="flex flex-col gap-6">
          <div className="flex items-baseline justify-between gap-4">
            <h2 id="titulo-canchas" className="text-4xl font-extrabold uppercase">
              Canchas
            </h2>
            <span className="text-texto-suave font-semibold">
              {sede.canchas.length} {sede.canchas.length === 1 ? 'cancha' : 'canchas'}
            </span>
          </div>
          {sede.canchas.length === 0 && (
            <EstadoVacio titulo="Esta sede aún no tiene canchas disponibles" />
          )}
          <div className="grid gap-5 sm:grid-cols-2">
            {sede.canchas.map((c, i) => (
              <Revelar key={c.id} i={i}>
                <TarjetaCancha cancha={c} />
              </Revelar>
            ))}
          </div>
          {sede.fotos.length > 1 && (
            <div className="mt-6 flex flex-col gap-4">
              <h2 className="text-4xl font-extrabold uppercase">Fotos</h2>
              <Galeria fotos={sede.fotos} />
            </div>
          )}
        </section>

        <aside className="flex flex-col gap-5 lg:sticky lg:top-24 lg:self-start">
          {sede.descripcion && (
            <Tarjeta>
              <h2 className="text-xl font-extrabold uppercase">La sede</h2>
              <p className="text-texto-suave mt-2">{sede.descripcion}</p>
              {sede.servicios.length > 0 && (
                <ul className="mt-4 grid grid-cols-2 gap-2">
                  {sede.servicios.map((s) => (
                    <li key={s} className="flex items-center gap-2 text-sm font-semibold">
                      <span className="bg-volt-claro text-primario flex size-5 items-center justify-center rounded-full">
                        <Check className="size-3" aria-hidden />
                      </span>
                      {s}
                    </li>
                  ))}
                </ul>
              )}
            </Tarjeta>
          )}
          <Tarjeta>
            <h2 className="mb-3 text-xl font-extrabold uppercase">Horario</h2>
            <HorarioTabla dias={sede.horario} />
          </Tarjeta>
          {(sede.telefono || sede.whatsapp) && (
            <div className="bg-noche flex flex-col gap-3 rounded-[1.25rem] p-5 text-white">
              <h2 className="text-xl font-extrabold uppercase">¿Preguntas?</h2>
              {sede.whatsapp && (
                <a
                  className="bg-volt text-noche presionable inline-flex items-center justify-center gap-2 rounded-full px-5 py-3 font-semibold"
                  href={`https://wa.me/${sede.whatsapp.replace(/\D/g, '')}`}
                  target="_blank"
                  rel="noreferrer"
                >
                  <MessageCircle className="size-4" aria-hidden />
                  Escribir por WhatsApp
                </a>
              )}
              {sede.telefono && (
                <a
                  href={`tel:${sede.telefono}`}
                  className="presionable inline-flex items-center justify-center gap-2 rounded-full border border-white/25 px-5 py-3 font-semibold hover:bg-white/10"
                >
                  <Phone className="size-4" aria-hidden />
                  {sede.telefono}
                </a>
              )}
            </div>
          )}
        </aside>
      </div>
    </div>
  )
}
