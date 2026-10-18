import { BellOff, CheckCheck } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { useAccionesNotificaciones, useNotificaciones } from '@/caracteristicas/reservas/consultas'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { Paginacion } from '@/componentes/ui/Paginacion'
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
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Notificaciones"
        acciones={
          <Boton
            variante="secundario"
            tamano="sm"
            onClick={() => leerTodas.mutate()}
            disabled={leerTodas.isPending}
          >
            <CheckCheck aria-hidden />
            Marcar todas como leídas
          </Boton>
        }
      />
      {isPending && <Cargando />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.contenido.length === 0 && (
        <EstadoVacio icono={BellOff} titulo="No tienes notificaciones" />
      )}
      <ul className="border-borde bg-superficie rounded-tarjeta shadow-suave divide-borde flex flex-col divide-y overflow-hidden border empty:hidden">
        {data?.contenido.map((n) => {
          const ruta = rutaDeNotificacion(n, rol)
          return (
            <li key={n.id}>
              <div
                className={cn(
                  'flex items-start justify-between gap-3 px-5 py-4',
                  !n.leida && 'bg-primario-claro/40',
                )}
              >
                <div className="flex gap-3">
                  <span
                    className={cn(
                      'mt-2 size-2 shrink-0 rounded-full',
                      n.leida ? 'bg-transparent' : 'bg-primario',
                    )}
                    aria-label={n.leida ? undefined : 'Sin leer'}
                  />
                  <div>
                    <p className="font-medium">{n.titulo}</p>
                    <p className="text-texto-suave text-sm">{n.mensaje}</p>
                    <p className="text-texto-suave mt-1 text-xs capitalize">
                      {formatearFechaHora(n.creadoEn)}
                    </p>
                  </div>
                </div>
                <div className="flex shrink-0 items-center gap-1">
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
              </div>
            </li>
          )
        })}
      </ul>
      {data && (
        <Paginacion pagina={data.pagina} totalPaginas={data.totalPaginas} alCambiar={setPagina} />
      )}
    </div>
  )
}
