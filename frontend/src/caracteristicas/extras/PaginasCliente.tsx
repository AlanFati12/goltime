import { Link } from 'react-router-dom'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Cargando } from '@/componentes/ui/Cargando'
import { EnlaceBoton } from '@/componentes/ui/EnlaceBoton'
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

const ACCESOS = [
  { a: '/mis-reservas', titulo: 'Mis reservas', texto: 'Próximas, historial y canceladas.' },
  {
    a: '/favoritos',
    titulo: 'Canchas favoritas',
    texto: 'Vuelve a reservar tus canchas preferidas.',
  },
  { a: '/lista-espera', titulo: 'Lista de espera', texto: 'Horarios ocupados que esperas.' },
  {
    a: '/mis-premios',
    titulo: 'Mis premios',
    texto: 'Tu progreso de fidelidad y premios ganados.',
  },
  { a: '/mis-torneos', titulo: 'Inscripciones a torneos', texto: 'Solicitudes de tus equipos.' },
  { a: '/notificaciones', titulo: 'Notificaciones', texto: 'Avisos sobre tus reservas.' },
  { a: '/perfil', titulo: 'Mi perfil', texto: 'Tus datos y contraseña.' },
]

export function PaginaMiCuenta() {
  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-4 p-4 sm:p-6">
      <h1 className="text-3xl font-bold">Mi cuenta</h1>
      <ul className="grid gap-3 sm:grid-cols-2">
        {ACCESOS.map((x) => (
          <li key={x.a}>
            <Link to={x.a} className="block h-full">
              <Tarjeta className="hover:border-primario h-full">
                <h2 className="font-semibold">{x.titulo}</h2>
                <p className="text-texto-suave text-sm">{x.texto}</p>
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
    <div className="mx-auto flex max-w-4xl flex-col gap-4 p-4 sm:p-6">
      <h1 className="text-3xl font-bold">Canchas favoritas</h1>
      {isPending && <Cargando texto="Cargando favoritas…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <Tarjeta className="text-texto-suave text-sm">
          Aún no tienes favoritas. Toca el corazón en la página de una cancha para guardarla aquí.
        </Tarjeta>
      )}
      <ul className="grid gap-3 sm:grid-cols-2">
        {data?.map((f) => (
          <li key={f.canchaId}>
            <Tarjeta className="flex items-center justify-between gap-3">
              <div>
                <Link to={`/canchas/${f.canchaId}`} className="font-semibold hover:underline">
                  {f.cancha}
                </Link>
                <p className="text-texto-suave text-sm">
                  {f.sede} · {f.modalidad} · desde {formatearCOP(f.precioBaseHora)}/h
                </p>
                {!f.activa && <Insignia tono="ambar">No disponible por ahora</Insignia>}
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
    <div className="mx-auto flex max-w-3xl flex-col gap-4 p-4 sm:p-6">
      <h1 className="text-3xl font-bold">Lista de espera</h1>
      <p className="text-texto-suave text-sm">
        Te avisamos en la app y por correo cuando uno de estos horarios se libere. El primero en
        solicitarlo se lo queda.
      </p>
      {isPending && <Cargando texto="Cargando…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <Tarjeta className="text-texto-suave text-sm">No estás esperando ningún horario.</Tarjeta>
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
                  <Insignia tono="verde">¡Se liberó! Reserva ya</Insignia>
                ) : (
                  <Insignia>Esperando</Insignia>
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
    <div className="mx-auto flex max-w-3xl flex-col gap-4 p-4 sm:p-6">
      <h1 className="text-3xl font-bold">Inscripciones a torneos</h1>
      {isPending && <Cargando texto="Cargando…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && !data.habilitado && (
        <Alerta tipo="info">
          Tu cuenta no está habilitada para inscribir equipos. Pídele al administrador que la
          habilite.
        </Alerta>
      )}
      {data && data.habilitado && (
        <p className="text-texto-suave text-sm">
          Elige un torneo con inscripciones abiertas y toca «Solicitar inscripción».{' '}
          <Link to="/torneos" className="text-primario underline">
            Ver torneos
          </Link>
        </p>
      )}
      {data?.solicitudes.length === 0 && data.habilitado && (
        <Tarjeta className="text-texto-suave text-sm">Aún no has pedido inscribir equipos.</Tarjeta>
      )}
      <ul className="flex flex-col gap-3">
        {data?.solicitudes.map((s) => (
          <li key={s.inscripcionId}>
            <Tarjeta className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <Link to={`/torneos/${s.torneoId}`} className="font-semibold hover:underline">
                  {s.equipo}
                </Link>
                <p className="text-texto-suave text-sm">{s.torneo}</p>
                <Insignia tono={TONO[s.estado]}>{TEXTO[s.estado]}</Insignia>
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
