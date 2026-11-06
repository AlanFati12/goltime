import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'

import { nombreCompleto } from '@/caracteristicas/autenticacion/almacenSesion'
import { Alerta } from '@/componentes/ui/Alerta'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { Cargando } from '@/componentes/ui/Cargando'
import { IndicadorClave } from '@/componentes/ui/IndicadorClave'
import { Tarjeta, TituloTarjeta } from '@/componentes/ui/Tarjeta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
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
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Administradores"
        descripcion="Personas con acceso completo al panel."
      />
      <div className="grid items-start gap-6 lg:grid-cols-2">
        <div className="flex flex-col gap-4">
          {isPending && <Cargando variante="tabla" />}
          {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
          {data && (
            <Tarjeta className="p-0 sm:p-0">
              <ul className="divide-borde divide-y">
                {data.map((a) => (
                  <li key={a.id} className="flex items-center gap-3 px-5 py-3.5">
                    <span className="bg-tenue text-texto-suave flex size-9 items-center justify-center rounded-full text-sm font-semibold">
                      {nombreCompleto(a).charAt(0).toUpperCase()}
                    </span>
                    <div className="min-w-0">
                      <div className="font-medium">{nombreCompleto(a)}</div>
                      <div className="text-texto-suave truncate text-sm">{a.correo}</div>
                    </div>
                  </li>
                ))}
              </ul>
            </Tarjeta>
          )}
        </div>

        <Tarjeta className="h-fit">
          <TituloTarjeta titulo="Crear administrador" />
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
    </div>
  )
}
