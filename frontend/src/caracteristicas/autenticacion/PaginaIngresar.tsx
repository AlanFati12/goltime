import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, Casilla } from '@/componentes/ui/Campo'
import { mensajeDeError } from '@/lib/api'
import { esquemaIngreso, type DatosIngreso } from '@/lib/validaciones'

import { useSesion } from './almacenSesion'
import { TarjetaAcceso } from './TarjetaAcceso'
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
    <TarjetaAcceso
      titulo="Bienvenido de nuevo"
      descripcion="Ingresa para reservar y gestionar tus partidos."
      pie={
        <>
          ¿No tienes cuenta?{' '}
          <Link to="/registro" className="enlace">
            Regístrate gratis
          </Link>
        </>
      }
    >
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
          <Link to="/olvide-clave" className="enlace text-sm">
            ¿Olvidaste tu contraseña?
          </Link>
        </div>
        <Boton
          type="submit"
          variante="volt"
          tamano="lg"
          disabled={isSubmitting}
          className="mt-1 w-full"
        >
          {isSubmitting ? 'Ingresando…' : 'Ingresar'}
        </Boton>
      </form>
    </TarjetaAcceso>
  )
}
