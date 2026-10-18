import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { esquemaOlvideClave, type DatosOlvideClave } from '@/lib/validaciones'

import { solicitarRecuperacion } from './servicioAutenticacion'

export function PaginaOlvideClave() {
  const [enviado, setEnviado] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DatosOlvideClave>({ resolver: zodResolver(esquemaOlvideClave) })

  const alEnviar = handleSubmit(async ({ correo }) => {
    setError(null)
    try {
      await solicitarRecuperacion(correo)
      setEnviado(true)
    } catch (e) {
      setError(mensajeDeError(e))
    }
  })

  return (
    <Tarjeta className="mx-auto w-full max-w-md">
      <h1 className="mb-1 text-2xl font-bold">Recuperar contraseña</h1>
      {enviado ? (
        <>
          <Alerta tipo="exito" className="my-4">
            Si el correo está registrado, te enviamos un enlace para crear una contraseña nueva.
            Vence en 30 minutos.
          </Alerta>
          <Link to="/ingresar" className="text-primario text-sm underline">
            Volver a ingresar
          </Link>
        </>
      ) : (
        <>
          <p className="text-texto-suave mb-5 text-sm">
            Escribe tu correo y te enviaremos un enlace para restablecerla.
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
            <Boton type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Enviando…' : 'Enviar enlace'}
            </Boton>
          </form>
        </>
      )}
    </Tarjeta>
  )
}
