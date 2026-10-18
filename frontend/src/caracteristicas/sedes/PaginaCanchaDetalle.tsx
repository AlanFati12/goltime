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
import { Insignia } from '@/componentes/ui/Insignia'
import { Migas } from '@/componentes/ui/Migas'
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
    <div className={ancho ? 'col-span-2 flex gap-2.5' : 'flex gap-2.5'}>
      <Icono className="text-texto-suave mt-0.5 size-4 shrink-0" aria-hidden />
      <div>
        <dt className="text-texto-suave text-xs">{etiqueta}</dt>
        <dd className="font-medium">{valor}</dd>
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
    <div className="flex flex-col gap-6">
      <Migas
        items={[
          { texto: 'Sedes', a: '/sedes' },
          { texto: c.sedeNombre, a: `/sedes/${c.sedeId}` },
          { texto: c.nombre },
        ]}
      />

      <div className="grid gap-6 lg:grid-cols-[3fr_2fr]">
        <Galeria fotos={c.fotos} />
        <Tarjeta className="flex flex-col gap-4">
          <div className="flex items-start justify-between gap-2">
            <div>
              <p className="text-texto-suave text-sm">{c.sedeNombre}</p>
              <h1 className="text-2xl font-bold sm:text-[1.75rem]">{c.nombre}</h1>
            </div>
            <span className="flex items-center gap-2">
              <Insignia tono="verde">{c.modalidad}</Insignia>
              <BotonFavorito canchaId={c.id} />
            </span>
          </div>
          {c.descripcion && <p className="text-texto-suave text-sm">{c.descripcion}</p>}
          <div className="flex items-baseline gap-1.5">
            <span className="font-titulo text-2xl font-bold">{formatearCOP(c.precioBaseHora)}</span>
            <span className="text-texto-suave text-sm">/ hora (precio base)</span>
          </div>
          <dl className="bg-tenue/60 rounded-boton grid grid-cols-2 gap-x-4 gap-y-3 p-4 text-sm">
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
          <EnlaceBoton
            tamano="lg"
            to={`/reservar?sedeId=${c.sedeId}&canchaId=${c.id}&modalidad=${c.modalidad}`}
          >
            <CalendarPlus aria-hidden />
            Reservar esta cancha
          </EnlaceBoton>
          {c.esCompuesta && (
            <Alerta tipo="info">
              Esta cancha se forma con {c.componentes.map((x) => x.nombre).join(' y ')}. Si alguna
              está ocupada, no estará disponible.
            </Alerta>
          )}
        </Tarjeta>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Cotizador cancha={c} />
        <div className="flex flex-col gap-6">
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
                    <strong>{formatearCOP(t.precioHora)}</strong>
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
      </div>
      <section aria-label="Calificaciones" className="flex flex-col gap-2">
        <h2 className="text-xl font-bold">Calificaciones</h2>
        <ResumenCalificaciones canchaId={c.id} />
      </section>
    </div>
  )
}
