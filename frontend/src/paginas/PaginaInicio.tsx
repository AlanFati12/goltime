import { useEstadoApi } from '@/caracteristicas/inicio/useEstadoApi'
import { Boton } from '@/componentes/ui/Boton'

export function PaginaInicio() {
  const { data, isPending, isError } = useEstadoApi()

  return (
    <section className="flex flex-col items-start gap-6 py-10">
      <h1 className="text-primario-oscuro text-4xl font-bold sm:text-5xl">
        Reserva tu cancha, <span className="text-primario">juega tu partido</span>
      </h1>
      <p className="text-texto-suave max-w-xl text-lg">
        Consulta disponibilidad, reserva, sigue los torneos y gana premios. Próximamente.
      </p>
      <Boton tamano="lg" disabled>
        Ver canchas disponibles
      </Boton>

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
