import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { CampoWhatsapp } from '@/componentes/ui/CampoWhatsapp'
import { mensajeDeError } from '@/lib/api'
import { esquemaPerfil, type DatosPerfil } from '@/lib/validaciones'

import { useSesion } from './almacenSesion'
import { completarPerfil } from './servicioAutenticacion'
import { TarjetaAcceso } from './TarjetaAcceso'

/**
 * Primer paso después de entrar con Google por primera vez: los datos que Google no entrega y Goltime necesita
 * (el WhatsApp para avisarte de tus reservas y para que el bot te reconozca; los apellidos si Google no los dio).
 * Mientras falten, el cliente no puede reservar; el servidor también lo exige.
 */
export function PaginaCompletarPerfil() {
  const usuario = useSesion((s) => s.usuario)!
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DatosPerfil>({
    resolver: zodResolver(esquemaPerfil),
    defaultValues: {
      nombres: usuario.nombres,
      apellidos: usuario.apellidos ?? '',
      telefono: usuario.telefono ?? '',
      documento: usuario.documento ?? '',
    },
  })

  // Quien ya completó sus datos (o es administrador) no tiene nada que hacer aquí.
  if (usuario.perfilCompleto) return <Navigate to="/" replace />

  const alEnviar = handleSubmit(async (datos) => {
    setError(null)
    try {
      await completarPerfil(datos)
      const desde = (ubicacion.state as { desde?: string } | null)?.desde
      navegar(desde ?? '/', { replace: true })
    } catch (e) {
      setError(mensajeDeError(e))
    }
  })

  return (
    <TarjetaAcceso
      titulo="Completa tu perfil"
      ancho="lg"
      descripcion={
        <>
          ¡Bienvenido, {usuario.nombres}! Google nos dio tu nombre y tu correo (
          <strong>{usuario.correo}</strong>). Faltan unos datos para que podamos avisarte de tus
          reservas.
        </>
      }
    >
      <form onSubmit={alEnviar} noValidate className="flex flex-col gap-4">
        {error && <Alerta tipo="error">{error}</Alerta>}
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo etiqueta="Nombres" error={errors.nombres?.message} {...register('nombres')} />
          <Campo
            etiqueta="Apellidos"
            autoComplete="family-name"
            error={errors.apellidos?.message}
            {...register('apellidos')}
          />
        </div>
        <Controller
          control={control}
          name="telefono"
          render={({ field }) => (
            <CampoWhatsapp
              etiqueta="Tu número de WhatsApp"
              value={field.value}
              onChange={field.onChange}
              onBlur={field.onBlur}
              error={errors.telefono?.message}
              ayuda="Te avisamos por aquí cuando aprueben tu reserva y puedes reservar escribiéndole a nuestro bot."
            />
          )}
        />
        <Campo
          etiqueta="Documento de identidad (opcional)"
          inputMode="numeric"
          error={errors.documento?.message}
          {...register('documento')}
        />
        <Boton
          type="submit"
          variante="volt"
          tamano="lg"
          disabled={isSubmitting}
          className="mt-1 w-full"
        >
          {isSubmitting ? 'Guardando…' : 'Guardar y continuar'}
        </Boton>
      </form>
    </TarjetaAcceso>
  )
}
