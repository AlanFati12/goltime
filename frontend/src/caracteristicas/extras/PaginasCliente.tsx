import { Heart, Hourglass, Trophy } from 'lucide-react'
import { Link } from 'react-router-dom'

import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { SECCIONES_CUENTA } from '@/componentes/layout/seccionesCuenta'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearFechaHora } from '@/lib/formato'

import { BotonFavorito } from './BotonFavorito'
import {
  useAccionesEspera,
  useAccionesSolicitud,
  useFavoritas,
  useMisEsperas,
  useMisSolicitudes,
} from './consultas'

export function PaginaMiCuenta() {
  const usuario = useSesion((u) => u.usuario)
  return (
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo={`Hola, ${usuario?.nombres.split(' ')[0] ?? ''}`}
        descripcion="Desde aquí gestionas tus reservas, premios y datos."
      />
      <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        {SECCIONES_CUENTA.filter((x) => x.a !== '/mi-cuenta').map((x) => (
          <li key={x.a}>
            <Link to={x.a} className="group block h-full">
              <Tarjeta className="group-hover:border-primario/40 flex h-full items-start gap-4 p-5 transition-colors duration-200 sm:p-5">
                <span className="bg-primario-claro text-primario flex size-10 shrink-0 items-center justify-center rounded-xl">
                  <x.icono className="size-5" aria-hidden />
                </span>
                <span>
                  <span className="block font-semibold">{x.texto}</span>
                  <span className="text-texto-suave block text-sm">{x.descripcion}</span>
                </span>
              </Tarjeta>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}

export function PaginaFavoritos() {
  const { data, isPending, isError, error } = useFavoritas()
  return (
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Canchas favoritas"
        descripcion="Tus canchas preferidas, a un toque de reservar."
      />
      {isPending && <Cargando texto="Cargando favoritas…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <EstadoVacio
          icono={Heart}
          titulo="Aún no tienes favoritas"
          descripcion="Toca el corazón en la página de una cancha para guardarla aquí."
          accion={
            <EnlaceBoton to="/sedes" tamano="sm" variante="secundario">
              Explorar canchas
            </EnlaceBoton>
          }
        />
      )}
      <ul className="grid gap-3 sm:grid-cols-2">
        {data?.map((f) => (
          <li key={f.canchaId}>
            <Tarjeta className="flex items-center justify-between gap-3">
              <div>
                <Link to={`/canchas/${f.canchaId}`} className="hover:text-primario font-semibold">
                  {f.cancha}
                </Link>
                <p className="text-texto-suave text-sm">
                  {f.sede} · {f.modalidad} · desde {formatearCOP(f.precioBaseHora)}/h
                </p>
                {!f.activa && (
                  <Insignia tono="ambar" className="mt-1.5">
                    No disponible por ahora
                  </Insignia>
                )}
              </div>
              <div className="flex items-center gap-2">
                {f.activa && (
                  <EnlaceBoton
                    to={`/reservar?sedeId=${f.sedeId}&canchaId=${f.canchaId}`}
                    tamano="sm"
                  >
                    Reservar
                  </EnlaceBoton>
                )}
                <BotonFavorito canchaId={f.canchaId} />
              </div>
            </Tarjeta>
          </li>
        ))}
      </ul>
    </div>
  )
}

export function PaginaListaEspera() {
  const { data, isPending, isError, error } = useMisEsperas()
  const { salir } = useAccionesEspera()
  return (
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Lista de espera"
        descripcion="Te avisamos en la app y por correo cuando uno de estos horarios se libere. El primero en solicitarlo se lo queda."
      />
      {isPending && <Cargando texto="Cargando…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <EstadoVacio
          icono={Hourglass}
          titulo="No estás esperando ningún horario"
          descripcion="Cuando un horario esté ocupado, toca «Avisarme» para entrar a la lista."
        />
      )}
      <ul className="flex flex-col gap-3">
        {data?.map((e) => (
          <li key={e.id}>
            <Tarjeta className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="font-semibold capitalize">{formatearFechaHora(e.inicio)}</p>
                <p className="text-texto-suave text-sm">
                  {e.cancha} · {e.sede}
                </p>
                {e.estado === 'NOTIFICADA' ? (
                  <Insignia tono="verde" className="mt-1.5">
                    ¡Se liberó! Reserva ya
                  </Insignia>
                ) : (
                  <Insignia className="mt-1.5">Esperando</Insignia>
                )}
              </div>
              <div className="flex gap-2">
                {e.estado === 'NOTIFICADA' && (
                  <EnlaceBoton to={`/reservar?canchaId=${e.canchaId}`} tamano="sm">
                    Reservar
                  </EnlaceBoton>
                )}
                <Boton
                  tamano="sm"
                  variante="secundario"
                  onClick={() =>
                    salir.mutate(e.id, { onError: (x) => avisar.error(mensajeDeError(x)) })
                  }
                >
                  Quitar
                </Boton>
              </div>
            </Tarjeta>
          </li>
        ))}
      </ul>
    </div>
  )
}

const TONO = {
  SOLICITADA: 'ambar',
  APROBADA: 'verde',
  RECHAZADA: 'rojo',
  RETIRADA: 'gris',
} as const
const TEXTO = {
  SOLICITADA: 'Pendiente de aprobación',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  RETIRADA: 'Retirada',
} as const

export function PaginaMisSolicitudes() {
  const { data, isPending, isError, error } = useMisSolicitudes()
  const { retirar } = useAccionesSolicitud()
  return (
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Mis torneos"
        descripcion="Solicitudes de inscripción de tus equipos."
        acciones={
          data?.habilitado && (
            <EnlaceBoton to="/torneos" variante="secundario">
              Ver torneos
            </EnlaceBoton>
          )
        }
      />
      {isPending && <Cargando texto="Cargando…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && !data.habilitado && (
        <Alerta tipo="info">
          Tu cuenta no está habilitada para inscribir equipos. Pídele al administrador que la
          habilite.
        </Alerta>
      )}
      {data?.solicitudes.length === 0 && data.habilitado && (
        <EstadoVacio
          icono={Trophy}
          titulo="Aún no has pedido inscribir equipos"
          descripcion="Elige un torneo con inscripciones abiertas y toca «Solicitar inscripción»."
        />
      )}
      <ul className="flex flex-col gap-3">
        {data?.solicitudes.map((s) => (
          <li key={s.inscripcionId}>
            <Tarjeta className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <Link to={`/torneos/${s.torneoId}`} className="hover:text-primario font-semibold">
                  {s.equipo}
                </Link>
                <p className="text-texto-suave text-sm">{s.torneo}</p>
                <Insignia tono={TONO[s.estado]} className="mt-1.5">
                  {TEXTO[s.estado]}
                </Insignia>
              </div>
              {s.estado === 'SOLICITADA' && (
                <Boton
                  tamano="sm"
                  variante="secundario"
                  onClick={() =>
                    retirar.mutate(s.inscripcionId, {
                      onError: (x) => avisar.error(mensajeDeError(x)),
                    })
                  }
                >
                  Retirar solicitud
                </Boton>
              )}
            </Tarjeta>
          </li>
        ))}
      </ul>
    </div>
  )
}
