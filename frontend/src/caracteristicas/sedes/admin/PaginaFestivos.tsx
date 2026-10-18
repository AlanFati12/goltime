import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'

import { useAccionesFestivos, useFestivos } from '../consultas'
import { esquemaFestivo } from './esquemas'

const DIAS = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado']

function diaDeLaSemana(fecha: string) {
  const [a, m, d] = fecha.split('-').map(Number)
  return DIAS[new Date(a, m - 1, d).getDay()]
}

export function PaginaFestivos() {
  const [anio, setAnio] = useState(new Date().getFullYear())
  const { data, isPending, isError, error } = useFestivos(anio)
  const { crear, eliminar } = useAccionesFestivos()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<{ fecha: string; nombre: string }>({ resolver: zodResolver(esquemaFestivo) })

  const alEnviar = handleSubmit((d) =>
    crear.mutate(d, {
      onSuccess: () => {
        avisar.exito('Festivo agregado.')
        reset()
      },
      onError: (e) => avisar.error(mensajeDeError(e)),
    }),
  )

  return (
    <div className="grid gap-6 lg:grid-cols-[3fr_2fr]">
      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between gap-2">
          <h1 className="text-2xl font-bold sm:text-[1.75rem]">Festivos</h1>
          <div className="flex items-center gap-2">
            <Boton
              tamano="sm"
              variante="secundario"
              aria-label="Año anterior"
              onClick={() => setAnio((a) => a - 1)}
            >
              ←
            </Boton>
            <span className="w-12 text-center font-medium">{anio}</span>
            <Boton
              tamano="sm"
              variante="secundario"
              aria-label="Año siguiente"
              onClick={() => setAnio((a) => a + 1)}
            >
              →
            </Boton>
          </div>
        </div>
        <p className="text-texto-suave text-sm">
          En estas fechas rige la tarifa de festivo de cada cancha (si tiene una).
        </p>
        {isPending && <Cargando />}
        {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
        {data?.length === 0 && (
          <Tarjeta className="text-texto-suave text-center text-sm">
            No hay festivos cargados para {anio}.
          </Tarjeta>
        )}
        {data && data.length > 0 && (
          <Tarjeta className="p-0 sm:p-0">
            <ul className="divide-borde divide-y text-sm">
              {data.map((f) => (
                <li key={f.id} className="flex items-center justify-between gap-3 px-4 py-2">
                  <div>
                    <span className="font-medium">{f.fecha.split('-').reverse().join('/')}</span>
                    <span className="text-texto-suave"> · {diaDeLaSemana(f.fecha)}</span>
                    <div>{f.nombre}</div>
                  </div>
                  <Boton
                    tamano="sm"
                    variante="secundario"
                    disabled={eliminar.isPending}
                    onClick={() =>
                      eliminar.mutate(f.id, { onError: (e) => avisar.error(mensajeDeError(e)) })
                    }
                  >
                    Quitar
                  </Boton>
                </li>
              ))}
            </ul>
          </Tarjeta>
        )}
      </div>

      <Tarjeta className="h-fit">
        <h2 className="mb-4 text-lg font-semibold">Agregar festivo</h2>
        <form onSubmit={alEnviar} noValidate className="flex flex-col gap-4">
          <Campo
            etiqueta="Fecha"
            type="date"
            error={errors.fecha?.message}
            {...register('fecha')}
          />
          <Campo etiqueta="Nombre" error={errors.nombre?.message} {...register('nombre')} />
          <Boton type="submit" className="self-start" disabled={crear.isPending}>
            {crear.isPending ? 'Agregando…' : 'Agregar'}
          </Boton>
        </form>
      </Tarjeta>
    </div>
  )
}
