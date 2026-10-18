import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { mensajeDeError } from '@/lib/api'

import { verificarCorreo } from './servicioAutenticacion'
import { TarjetaAcceso } from './TarjetaAcceso'

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
    <TarjetaAcceso
      titulo="Verificar correo"
      pie={
        <Link to="/ingresar" className="enlace">
          Ir a ingresar
        </Link>
      }
    >
      {!token && <Alerta tipo="error">El enlace no es válido.</Alerta>}
      {token && isPending && <Cargando texto="Verificando tu correo…" />}
      {token && isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {token && !isPending && !isError && <Alerta tipo="exito">¡Tu correo fue verificado!</Alerta>}
    </TarjetaAcceso>
  )
}
