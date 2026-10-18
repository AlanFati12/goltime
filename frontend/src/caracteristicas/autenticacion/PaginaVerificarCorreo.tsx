import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'

import { verificarCorreo } from './servicioAutenticacion'

export function PaginaVerificarCorreo() {
  const token = useSearchParams()[0].get('token')
  const { isPending, isError, error } = useQuery({
    queryKey: ['verificar-correo', token],
    queryFn: () => verificarCorreo(token!),
    enabled: !!token,
    retry: false,
    staleTime: Infinity,
  })

  return (
    <Tarjeta className="mx-auto w-full max-w-md">
      <h1 className="mb-4 text-2xl font-bold">Verificar correo</h1>
      {!token && <Alerta tipo="error">El enlace no es válido.</Alerta>}
      {token && isPending && <Cargando texto="Verificando tu correo…" />}
      {token && isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {token && !isPending && !isError && <Alerta tipo="exito">¡Tu correo fue verificado!</Alerta>}
      <Link to="/ingresar" className="text-primario mt-4 inline-block text-sm underline">
        Ir a ingresar
      </Link>
    </Tarjeta>
  )
}
