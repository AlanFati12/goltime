import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'

import { nombreCompleto } from '@/caracteristicas/autenticacion/almacenSesion'
import { Alerta } from '@/componentes/ui/Alerta'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { Cargando } from '@/componentes/ui/Cargando'
import { IndicadorClave } from '@/componentes/ui/IndicadorClave'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { esquemaNuevoAdmin, type DatosNuevoAdmin } from '@/lib/validaciones'

import { useAdministradores, useCrearAdministrador } from './consultasAdmin'

export function PaginaAdministradores() {
  const { data, isPending, isError, error } = useAdministradores()
  const crear = useCrearAdministrador()
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<DatosNuevoAdmin>({ resolver: zodResolver(esquemaNuevoAdmin) })

  const alEnviar = handleSubmit((datos) =>
    crear.mutate(datos, {
      onSuccess: (u) => {
        avisar.exito(`Administrador ${nombreCompleto(u)} creado.`)
        reset()
      },
      onError: (e) => avisar.error(mensajeDeError(e)),
    }),
  )

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <div className="flex flex-col gap-4">
        <h1 className="text-2xl font-bold">Administradores</h1>
        {isPending && <Cargando />}
        {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
        {data && (
          <Tarjeta className="p-0">
            <ul className="divide-borde divide-y">
              {data.map((a) => (
                <li key={a.id} className="px-4 py-3">
                  <div className="font-medium">{nombreCompleto(a)}</div>
                  <div className="text-texto-suave text-sm">{a.correo}</div>
                </li>
              ))}
            </ul>
          </Tarjeta>
        )}
      </div>

      <Tarjeta className="h-fit">
        <h2 className="mb-4 text-lg font-semibold">Crear administrador</h2>
        <form onSubmit={alEnviar} noValidate className="flex flex-col gap-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Campo etiqueta="Nombres" error={errors.nombres?.message} {...register('nombres')} />
            <Campo
              etiqueta="Apellidos"
              error={errors.apellidos?.message}
              {...register('apellidos')}
            />
          </div>
          <Campo
            etiqueta="Correo electrónico"
            type="email"
            error={errors.correo?.message}
            {...register('correo')}
          />
          <div className="flex flex-col gap-2">
            <Campo
              etiqueta="Contraseña inicial"
              type="password"
              autoComplete="new-password"
              error={errors.clave?.message}
              {...register('clave')}
            />
            <IndicadorClave clave={watch('clave') ?? ''} />
          </div>
          <Boton type="submit" disabled={crear.isPending} className="self-start">
            {crear.isPending ? 'Creando…' : 'Crear administrador'}
          </Boton>
        </form>
      </Tarjeta>
    </div>
  )
}
