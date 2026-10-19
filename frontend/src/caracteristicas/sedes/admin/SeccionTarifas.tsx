import { zodResolver } from '@hookform/resolvers/zod'
import { Controller, useForm } from 'react-hook-form'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, Casilla } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { formatearCOP } from '@/lib/formato'
import { mensajeDeError } from '@/lib/api'
import { cn } from '@/lib/utils'

import { DIAS_CORTOS, HORAS } from '../constantes'
import { useTarifas } from '../consultas'
import type { Cancha } from '../tipos'
import { esquemaTarifa, type DatosTarifaFormulario } from './esquemas'

/** Franjas de tarifa de una cancha: lista y formulario para agregar. */
export function SeccionTarifas({ cancha }: { cancha: Cancha }) {
  const { crear, eliminar } = useTarifas()
  const {
    register,
    handleSubmit,
    control,
    watch,
    reset,
    formState: { errors },
  } = useForm<DatosTarifaFormulario>({
    resolver: zodResolver(esquemaTarifa),
    defaultValues: {
      diasSemana: [],
      aplicaFestivo: false,
      horaInicio: '06:00',
      horaFin: '17:00',
      vigenteDesde: '',
      vigenteHasta: '',
    },
  })
  const festivo = watch('aplicaFestivo')

  const alEnviar = handleSubmit((d) =>
    crear.mutate(
      {
        canchaId: cancha.id,
        datos: {
          ...d,
          diasSemana: d.aplicaFestivo ? [] : d.diasSemana,
          vigenteDesde: d.vigenteDesde || undefined,
          vigenteHasta: d.vigenteHasta || undefined,
        },
      },
      {
        onSuccess: () => {
          avisar.exito('Franja agregada.')
          reset()
        },
        onError: (e) => avisar.error(mensajeDeError(e)),
      },
    ),
  )

  return (
    <div className="flex flex-col gap-5">
      {cancha.tarifas.length === 0 ? (
        <p className="text-texto-suave text-sm">
          Sin franjas: siempre se cobra el precio base ({formatearCOP(cancha.precioBaseHora)}/h).
        </p>
      ) : (
        <ul className="divide-borde divide-y text-sm">
          {cancha.tarifas.map((t) => (
            <li key={t.id} className="flex items-center justify-between gap-3 py-2">
              <div>
                <div className="font-medium">
                  {t.aplicaFestivo
                    ? 'Festivos'
                    : t.diasSemana.map((d) => DIAS_CORTOS[d - 1]).join(', ')}{' '}
                  · {t.horaInicio}–{t.horaFin}
                </div>
                {(t.vigenteDesde || t.vigenteHasta) && (
                  <div className="text-texto-suave text-xs">
                    Vigente {t.vigenteDesde ? `desde ${t.vigenteDesde}` : ''}{' '}
                    {t.vigenteHasta ? `hasta ${t.vigenteHasta}` : ''}
                  </div>
                )}
              </div>
              <div className="flex items-center gap-3">
                <strong>{formatearCOP(t.precioHora)}</strong>
                <Boton
                  tamano="sm"
                  variante="secundario"
                  disabled={eliminar.isPending}
                  onClick={() =>
                    eliminar.mutate(t.id, { onError: (e) => avisar.error(mensajeDeError(e)) })
                  }
                >
                  Quitar
                </Boton>
              </div>
            </li>
          ))}
        </ul>
      )}

      <form
        onSubmit={alEnviar}
        noValidate
        className="border-borde rounded-boton flex flex-col gap-4 border p-4"
      >
        <h3 className="font-semibold">Agregar franja</h3>
        <Casilla etiqueta="Solo en festivos de Colombia" {...register('aplicaFestivo')} />
        {!festivo && (
          <Controller
            control={control}
            name="diasSemana"
            render={({ field }) => (
              <fieldset>
                <legend className="mb-1 text-sm font-medium">Días</legend>
                <div className="flex flex-wrap gap-2">
                  {DIAS_CORTOS.map((nombre, i) => {
                    const dia = i + 1
                    const activo = field.value.includes(dia)
                    return (
                      <button
                        key={dia}
                        type="button"
                        aria-pressed={activo}
                        onClick={() =>
                          field.onChange(
                            activo ? field.value.filter((d) => d !== dia) : [...field.value, dia],
                          )
                        }
                        className={cn(
                          'rounded-boton border px-3 py-1 text-sm',
                          activo
                            ? 'bg-primario border-primario text-sobre-primario'
                            : 'border-borde bg-superficie',
                        )}
                      >
                        {nombre}
                      </button>
                    )
                  })}
                </div>
                {errors.diasSemana && (
                  <p role="alert" className="text-peligro mt-1 text-xs">
                    {errors.diasSemana.message}
                  </p>
                )}
              </fieldset>
            )}
          />
        )}
        <div className="grid gap-4 sm:grid-cols-3">
          <CampoSelect etiqueta="Desde" {...register('horaInicio')}>
            {HORAS.slice(0, 48).map((h) => (
              <option key={h}>{h}</option>
            ))}
          </CampoSelect>
          <CampoSelect etiqueta="Hasta" error={errors.horaFin?.message} {...register('horaFin')}>
            {HORAS.slice(1).map((h) => (
              <option key={h}>{h}</option>
            ))}
          </CampoSelect>
          <Campo
            etiqueta="Precio por hora (COP)"
            type="number"
            inputMode="numeric"
            min={0}
            error={errors.precioHora?.message}
            {...register('precioHora', { valueAsNumber: true })}
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo etiqueta="Vigente desde (opcional)" type="date" {...register('vigenteDesde')} />
          <Campo
            etiqueta="Vigente hasta (opcional)"
            type="date"
            error={errors.vigenteHasta?.message}
            {...register('vigenteHasta')}
          />
        </div>
        <Boton type="submit" className="self-start" disabled={crear.isPending}>
          {crear.isPending ? 'Agregando…' : 'Agregar franja'}
        </Boton>
      </form>
    </div>
  )
}
