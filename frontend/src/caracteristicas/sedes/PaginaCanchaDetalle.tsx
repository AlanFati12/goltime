import { Link, useParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { codigoDeError, mensajeDeError } from '@/lib/api'
import { formatearCOP } from '@/lib/formato'

import { Cotizador } from './Cotizador'
import { Galeria } from './Galeria'
import { HorarioTabla } from './HorarioTabla'
import { DIAS_CORTOS } from './constantes'
import { useCanchaPublica } from './consultas'

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
        <Link to="/sedes" className="text-primario text-sm underline">
          Ver todas las sedes
        </Link>
      </div>
    )
  }

  const dimensiones = c.largoM && c.anchoM ? `${c.largoM} × ${c.anchoM} m` : null

  return (
    <div className="flex flex-col gap-6">
      <nav aria-label="Ruta" className="text-texto-suave text-sm">
        <Link to="/sedes" className="hover:underline">
          Sedes
        </Link>{' '}
        /{' '}
        <Link to={`/sedes/${c.sedeId}`} className="hover:underline">
          {c.sedeNombre}
        </Link>{' '}
        / {c.nombre}
      </nav>

      <div className="grid gap-6 lg:grid-cols-[3fr_2fr]">
        <Galeria fotos={c.fotos} />
        <Tarjeta className="flex flex-col gap-4">
          <div className="flex items-start justify-between gap-2">
            <h1 className="text-2xl font-bold">{c.nombre}</h1>
            <Insignia tono="verde">{c.modalidad}</Insignia>
          </div>
          {c.descripcion && <p className="text-sm">{c.descripcion}</p>}
          <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
            <dt className="text-texto-suave">Jugadores por equipo</dt>
            <dd>{c.jugadoresPorEquipo}</dd>
            <dt className="text-texto-suave">Capacidad</dt>
            <dd>{c.capacidadTotal} personas</dd>
            <dt className="text-texto-suave">Superficie</dt>
            <dd>{c.superficie}</dd>
            {dimensiones && (
              <>
                <dt className="text-texto-suave">Dimensiones</dt>
                <dd>{dimensiones}</dd>
              </>
            )}
            <dt className="text-texto-suave">Techada</dt>
            <dd>{c.techada ? 'Sí' : 'No'}</dd>
            <dt className="text-texto-suave">Iluminación</dt>
            <dd>{c.iluminacion ? 'Sí' : 'No'}</dd>
            <dt className="text-texto-suave">Reserva</dt>
            <dd>
              bloques de {c.duracionBloqueMin} min ({c.duracionMinMin}–{c.duracionMaxMin} min)
            </dd>
          </dl>
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
            <h2 className="mb-3 text-lg font-semibold">Tarifas</h2>
            <p className="mb-2 text-sm">
              Precio base: <strong>{formatearCOP(c.precioBaseHora)}</strong> por hora
            </p>
            {c.tarifas.length > 0 ? (
              <ul className="flex flex-col gap-1 text-sm">
                {c.tarifas.map((t) => (
                  <li key={t.id} className="flex justify-between gap-2">
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
            <h2 className="mb-3 text-lg font-semibold">Horario</h2>
            <HorarioTabla dias={c.horarioEfectivo} />
          </Tarjeta>
        </div>
      </div>
    </div>
  )
}
