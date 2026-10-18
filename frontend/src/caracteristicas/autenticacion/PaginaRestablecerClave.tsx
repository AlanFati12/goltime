import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { IndicadorClave } from '@/componentes/ui/IndicadorClave'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { esquemaRestablecer, type DatosRestablecer } from '@/lib/validaciones'

import { restablecerClave } from './servicioAutenticacion'

export function PaginaRestablecerClave() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const [listo, setListo] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<DatosRestablecer>({ resolver: zodResolver(esquemaRestablecer) })

  if (!token) {
    return (
      <Tarjeta className="mx-auto w-full max-w-md">
        <Alerta tipo="error">El enlace no es válido. Solicita uno nuevo.</Alerta>
        <Link to="/olvide-clave" className="text-primario mt-4 inline-block text-sm underline">
          Solicitar un enlace nuevo
        </Link>
      </Tarjeta>
    )
  }

  if (listo) {
    return (
      <Tarjeta className="mx-auto w-full max-w-md">
        <Alerta tipo="exito" className="mb-4">
          Tu contraseña fue actualizada. Ya puedes ingresar con la nueva.
        </Alerta>
        <Link to="/ingresar" className="text-primario text-sm underline">
          Ir a ingresar
        </Link>
      </Tarjeta>
    )
  }

  const alEnviar = handleSubmit(async ({ claveNueva, confirmacion }) => {
    setError(null)
    try {
      await restablecerClave(token, claveNueva, confirmacion)
      setListo(true)
    } catch (e) {
      setError(mensajeDeError(e))
    }
  })

  return (
    <Tarjeta className="mx-auto w-full max-w-md">
      <h1 className="mb-5 text-2xl font-bold">Crear contraseña nueva</h1>
      <form onSubmit={alEnviar} noValidate className="flex flex-col gap-4">
        {error && <Alerta tipo="error">{error}</Alerta>}
        <div className="flex flex-col gap-2">
          <Campo
            etiqueta="Contraseña nueva"
            type="password"
            autoComplete="new-password"
            ayuda="Mínimo 8 caracteres, con al menos una letra y un número."
            error={errors.claveNueva?.message}
            {...register('claveNueva')}
          />
          <IndicadorClave clave={watch('claveNueva') ?? ''} />
        </div>
        <Campo
          etiqueta="Confirmar contraseña"
          type="password"
          autoComplete="new-password"
          error={errors.confirmacion?.message}
          {...register('confirmacion')}
        />
        <Boton type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Guardando…' : 'Guardar contraseña'}
        </Boton>
      </form>
    </Tarjeta>
  )
}
