import { useEstadoApi } from '@/caracteristicas/inicio/useEstadoApi'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { useProximosPartidos } from '@/caracteristicas/torneos/consultas'
import { TarjetaPartido } from '@/caracteristicas/torneos/TarjetaPartido'

export function PaginaInicio() {
  const { data, isPending, isError } = useEstadoApi()
  const proximos = useProximosPartidos(4)

  return (
    <section className="flex flex-col items-start gap-6 py-10">
      <h1 className="text-primario-oscuro text-4xl font-bold sm:text-5xl">
        Reserva tu cancha, <span className="text-primario">juega tu partido</span>
      </h1>
      <p className="text-texto-suave max-w-xl text-lg">
        Conoce nuestras sedes y canchas, consulta precios y horarios. Reserva en línea y sigue los
        torneos de la sede.
      </p>
      <div className="flex flex-wrap gap-3">
        <EnlaceBoton to="/reservar" tamano="lg">
          Reservar una cancha
        </EnlaceBoton>
        <EnlaceBoton to="/sedes" tamano="lg" variante="secundario">
          Ver sedes y canchas
        </EnlaceBoton>
        <EnlaceBoton to="/torneos" tamano="lg" variante="secundario">
          Ver torneos
        </EnlaceBoton>
      </div>

      {proximos.data && proximos.data.length > 0 && (
        <section className="flex w-full flex-col gap-3" aria-label="Próximos partidos">
          <h2 className="text-xl font-semibold">Próximos partidos</h2>
          <div className="grid gap-3 md:grid-cols-2">
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

      <p
        role="status"
        className="border-borde bg-superficie text-texto-suave rounded-tarjeta border px-3 py-2 text-sm"
      >
        {isPending && 'Conectando con la API…'}
        {isError && 'La API no responde.'}
        {data && `API conectada (${data.aplicacion}) · zona horaria ${data.zonaHoraria}`}
      </p>
    </section>
  )
}
