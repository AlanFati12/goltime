import { useQuery } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, Casilla } from '@/componentes/ui/Campo'
import { Cargando } from '@/componentes/ui/Cargando'
import { mensajeDeError } from '@/lib/api'

import { useSesion } from './almacenSesion'
import { BotonGoogle } from './BotonGoogle'
import {
  cargarConfiguracionIngreso,
  destinoTrasIngresar,
  ingresarConGoogle,
  rutaInicial,
} from './servicioAutenticacion'
import { TarjetaAcceso } from './TarjetaAcceso'

/**
 * Único acceso a Goltime: «Continuar con Google». No hay registro ni contraseñas; la primera vez que alguien entra se
 * le crea la cuenta y se le pide completar su perfil. En desarrollo (`GOOGLE_MODO_PRUEBA`) también aparece un ingreso
 * de mentira para trabajar sin cuenta de Google.
 */
export function PaginaIngresar() {
  const usuario = useSesion((s) => s.usuario)
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const config = useQuery({
    queryKey: ['autenticacion', 'configuracion'],
    queryFn: cargarConfiguracionIngreso,
    staleTime: Infinity,
  })
  const [error, setError] = useState<string | null>(null)
  const [entrando, setEntrando] = useState(false)
  const [recordarme, setRecordarme] = useState(true)
  const recordarmeActual = useRef(recordarme)
  const [correoPrueba, setCorreoPrueba] = useState('')
  const [nombrePrueba, setNombrePrueba] = useState('')

  if (usuario) return <Navigate to={rutaInicial(usuario)} replace />

  const entrar = async (credencial: string) => {
    setError(null)
    setEntrando(true)
    try {
      const u = await ingresarConGoogle(credencial, recordarmeActual.current)
      const desde = (ubicacion.state as { desde?: string } | null)?.desde
      navegar(destinoTrasIngresar(u, desde), { replace: true, state: { desde } })
    } catch (e) {
      setError(mensajeDeError(e))
      setEntrando(false)
    }
  }

  return (
    <TarjetaAcceso
      titulo="Ingresa a Goltime"
      descripcion="Entra con tu cuenta de Google. Si es tu primera vez, tu cuenta se crea al instante."
      pie={
        <>
          Al continuar aceptas los{' '}
          <Link to="/terminos" className="underline">
            términos de uso
          </Link>{' '}
          y el tratamiento de tus datos personales según la{' '}
          <Link to="/privacidad" className="underline">
            política de privacidad
          </Link>
          .
        </>
      }
    >
      <div className="flex flex-col gap-5">
        {error && <Alerta tipo="error">{error}</Alerta>}
        {config.isPending && (
          <Cargando variante="giro" texto="Preparando el ingreso…" className="py-6" />
        )}
        {config.isError && <Alerta tipo="error">{mensajeDeError(config.error)}</Alerta>}

        {config.data && config.data.googleClientId && (
          <>
            <BotonGoogle clientId={config.data.googleClientId} alCredencial={entrar} />
            <Casilla
              etiqueta="Mantener la sesión iniciada en este dispositivo"
              checked={recordarme}
              onChange={(e) => {
                setRecordarme(e.target.checked)
                recordarmeActual.current = e.target.checked
              }}
            />
          </>
        )}

        {config.data && !config.data.googleClientId && !config.data.modoPrueba && (
          <Alerta tipo="aviso">
            El ingreso con Google todavía no está configurado en este sitio.
          </Alerta>
        )}

        {config.data?.modoPrueba && (
          <form
            className="border-borde flex flex-col gap-3 rounded-2xl border border-dashed p-4"
            onSubmit={(e) => {
              e.preventDefault()
              const [nombres, ...apellidos] = nombrePrueba.trim().split(/\s+/)
              void entrar(`prueba|${correoPrueba.trim()}|${nombres ?? ''}|${apellidos.join(' ')}`)
            }}
          >
            <p className="text-sm font-semibold">Ingreso de desarrollo (sin Google)</p>
            <p className="text-texto-suave text-xs">
              Solo existe en tu computador (<code>GOOGLE_MODO_PRUEBA=true</code>): escribe un correo
              y entras como si Google lo hubiera confirmado. El correo del administrador entra como
              administrador.
            </p>
            <Campo
              etiqueta="Correo"
              type="email"
              required
              value={correoPrueba}
              onChange={(e) => setCorreoPrueba(e.target.value)}
            />
            <Campo
              etiqueta="Nombre y apellido (opcional)"
              value={nombrePrueba}
              onChange={(e) => setNombrePrueba(e.target.value)}
            />
            <Boton type="submit" variante="secundario" disabled={entrando || !correoPrueba}>
              {entrando ? 'Entrando…' : 'Entrar (desarrollo)'}
            </Boton>
          </form>
        )}

        {entrando && <p className="text-texto-suave text-center text-sm">Ingresando…</p>}
      </div>
    </TarjetaAcceso>
  )
}
