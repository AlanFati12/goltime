import { useEstadoApi } from '@/caracteristicas/inicio/useEstadoApi'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'

export function PaginaInicio() {
  const { data, isPending, isError } = useEstadoApi()

  return (
    <section className="flex flex-col items-start gap-6 py-10">
      <h1 className="text-primario-oscuro text-4xl font-bold sm:text-5xl">
        Reserva tu cancha, <span className="text-primario">juega tu partido</span>
      </h1>
      <p className="text-texto-suave max-w-xl text-lg">
        Conoce nuestras sedes y canchas, consulta precios y horarios. Muy pronto podrás reservar,
        seguir torneos y ganar premios.
      </p>
      <div className="flex flex-wrap gap-3">
        <EnlaceBoton to="/reservar" tamano="lg">
          Reservar una cancha
        </EnlaceBoton>
        <EnlaceBoton to="/sedes" tamano="lg" variante="secundario">
          Ver sedes y canchas
        </EnlaceBoton>
      </div>

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
