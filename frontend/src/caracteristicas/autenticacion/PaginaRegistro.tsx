import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, Casilla } from '@/componentes/ui/Campo'
import { IndicadorClave } from '@/componentes/ui/IndicadorClave'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { esquemaRegistro, type DatosRegistro } from '@/lib/validaciones'

import { useSesion } from './almacenSesion'
import { registrar, rutaInicial } from './servicioAutenticacion'

export function PaginaRegistro() {
  const usuario = useSesion((s) => s.usuario)
  const [error, setError] = useState<string | null>(null)
  const [correoRegistrado, setCorreoRegistrado] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<DatosRegistro>({
    resolver: zodResolver(esquemaRegistro),
    defaultValues: { aceptaTerminos: false },
  })

  if (usuario) return <Navigate to={rutaInicial(usuario)} replace />

  if (correoRegistrado) {
    return (
      <Tarjeta className="mx-auto w-full max-w-md text-center">
        <h1 className="mb-2 text-2xl font-bold">¡Cuenta creada!</h1>
        <p className="text-texto-suave mb-5 text-sm">
          Te enviamos un correo a <strong>{correoRegistrado}</strong> para verificar tu cuenta. Ya
          puedes ingresar.
        </p>
        <Link to="/ingresar" className="text-primario font-medium underline">
          Ir a ingresar
        </Link>
      </Tarjeta>
    )
  }

  const alEnviar = handleSubmit(async (datos) => {
    setError(null)
    try {
      await registrar(datos)
      setCorreoRegistrado(datos.correo)
    } catch (e) {
      setError(mensajeDeError(e))
    }
  })

  return (
    <Tarjeta className="mx-auto w-full max-w-xl">
      <h1 className="mb-1 text-2xl font-bold">Crear cuenta</h1>
      <p className="text-texto-suave mb-5 text-sm">
        Reserva canchas, sigue torneos y gana premios.
      </p>
      <form onSubmit={alEnviar} noValidate className="flex flex-col gap-4">
        {error && <Alerta tipo="error">{error}</Alerta>}
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo
            etiqueta="Nombres"
            autoComplete="given-name"
            error={errors.nombres?.message}
            {...register('nombres')}
          />
          <Campo
            etiqueta="Apellidos"
            autoComplete="family-name"
            error={errors.apellidos?.message}
            {...register('apellidos')}
          />
        </div>
        <Campo
          etiqueta="Correo electrónico"
          type="email"
          autoComplete="email"
          error={errors.correo?.message}
          {...register('correo')}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo
            etiqueta="Celular"
            type="tel"
            autoComplete="tel"
            error={errors.telefono?.message}
            {...register('telefono')}
          />
          <Campo
            etiqueta="Documento (opcional)"
            error={errors.documento?.message}
            {...register('documento')}
          />
        </div>
        <div className="flex flex-col gap-2">
          <Campo
            etiqueta="Contraseña"
            type="password"
            autoComplete="new-password"
            ayuda="Mínimo 8 caracteres, con al menos una letra y un número."
            error={errors.clave?.message}
            {...register('clave')}
          />
          <IndicadorClave clave={watch('clave') ?? ''} />
        </div>
        <Campo
          etiqueta="Confirmar contraseña"
          type="password"
          autoComplete="new-password"
          error={errors.confirmacionClave?.message}
          {...register('confirmacionClave')}
        />
        <Casilla
          etiqueta="Acepto los términos y condiciones y el tratamiento de mis datos personales (Ley 1581 de 2012)."
          error={errors.aceptaTerminos?.message}
          {...register('aceptaTerminos')}
        />
        <Boton type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Creando cuenta…' : 'Crear cuenta'}
        </Boton>
      </form>
      <p className="text-texto-suave mt-5 text-center text-sm">
        ¿Ya tienes cuenta?{' '}
        <Link to="/ingresar" className="text-primario font-medium underline">
          Ingresa
        </Link>
      </p>
    </Tarjeta>
  )
}
