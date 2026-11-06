import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Controller, useForm } from 'react-hook-form'

import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { CampoWhatsapp } from '@/componentes/ui/CampoWhatsapp'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Insignia } from '@/componentes/ui/Insignia'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { Tarjeta, TituloTarjeta } from '@/componentes/ui/Tarjeta'
import { api, mensajeDeError } from '@/lib/api'
import { esquemaPerfil, type DatosPerfil } from '@/lib/validaciones'

import type { Usuario } from '@/caracteristicas/autenticacion/almacenSesion'

import { TarjetaWhatsapp } from './TarjetaWhatsapp'

function FormularioDatos({ usuario }: { usuario: Usuario }) {
  const actualizarUsuario = useSesion((s) => s.actualizarUsuario)
  const {
    register,
    control,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<DatosPerfil>({
    resolver: zodResolver(esquemaPerfil),
    defaultValues: {
      nombres: usuario.nombres,
      apellidos: usuario.apellidos,
      telefono: usuario.telefono ?? '',
      documento: usuario.documento ?? '',
    },
  })
  const guardar = useMutation({
    mutationFn: async (datos: DatosPerfil) => (await api.put<Usuario>('/me', datos)).data,
    onSuccess: (u) => {
      actualizarUsuario(u)
      avisar.exito('Datos actualizados.')
    },
    onError: (e) => avisar.error(mensajeDeError(e)),
  })

  return (
    <Tarjeta>
      <TituloTarjeta titulo="Mis datos" descripcion="Así te verán los administradores." />
      <form
        onSubmit={handleSubmit((d) => guardar.mutate(d))}
        noValidate
        className="flex flex-col gap-4"
      >
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
          value={usuario.correo}
          readOnly
          disabled
          ayuda="Es el correo de tu cuenta de Google; no se puede cambiar."
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-2">
            <Controller
              control={control}
              name="telefono"
              render={({ field }) => (
                <CampoWhatsapp
                  value={field.value}
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                  error={errors.telefono?.message}
                />
              )}
            />
          </div>
          <Campo
            etiqueta="Documento"
            error={errors.documento?.message}
            {...register('documento')}
          />
        </div>
        <Boton type="submit" disabled={!isDirty || guardar.isPending} className="self-start">
          {guardar.isPending ? 'Guardando…' : 'Guardar cambios'}
        </Boton>
      </form>
    </Tarjeta>
  )
}

export function PaginaPerfil() {
  const usuario = useSesion((s) => s.usuario)!
  return (
    <div className="flex max-w-2xl flex-col gap-6">
      <EncabezadoPagina
        titulo="Mi perfil"
        descripcion="Entras con tu cuenta de Google: no tienes contraseña de Goltime que cuidar."
        acciones={<Insignia tono="verde">Cuenta de Google</Insignia>}
      />
      <FormularioDatos usuario={usuario} />
      {usuario.rol === 'CLIENTE' && <TarjetaWhatsapp usuario={usuario} />}
    </div>
  )
}
