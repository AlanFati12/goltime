import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { IndicadorClave } from '@/componentes/ui/IndicadorClave'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { mensajeDeError } from '@/lib/api'
import { esquemaRestablecer, type DatosRestablecer } from '@/lib/validaciones'

import { restablecerClave } from './servicioAutenticacion'
import { TarjetaAcceso } from './TarjetaAcceso'

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
      <TarjetaAcceso
        titulo="Enlace no válido"
        pie={
          <Link to="/olvide-clave" className="enlace">
            Solicitar un enlace nuevo
          </Link>
        }
      >
        <Alerta tipo="error">El enlace no es válido. Solicita uno nuevo.</Alerta>
      </TarjetaAcceso>
    )
  }

  if (listo) {
    return (
      <TarjetaAcceso titulo="Contraseña actualizada">
        <div className="flex flex-col gap-4">
          <Alerta tipo="exito">
            Tu contraseña fue actualizada. Ya puedes ingresar con la nueva.
          </Alerta>
          <EnlaceBoton to="/ingresar" className="w-full">
            Ir a ingresar
          </EnlaceBoton>
        </div>
      </TarjetaAcceso>
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
    <TarjetaAcceso titulo="Crea una contraseña nueva">
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
        <Boton type="submit" variante="volt" tamano="lg" disabled={isSubmitting} className="w-full">
          {isSubmitting ? 'Guardando…' : 'Guardar contraseña'}
        </Boton>
      </form>
    </TarjetaAcceso>
  )
}
