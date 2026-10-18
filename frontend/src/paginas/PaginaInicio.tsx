import { useProximosPartidos } from '@/caracteristicas/torneos/consultas'
import { TarjetaPartido } from '@/caracteristicas/torneos/TarjetaPartido'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'

const PASOS = [
  { n: 1, titulo: 'Elige tu cancha', texto: 'Mira las sedes, modalidades y precios.' },
  { n: 2, titulo: 'Escoge el horario', texto: 'Toca un espacio libre en el calendario.' },
  { n: 3, titulo: 'Juega', texto: 'Te confirmamos la reserva y te avisamos.' },
]

export function PaginaInicio() {
  const proximos = useProximosPartidos(4)

  return (
    <div className="flex flex-col gap-16 sm:gap-20">
      <section className="flex flex-col items-start gap-6 pt-4 sm:pt-10">
        <h1 className="text-primario-oscuro max-w-3xl text-4xl leading-tight font-bold sm:text-6xl">
          Reserva tu cancha, <span className="text-primario">juega tu partido</span>
        </h1>
        <p className="text-texto-suave max-w-xl text-lg">
          Fútbol sintético en Neiva: reserva en línea en menos de un minuto.
        </p>
        <div className="flex flex-wrap gap-3">
          <EnlaceBoton to="/reservar" tamano="lg">
            Reservar una cancha
          </EnlaceBoton>
          <EnlaceBoton to="/sedes" tamano="lg" variante="secundario">
            Ver sedes
          </EnlaceBoton>
        </div>
      </section>

      <section aria-label="Cómo funciona">
        <ol className="grid gap-4 sm:grid-cols-3">
          {PASOS.map((p) => (
            <li
              key={p.n}
              className="border-borde bg-superficie rounded-tarjeta flex gap-4 border p-5"
            >
              <span className="bg-primario-claro text-primario-oscuro font-titulo flex size-9 shrink-0 items-center justify-center rounded-full font-bold">
                {p.n}
              </span>
              <div>
                <h2 className="font-semibold">{p.titulo}</h2>
                <p className="text-texto-suave mt-0.5 text-sm">{p.texto}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      {proximos.data && proximos.data.length > 0 && (
        <section className="flex flex-col gap-4" aria-label="Próximos partidos">
          <div className="flex items-baseline justify-between">
            <h2 className="text-2xl font-semibold">Próximos partidos</h2>
            <EnlaceBoton to="/torneos" variante="fantasma" tamano="sm">
              Ver torneos
            </EnlaceBoton>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            {proximos.data.map((p) => (
              <TarjetaPartido
                key={p.id}
                partido={p}
                ruta={`/torneos/partidos/${p.id}`}
                mostrarTorneo
              />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
