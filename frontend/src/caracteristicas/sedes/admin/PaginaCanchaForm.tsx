import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, CampoTexto, Casilla } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'

import { EditorHorario } from '../EditorHorario'
import { GestorFotos } from '../GestorFotos'
import { ModalCambioEstado } from '../ModalCambioEstado'
import { ETIQUETA_ESTADO, MODALIDADES } from '../constantes'
import {
  useCanchaAdmin,
  useCanchasAdmin,
  useEstado,
  useFotos,
  useGuardarCancha,
  useHorarioCancha,
} from '../consultas'
import type { Cancha, EstadoCancha } from '../tipos'
import { SeccionTarifas } from './SeccionTarifas'
import { esquemaCancha, numeroOpcional, type DatosCanchaFormulario } from './esquemas'

function FormularioDatos({ cancha, sedeId }: { cancha: Cancha | null; sedeId: number }) {
  const navegar = useNavigate()
  const guardar = useGuardarCancha()
  const otras = useCanchasAdmin(sedeId)
  const [componentes, setComponentes] = useState<number[]>(
    cancha?.componentes.map((c) => c.id) ?? [],
  )
  const candidatas = (otras.data ?? []).filter((c) => c.id !== cancha?.id && !c.esCompuesta)

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<DatosCanchaFormulario>({
    resolver: zodResolver(esquemaCancha),
    defaultValues: {
      nombre: cancha?.nombre ?? '',
      jugadoresPorEquipo: cancha?.jugadoresPorEquipo ?? 5,
      capacidadTotal: cancha?.capacidadTotal,
      superficie: cancha?.superficie ?? 'Sintética',
      largoM: cancha?.largoM ?? undefined,
      anchoM: cancha?.anchoM ?? undefined,
      techada: cancha?.techada ?? false,
      iluminacion: cancha?.iluminacion ?? true,
      precioBaseHora: cancha?.precioBaseHora,
      duracionBloqueMin: cancha?.duracionBloqueMin ?? 60,
      duracionMinMin: cancha?.duracionMinMin ?? 60,
      duracionMaxMin: cancha?.duracionMaxMin ?? 120,
      descripcion: cancha?.descripcion ?? '',
      orden: cancha?.orden ?? 0,
    },
  })

  const alEnviar = handleSubmit((d) =>
    guardar.mutate(
      { id: cancha?.id ?? null, sedeId, datos: { ...d, componentes } },
      {
        onSuccess: (guardada) => {
          avisar.exito(
            cancha ? 'Cancha actualizada.' : 'Cancha creada. Ahora puedes agregar tarifas y fotos.',
          )
          if (!cancha) navegar(`/admin/canchas/${guardada.id}`, { replace: true })
        },
        onError: (e) => avisar.error(mensajeDeError(e)),
      },
    ),
  )

  const alternar = (id: number) =>
    setComponentes((c) => (c.includes(id) ? c.filter((x) => x !== id) : [...c, id]))
  const cambioComponentes =
    JSON.stringify([...componentes].sort()) !==
    JSON.stringify((cancha?.componentes.map((c) => c.id) ?? []).sort())

  return (
    <Tarjeta>
      <h2 className="mb-4 text-lg font-semibold">Datos de la cancha</h2>
      <form onSubmit={alEnviar} noValidate className="flex flex-col gap-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo
            etiqueta="Nombre o número"
            error={errors.nombre?.message}
            {...register('nombre')}
          />
          <CampoSelect
            etiqueta="Modalidad"
            error={errors.jugadoresPorEquipo?.message}
            {...register('jugadoresPorEquipo', { valueAsNumber: true })}
          >
            {MODALIDADES.map((m) => (
              <option key={m} value={m}>
                Fútbol {m}
              </option>
            ))}
          </CampoSelect>
        </div>
        <div className="grid gap-4 sm:grid-cols-3">
          <Campo
            etiqueta="Precio base por hora (COP)"
            type="number"
            inputMode="numeric"
            min={0}
            error={errors.precioBaseHora?.message}
            {...register('precioBaseHora', { valueAsNumber: true })}
          />
          <Campo
            etiqueta="Capacidad total (opcional)"
            type="number"
            error={errors.capacidadTotal?.message}
            {...register('capacidadTotal', { setValueAs: numeroOpcional })}
          />
          <Campo
            etiqueta="Superficie"
            error={errors.superficie?.message}
            {...register('superficie')}
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-3">
          <CampoSelect
            etiqueta="Bloque de reserva"
            {...register('duracionBloqueMin', { valueAsNumber: true })}
          >
            {[30, 60, 90, 120].map((m) => (
              <option key={m} value={m}>
                {m} min
              </option>
            ))}
          </CampoSelect>
          <Campo
            etiqueta="Duración mínima (min)"
            type="number"
            step={15}
            error={errors.duracionMinMin?.message}
            {...register('duracionMinMin', { valueAsNumber: true })}
          />
          <Campo
            etiqueta="Duración máxima (min)"
            type="number"
            step={15}
            error={errors.duracionMaxMin?.message}
            {...register('duracionMaxMin', { valueAsNumber: true })}
          />
        </div>
        <div className="grid gap-4 sm:grid-cols-3">
          <Campo
            etiqueta="Largo en metros (opcional)"
            inputMode="decimal"
            error={errors.largoM?.message}
            {...register('largoM', { setValueAs: numeroOpcional })}
          />
          <Campo
            etiqueta="Ancho en metros (opcional)"
            inputMode="decimal"
            error={errors.anchoM?.message}
            {...register('anchoM', { setValueAs: numeroOpcional })}
          />
          <Campo
            etiqueta="Orden de visualización"
            type="number"
            {...register('orden', { setValueAs: numeroOpcional })}
          />
        </div>
        <div className="flex flex-wrap gap-6">
          <Casilla etiqueta="Techada" {...register('techada')} />
          <Casilla etiqueta="Iluminación" {...register('iluminacion')} />
        </div>
        <CampoTexto
          etiqueta="Descripción"
          error={errors.descripcion?.message}
          {...register('descripcion')}
        />

        <fieldset className="border-borde rounded-boton border p-4">
          <legend className="px-1 text-sm font-medium">¿Depende de otras canchas?</legend>
          <p className="text-texto-suave mb-3 text-xs">
            Si esta cancha se forma con otras (ej. una de fútbol 8 con dos de fútbol 5), márcalas:
            si alguna está ocupada o bloqueada, esta no estará disponible, y al revés.
          </p>
          {candidatas.length === 0 ? (
            <p className="text-texto-suave text-sm">No hay otras canchas simples en esta sede.</p>
          ) : (
            <div className="flex flex-col gap-2">
              {candidatas.map((c) => (
                <Casilla
                  key={c.id}
                  etiqueta={`${c.nombre} (${c.modalidad})`}
                  checked={componentes.includes(c.id)}
                  onChange={() => alternar(c.id)}
                />
              ))}
            </div>
          )}
        </fieldset>

        <Boton
          type="submit"
          className="self-start"
          disabled={guardar.isPending || (!!cancha && !isDirty && !cambioComponentes)}
        >
          {guardar.isPending ? 'Guardando…' : cancha ? 'Guardar cambios' : 'Crear cancha'}
        </Boton>
      </form>
    </Tarjeta>
  )
}

const ACCION_ESTADO: Record<EstadoCancha, string> = {
  ACTIVA: 'Activar',
  MANTENIMIENTO: 'Poner en mantenimiento',
  INACTIVA: 'Marcar como inactiva',
}

function SeccionEstado({ cancha }: { cancha: Cancha }) {
  const estado = useEstado()
  const [nuevo, setNuevo] = useState<EstadoCancha | null>(null)
  const opciones = (['ACTIVA', 'MANTENIMIENTO', 'INACTIVA'] as EstadoCancha[]).filter(
    (e) => e !== cancha.estado,
  )

  return (
    <Tarjeta>
      <h2 className="mb-1 text-lg font-semibold">Estado</h2>
      <p className="text-texto-suave mb-3 text-sm">
        Solo las canchas activas se pueden reservar. Sacar una cancha de servicio cancela sus
        reservas futuras (previa confirmación).
      </p>
      <div className="flex flex-wrap items-center gap-2">
        <Insignia
          tono={
            cancha.estado === 'ACTIVA'
              ? 'verde'
              : cancha.estado === 'MANTENIMIENTO'
                ? 'ambar'
                : 'rojo'
          }
        >
          {ETIQUETA_ESTADO[cancha.estado]}
        </Insignia>
        {opciones.map((e) => (
          <Boton key={e} tamano="sm" variante="secundario" onClick={() => setNuevo(e)}>
            {ACCION_ESTADO[e]}
          </Boton>
        ))}
      </div>
      {nuevo && (
        <ModalCambioEstado
          abierto
          titulo={ACCION_ESTADO[nuevo]}
          descripcion={
            nuevo === 'ACTIVA'
              ? `"${cancha.nombre}" volverá a estar disponible para reservar.`
              : `"${cancha.nombre}" dejará de mostrarse y de poder reservarse.`
          }
          requiereMotivo={nuevo !== 'ACTIVA'}
          textoConfirmar="Confirmar"
          tono={nuevo === 'ACTIVA' ? 'primario' : 'peligro'}
          previsualizar={() => estado.previsualizarCancha(cancha.id, nuevo)}
          confirmar={async (motivo, confirmarConflictos) => {
            await estado.cambiarCancha.mutateAsync({
              id: cancha.id,
              estado: nuevo,
              motivo,
              confirmarConflictos,
            })
            avisar.exito('Estado actualizado.')
          }}
          alCerrar={() => setNuevo(null)}
        />
      )}
    </Tarjeta>
  )
}

export function PaginaCanchaForm() {
  const parametro = useParams().id
  const id = parametro ? Number(parametro) : null
  const sedeParam = Number(useSearchParams()[0].get('sedeId'))
  const { data: cancha, isPending, isError, error } = useCanchaAdmin(id)
  const horario = useHorarioCancha()
  const fotos = useFotos()

  if (id !== null && isPending) return <Cargando texto="Cargando cancha…" />
  if (id !== null && isError) return <Alerta tipo="error">{mensajeDeError(error)}</Alerta>
  const sedeId = cancha?.sedeId ?? sedeParam
  if (!sedeId) return <Alerta tipo="error">Elige una sede para crear la cancha.</Alerta>

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-6">
      <div>
        <Link to="/admin/canchas" className="text-texto-suave text-sm hover:underline">
          ← Canchas
        </Link>
        <h1 className="text-2xl font-bold">{cancha ? cancha.nombre : 'Nueva cancha'}</h1>
        {cancha && <p className="text-texto-suave text-sm">{cancha.sedeNombre}</p>}
      </div>

      <FormularioDatos key={cancha?.id ?? 'nueva'} cancha={cancha ?? null} sedeId={sedeId} />

      {cancha && (
        <>
          <SeccionEstado key={cancha.estado} cancha={cancha} />

          <Tarjeta>
            <h2 className="mb-1 text-lg font-semibold">Tarifas</h2>
            <p className="text-texto-suave mb-4 text-sm">
              Franjas por día y hora, y tarifa especial de festivos. Si un horario cruza dos
              franjas, se cobra proporcionalmente.
            </p>
            <SeccionTarifas cancha={cancha} />
          </Tarjeta>

          <Tarjeta>
            <h2 className="mb-1 text-lg font-semibold">Horario</h2>
            <p className="text-texto-suave mb-4 text-sm">
              {cancha.horarioPropio.length > 0
                ? 'Esta cancha tiene un horario propio que reemplaza al de la sede.'
                : 'Usa el horario de la sede. Puedes definir uno propio.'}
            </p>
            <EditorHorario
              key={JSON.stringify(cancha.horarioEfectivo)}
              dias={cancha.horarioEfectivo}
              textoGuardar="Guardar horario propio"
              guardando={horario.isPending}
              alGuardar={(dias) =>
                horario.mutate(
                  { id: cancha.id, dias },
                  {
                    onSuccess: () => avisar.exito('Horario propio guardado.'),
                    onError: (e) => avisar.error(mensajeDeError(e)),
                  },
                )
              }
              alQuitar={
                cancha.horarioPropio.length > 0
                  ? () =>
                      horario.mutate(
                        { id: cancha.id, dias: null },
                        { onSuccess: () => avisar.exito('Ahora usa el horario de la sede.') },
                      )
                  : undefined
              }
            />
          </Tarjeta>

          <Tarjeta>
            <h2 className="mb-4 text-lg font-semibold">Fotos</h2>
            <GestorFotos
              fotos={cancha.fotos}
              subiendo={fotos.subirACancha.isPending}
              alSubir={(archivo) =>
                fotos.subirACancha.mutate(
                  { id: cancha.id, archivo },
                  {
                    onSuccess: () => avisar.exito('Foto agregada.'),
                    onError: (e) => avisar.error(mensajeDeError(e)),
                  },
                )
              }
              alQuitar={(f) =>
                fotos.quitarDeCancha.mutate(
                  { id: cancha.id, fotoId: f.id },
                  { onError: (e) => avisar.error(mensajeDeError(e)) },
                )
              }
            />
          </Tarjeta>
        </>
      )}
    </div>
  )
}
