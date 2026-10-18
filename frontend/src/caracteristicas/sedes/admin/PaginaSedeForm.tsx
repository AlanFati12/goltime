import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router-dom'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, CampoTexto } from '@/componentes/ui/Campo'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { mensajeDeError } from '@/lib/api'

import { EditorHorario } from '../EditorHorario'
import { GestorFotos } from '../GestorFotos'
import { useFotos, useGuardarHorarioSede, useGuardarSede, useSedeAdmin } from '../consultas'
import type { Sede } from '../tipos'
import { esquemaSede, numeroOpcional, type DatosSedeFormulario } from './esquemas'

function FormularioDatos({ sede }: { sede: Sede | null }) {
  const navegar = useNavigate()
  const guardar = useGuardarSede()
  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<DatosSedeFormulario>({
    resolver: zodResolver(esquemaSede),
    defaultValues: {
      nombre: sede?.nombre ?? '',
      direccion: sede?.direccion ?? '',
      ciudad: sede?.ciudad ?? 'Neiva',
      telefono: sede?.telefono ?? '',
      whatsapp: sede?.whatsapp ?? '',
      descripcion: sede?.descripcion ?? '',
      servicios: sede?.servicios.join(', ') ?? '',
      latitud: sede?.latitud ?? undefined,
      longitud: sede?.longitud ?? undefined,
    },
  })

  const alEnviar = handleSubmit((d) =>
    guardar.mutate(
      {
        id: sede?.id ?? null,
        datos: {
          ...d,
          servicios: d.servicios
            .split(',')
            .map((s) => s.trim())
            .filter(Boolean),
        },
      },
      {
        onSuccess: (guardada) => {
          avisar.exito(
            sede ? 'Sede actualizada.' : 'Sede creada. Ahora define su horario y agrega canchas.',
          )
          if (!sede) navegar(`/admin/sedes/${guardada.id}`, { replace: true })
        },
        onError: (e) => avisar.error(mensajeDeError(e)),
      },
    ),
  )

  return (
    <Tarjeta>
      <h2 className="mb-4 text-base font-semibold">Datos de la sede</h2>
      <form onSubmit={alEnviar} noValidate className="flex flex-col gap-4">
        <Campo etiqueta="Nombre" error={errors.nombre?.message} {...register('nombre')} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo
            etiqueta="Dirección"
            error={errors.direccion?.message}
            {...register('direccion')}
          />
          <Campo etiqueta="Ciudad" error={errors.ciudad?.message} {...register('ciudad')} />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo
            etiqueta="Teléfono"
            type="tel"
            error={errors.telefono?.message}
            {...register('telefono')}
          />
          <Campo
            etiqueta="WhatsApp"
            type="tel"
            error={errors.whatsapp?.message}
            {...register('whatsapp')}
          />
        </div>
        <CampoTexto
          etiqueta="Descripción"
          error={errors.descripcion?.message}
          {...register('descripcion')}
        />
        <Campo
          etiqueta="Servicios"
          ayuda="Separados por coma. Ej.: Parqueadero, Camerinos, Cafetería"
          error={errors.servicios?.message}
          {...register('servicios')}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo
            etiqueta="Latitud (opcional)"
            inputMode="decimal"
            error={errors.latitud?.message}
            {...register('latitud', { setValueAs: numeroOpcional })}
          />
          <Campo
            etiqueta="Longitud (opcional)"
            inputMode="decimal"
            error={errors.longitud?.message}
            {...register('longitud', { setValueAs: numeroOpcional })}
          />
        </div>
        <Boton
          type="submit"
          className="self-start"
          disabled={guardar.isPending || (!!sede && !isDirty)}
        >
          {guardar.isPending ? 'Guardando…' : sede ? 'Guardar cambios' : 'Crear sede'}
        </Boton>
      </form>
    </Tarjeta>
  )
}

export function PaginaSedeForm() {
  const parametro = useParams().id
  const id = parametro ? Number(parametro) : null
  const { data: sede, isPending, isError, error } = useSedeAdmin(id)
  const horario = useGuardarHorarioSede()
  const fotos = useFotos()

  if (id !== null && isPending) return <Cargando texto="Cargando sede…" />
  if (id !== null && isError) return <Alerta tipo="error">{mensajeDeError(error)}</Alerta>

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-6">
      <EncabezadoPagina
        titulo={sede ? sede.nombre : 'Nueva sede'}
        volver={{ a: '/admin/sedes', texto: 'Sedes' }}
        acciones={
          sede && (
            <Insignia tono={sede.activa ? 'verde' : 'rojo'}>
              {sede.activa ? 'Activa' : 'Inactiva'}
            </Insignia>
          )
        }
      />

      <FormularioDatos key={sede?.id ?? 'nueva'} sede={sede ?? null} />

      {sede && (
        <>
          <Tarjeta>
            <h2 className="mb-1 text-base font-semibold">Horario de operación</h2>
            <p className="text-texto-suave mb-4 text-sm">
              Las canchas lo usan por defecto; cada una puede tener su propio horario.
            </p>
            <EditorHorario
              key={JSON.stringify(sede.horario)}
              dias={sede.horario}
              guardando={horario.isPending}
              alGuardar={(dias) =>
                horario.mutate(
                  { id: sede.id, dias },
                  {
                    onSuccess: () => avisar.exito('Horario actualizado.'),
                    onError: (e) => avisar.error(mensajeDeError(e)),
                  },
                )
              }
            />
          </Tarjeta>

          <Tarjeta>
            <h2 className="mb-4 text-base font-semibold">Fotos</h2>
            <GestorFotos
              fotos={sede.fotos}
              subiendo={fotos.subirASede.isPending}
              alSubir={(archivo) =>
                fotos.subirASede.mutate(
                  { id: sede.id, archivo },
                  {
                    onSuccess: () => avisar.exito('Foto agregada.'),
                    onError: (e) => avisar.error(mensajeDeError(e)),
                  },
                )
              }
              alQuitar={(f) =>
                fotos.quitarDeSede.mutate(
                  { id: sede.id, fotoId: f.id },
                  { onError: (e) => avisar.error(mensajeDeError(e)) },
                )
              }
            />
          </Tarjeta>

          <Tarjeta>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-base font-semibold">Canchas ({sede.canchas.length})</h2>
              <EnlaceBoton to={`/admin/canchas/nueva?sedeId=${sede.id}`} tamano="sm">
                Nueva cancha
              </EnlaceBoton>
            </div>
            {sede.canchas.length === 0 ? (
              <p className="text-texto-suave text-sm">Esta sede aún no tiene canchas.</p>
            ) : (
              <ul className="divide-borde divide-y text-sm">
                {sede.canchas.map((c) => (
                  <li key={c.id} className="flex items-center justify-between gap-2 py-2">
                    <span>
                      {c.nombre} <Insignia tono="verde">{c.modalidad}</Insignia>
                    </span>
                    <Link to={`/admin/canchas/${c.id}`} className="enlace">
                      Editar
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </Tarjeta>
        </>
      )}
    </div>
  )
}
