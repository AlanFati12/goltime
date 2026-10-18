import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, Casilla } from '@/componentes/ui/Campo'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { esquemaIngreso, type DatosIngreso } from '@/lib/validaciones'

import { useSesion } from './almacenSesion'
import { ingresar, rutaInicial } from './servicioAutenticacion'

export function PaginaIngresar() {
  const usuario = useSesion((s) => s.usuario)
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DatosIngreso>({
    resolver: zodResolver(esquemaIngreso),
    defaultValues: { correo: '', clave: '', recordarme: false },
  })

  if (usuario) return <Navigate to={rutaInicial(usuario)} replace />

  const alEnviar = handleSubmit(async (datos) => {
    setError(null)
    try {
      const u = await ingresar(datos)
      const desde = (ubicacion.state as { desde?: string } | null)?.desde
      navegar(u.rol === 'ADMIN' ? '/admin' : (desde ?? '/'), { replace: true })
    } catch (e) {
      setError(mensajeDeError(e))
    }
  })

  return (
    <Tarjeta className="mx-auto w-full max-w-md">
      <h1 className="mb-1 text-2xl font-bold">Ingresar</h1>
      <p className="text-texto-suave mb-5 text-sm">
        Accede para reservar y gestionar tus partidos.
      </p>
      <form onSubmit={alEnviar} noValidate className="flex flex-col gap-4">
        {error && <Alerta tipo="error">{error}</Alerta>}
        <Campo
          etiqueta="Correo electrónico"
          type="email"
          autoComplete="email"
          error={errors.correo?.message}
          {...register('correo')}
        />
        <Campo
          etiqueta="Contraseña"
          type="password"
          autoComplete="current-password"
          error={errors.clave?.message}
          {...register('clave')}
        />
        <div className="flex items-center justify-between">
          <Casilla etiqueta="Recordarme" {...register('recordarme')} />
          <Link to="/olvide-clave" className="text-primario text-sm underline">
            ¿Olvidaste tu contraseña?
          </Link>
        </div>
        <Boton type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Ingresando…' : 'Ingresar'}
        </Boton>
      </form>
      <p className="text-texto-suave mt-5 text-center text-sm">
        ¿No tienes cuenta?{' '}
        <Link to="/registro" className="text-primario font-medium underline">
          Regístrate
        </Link>
      </p>
    </Tarjeta>
  )
}
