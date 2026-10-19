import {
  CalendarPlus,
  Layers,
  Lightbulb,
  Ruler,
  Timer,
  Umbrella,
  Users,
  UsersRound,
  type LucideIcon,
} from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { InsigniaFoto, PortadaPagina } from '@/componentes/ui/PortadaPagina'
import { Tarjeta, TituloTarjeta } from '@/componentes/ui/Tarjeta'
import { codigoDeError, mensajeDeError } from '@/lib/api'
import { formatearCOP } from '@/lib/formato'

import { BotonFavorito } from '@/caracteristicas/extras/BotonFavorito'
import { ResumenCalificaciones } from '@/caracteristicas/extras/ResumenCalificaciones'
import { Cotizador } from './Cotizador'
import { Galeria } from './Galeria'
import { HorarioTabla } from './HorarioTabla'
import { DIAS_CORTOS } from './constantes'
import { useCanchaPublica } from './consultas'

function Dato({
  icono: Icono,
  etiqueta,
  valor,
  ancho,
}: {
  icono: LucideIcon
  etiqueta: string
  valor: string
  ancho?: boolean
}) {
  return (
    <div className={ancho ? 'col-span-2 flex gap-3' : 'flex gap-3'}>
      <span className="bg-volt-claro text-primario flex size-9 shrink-0 items-center justify-center rounded-full">
        <Icono className="size-4" aria-hidden />
      </span>
      <div>
        <dt className="text-texto-suave text-xs font-semibold">{etiqueta}</dt>
        <dd className="font-semibold">{valor}</dd>
      </div>
    </div>
  )
}

export function PaginaCanchaDetalle() {
  const id = Number(useParams().id)
  const { data: c, isPending, isError, error } = useCanchaPublica(id)

  if (isPending) return <Cargando texto="Cargando cancha…" />
  if (isError) {
    return (
      <div className="flex flex-col gap-3">
        <Alerta tipo="error">
          {codigoDeError(error) === 'RECURSO_NO_ENCONTRADO'
            ? 'Esta cancha no existe o no está disponible.'
            : mensajeDeError(error)}
        </Alerta>
        <Link to="/sedes" className="enlace text-sm">
          Ver todas las sedes
        </Link>
      </div>
    )
  }

  const dimensiones = c.largoM && c.anchoM ? `${c.largoM} × ${c.anchoM} m` : null

  return (
    <div className="flex flex-col gap-10">
      <PortadaPagina
        titulo={c.nombre}
        imagen={c.fotos[0]?.url ?? '/img/cancha-aerea.webp'}
        alt={`Cancha ${c.nombre}`}
        volver={{ a: `/sedes/${c.sedeId}`, texto: c.sedeNombre }}
        acciones={
          <>
            <BotonFavorito canchaId={c.id} className="size-12" />
            <EnlaceBoton
              tamano="lg"
              variante="volt"
              to={`/reservar?sedeId=${c.sedeId}&canchaId=${c.id}&modalidad=${c.modalidad}`}
            >
              <CalendarPlus aria-hidden />
              Reservar esta cancha
            </EnlaceBoton>
          </>
        }
      >
        <div className="flex flex-wrap items-center gap-2">
          <InsigniaFoto>{c.modalidad}</InsigniaFoto>
          <InsigniaFoto>{c.sedeNombre}</InsigniaFoto>
        </div>
        <p className="text-lg">
          Desde{' '}
          <span className="font-titulo cifras text-volt-vivo text-4xl font-extrabold">
            {formatearCOP(c.precioBaseHora)}
          </span>{' '}
          / hora
        </p>
      </PortadaPagina>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_24rem]">
        <div className="flex flex-col gap-6">
          {c.fotos.length > 1 && <Galeria fotos={c.fotos} />}
          {c.descripcion && <p className="max-w-2xl text-lg">{c.descripcion}</p>}
          <dl className="bg-superficie border-borde/80 grid grid-cols-2 gap-x-4 gap-y-5 rounded-[1.25rem] border p-6 sm:grid-cols-3">
            <Dato icono={Users} etiqueta="Por equipo" valor={`${c.jugadoresPorEquipo} jugadores`} />
            <Dato icono={UsersRound} etiqueta="Capacidad" valor={`${c.capacidadTotal} personas`} />
            <Dato icono={Layers} etiqueta="Superficie" valor={c.superficie} />
            {dimensiones && <Dato icono={Ruler} etiqueta="Dimensiones" valor={dimensiones} />}
            <Dato icono={Umbrella} etiqueta="Techada" valor={c.techada ? 'Sí' : 'No'} />
            <Dato icono={Lightbulb} etiqueta="Iluminación" valor={c.iluminacion ? 'Sí' : 'No'} />
            <Dato
              icono={Timer}
              etiqueta="Reserva"
              valor={`Bloques de ${c.duracionBloqueMin} min (${c.duracionMinMin}–${c.duracionMaxMin})`}
              ancho
            />
          </dl>
          {c.esCompuesta && (
            <Alerta tipo="info">
              Esta cancha se forma con {c.componentes.map((x) => x.nombre).join(' y ')}. Si alguna
              está ocupada, no estará disponible.
            </Alerta>
          )}
        </div>
        <Cotizador cancha={c} />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Tarjeta>
          <TituloTarjeta
            titulo="Tarifas"
            descripcion={`Precio base: ${formatearCOP(c.precioBaseHora)} por hora`}
          />
          {c.tarifas.length > 0 ? (
            <ul className="divide-borde/70 flex flex-col divide-y text-sm">
              {c.tarifas.map((t) => (
                <li key={t.id} className="flex justify-between gap-2 py-2">
                  <span>
                    {t.aplicaFestivo
                      ? 'Festivos'
                      : t.diasSemana.map((d) => DIAS_CORTOS[d - 1]).join(', ')}{' '}
                    · {t.horaInicio}–{t.horaFin}
                  </span>
                  <strong className="cifras">{formatearCOP(t.precioHora)}</strong>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-texto-suave text-sm">
              Esta cancha no tiene franjas especiales: aplica el precio base.
            </p>
          )}
        </Tarjeta>
        <Tarjeta>
          <TituloTarjeta titulo="Horario" />
          <HorarioTabla dias={c.horarioEfectivo} />
        </Tarjeta>
      </div>
      <section aria-label="Calificaciones" className="flex flex-col gap-2">
        <h2 className="text-4xl font-extrabold uppercase">Calificaciones</h2>
        <ResumenCalificaciones canchaId={c.id} />
      </section>
    </div>
  )
}
