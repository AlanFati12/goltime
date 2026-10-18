import { CalendarX2, ChevronLeft, ChevronRight } from 'lucide-react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta, TituloTarjeta } from '@/componentes/ui/Tarjeta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
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
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Festivos"
        descripcion="En estas fechas rige la tarifa de festivo de cada cancha (si tiene una)."
        acciones={
          <div className="border-borde bg-superficie shadow-suave rounded-boton flex items-center border">
            <Boton
              tamano="icono"
              variante="fantasma"
              aria-label="Año anterior"
              onClick={() => setAnio((a) => a - 1)}
            >
              <ChevronLeft aria-hidden />
            </Boton>
            <span className="w-14 text-center text-sm font-semibold tabular-nums">{anio}</span>
            <Boton
              tamano="icono"
              variante="fantasma"
              aria-label="Año siguiente"
              onClick={() => setAnio((a) => a + 1)}
            >
              <ChevronRight aria-hidden />
            </Boton>
          </div>
        }
      />
      <div className="grid items-start gap-6 lg:grid-cols-[3fr_2fr]">
        <div className="flex flex-col gap-4">
          {isPending && <Cargando />}
          {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
          {data?.length === 0 && (
            <EstadoVacio icono={CalendarX2} titulo={`No hay festivos cargados para ${anio}`} />
          )}
          {data && data.length > 0 && (
            <Tarjeta className="p-0 sm:p-0">
              <ul className="divide-borde divide-y text-sm">
                {data.map((f) => (
                  <li key={f.id} className="flex items-center justify-between gap-3 px-5 py-3">
                    <div>
                      <div className="font-medium">{f.nombre}</div>
                      <span className="text-texto-suave text-xs">
                        {f.fecha.split('-').reverse().join('/')} · {diaDeLaSemana(f.fecha)}
                      </span>
                    </div>
                    <Boton
                      tamano="sm"
                      variante="fantasma"
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
          <TituloTarjeta titulo="Agregar festivo" />
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
    </div>
  )
}
