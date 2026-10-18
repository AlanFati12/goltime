import { zodResolver } from '@hookform/resolvers/zod'
import { MailCheck } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, Casilla } from '@/componentes/ui/Campo'
import { IndicadorClave } from '@/componentes/ui/IndicadorClave'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { mensajeDeError } from '@/lib/api'
import { esquemaRegistro, type DatosRegistro } from '@/lib/validaciones'

import { useSesion } from './almacenSesion'
import { TarjetaAcceso } from './TarjetaAcceso'
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
      <TarjetaAcceso titulo="¡Cuenta creada!">
        <div className="flex flex-col items-center gap-4 text-center">
          <span className="bg-primario-claro text-primario flex size-12 items-center justify-center rounded-full">
            <MailCheck className="size-6" aria-hidden />
          </span>
          <p className="text-texto-suave text-sm">
            Te enviamos un correo a <strong className="text-texto">{correoRegistrado}</strong> para
            verificar tu cuenta. Ya puedes ingresar.
          </p>
          <EnlaceBoton to="/ingresar" className="w-full">
            Ir a ingresar
          </EnlaceBoton>
        </div>
      </TarjetaAcceso>
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
    <TarjetaAcceso
      titulo="Crea tu cuenta"
      descripcion="Reserva canchas, sigue torneos y gana premios."
      ancho="lg"
      pie={
        <>
          ¿Ya tienes cuenta?{' '}
          <Link to="/ingresar" className="enlace">
            Ingresa
          </Link>
        </>
      }
    >
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
        <Boton type="submit" disabled={isSubmitting} className="mt-1 w-full">
          {isSubmitting ? 'Creando cuenta…' : 'Crear cuenta'}
        </Boton>
      </form>
    </TarjetaAcceso>
  )
}
