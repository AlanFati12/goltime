import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { useAccionesNotificaciones, useNotificaciones } from '@/caracteristicas/reservas/consultas'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearFechaHora } from '@/lib/formato'
import { cn } from '@/lib/utils'

import { rutaDeNotificacion } from './CampanaNotificaciones'

export function PaginaNotificaciones() {
  const rol = useSesion((s) => s.usuario!.rol)
  const navegar = useNavigate()
  const [pagina, setPagina] = useState(0)
  const { data, isPending, isError, error } = useNotificaciones(pagina)
  const { leer, leerTodas } = useAccionesNotificaciones()

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-col gap-4">
      <div className="flex items-center justify-between gap-2">
        <h1 className="text-2xl font-bold sm:text-3xl">Notificaciones</h1>
        <Boton
          variante="secundario"
          tamano="sm"
          onClick={() => leerTodas.mutate()}
          disabled={leerTodas.isPending}
        >
          Marcar todas como leídas
        </Boton>
      </div>
      {isPending && <Cargando />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.contenido.length === 0 && (
        <Tarjeta className="text-texto-suave text-center text-sm">
          No tienes notificaciones.
        </Tarjeta>
      )}
      <ul className="flex flex-col gap-2">
        {data?.contenido.map((n) => {
          const ruta = rutaDeNotificacion(n, rol)
          return (
            <li key={n.id}>
              <Tarjeta
                className={cn(
                  'flex items-start justify-between gap-3 p-4',
                  !n.leida && 'border-primario',
                )}
              >
                <div>
                  <p className="font-medium">{n.titulo}</p>
                  <p className="text-texto-suave text-sm">{n.mensaje}</p>
                  <p className="text-texto-suave mt-1 text-xs capitalize">
                    {formatearFechaHora(n.creadoEn)}
                  </p>
                </div>
                <div className="flex shrink-0 flex-col gap-1">
                  {ruta && (
                    <Boton
                      tamano="sm"
                      variante="secundario"
                      onClick={() => (!n.leida && leer.mutate(n.id), navegar(ruta))}
                    >
                      Ver
                    </Boton>
                  )}
                  {!n.leida && (
                    <Boton tamano="sm" variante="fantasma" onClick={() => leer.mutate(n.id)}>
                      Marcar leída
                    </Boton>
                  )}
                </div>
              </Tarjeta>
            </li>
          )
        })}
      </ul>
      {data && data.totalPaginas > 1 && (
        <div className="flex justify-between text-sm">
          <Boton
            tamano="sm"
            variante="secundario"
            disabled={pagina === 0}
            onClick={() => setPagina((p) => p - 1)}
          >
            Anterior
          </Boton>
          <Boton
            tamano="sm"
            variante="secundario"
            disabled={pagina + 1 >= data.totalPaginas}
            onClick={() => setPagina((p) => p + 1)}
          >
            Siguiente
          </Boton>
        </div>
      )}
    </div>
  )
}
