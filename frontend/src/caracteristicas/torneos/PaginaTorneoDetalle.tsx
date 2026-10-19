import { useParams } from 'react-router-dom'

import { Medal, Trophy } from 'lucide-react'

import { Alerta } from '@/componentes/ui/Alerta'
import { Cargando } from '@/componentes/ui/Cargando'
import { InsigniaFoto, PortadaPagina } from '@/componentes/ui/PortadaPagina'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearDia } from '@/lib/formato'
import { cn } from '@/lib/utils'

import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { useMisSolicitudes } from '@/caracteristicas/extras/consultas'
import { ModalSolicitarInscripcion } from '@/caracteristicas/extras/ModalSolicitarInscripcion'
import { Boton } from '@/componentes/ui/Boton'
import { useState } from 'react'
import { useTorneo } from './consultas'
import { ETIQUETA_ESTADO_TORNEO, ETIQUETA_FORMATO, TONO_ESTADO_TORNEO } from './etiquetas'
import { PanelTorneo } from './PanelTorneo'
import type { Torneo } from './tipos'

export function ResumenTorneo({ t }: { t: Torneo }) {
  const podio = [
    { titulo: 'Campeón', nombre: t.campeon, clase: 'bg-volt text-noche' },
    { titulo: 'Subcampeón', nombre: t.subcampeon, clase: 'bg-noche text-white' },
    { titulo: 'Tercero', nombre: t.tercero, clase: 'bg-tenue text-texto' },
  ].filter((x) => x.nombre)
  return (
    <div className="flex flex-col gap-4">
      {t.estado === 'FINALIZADO' && podio.length > 0 && (
        <ol className="grid gap-3 sm:grid-cols-3">
          {podio.map((x, i) => (
            <li
              key={x.titulo}
              className={cn('flex items-center gap-4 rounded-[1.25rem] p-5', x.clase)}
            >
              {i === 0 ? (
                <Trophy className="size-8 shrink-0" aria-hidden />
              ) : (
                <Medal className="size-8 shrink-0 opacity-70" aria-hidden />
              )}
              <span>
                <span className="block text-sm font-semibold opacity-70">{x.titulo}</span>
                <span className="font-titulo block text-2xl leading-none font-extrabold uppercase">
                  {x.nombre}
                </span>
              </span>
            </li>
          ))}
        </ol>
      )}
      {(t.descripcion || t.premiosTexto || t.reglamento) && (
        <Tarjeta className="flex flex-col gap-3">
          {t.descripcion && <p className="text-lg">{t.descripcion}</p>}
          {t.premiosTexto && (
            <details className="bg-tenue group rounded-2xl px-5 py-4">
              <summary className="cursor-pointer font-semibold">Premios</summary>
              <p className="text-texto-suave mt-2 whitespace-pre-line">{t.premiosTexto}</p>
            </details>
          )}
          {t.reglamento && (
            <details className="bg-tenue group rounded-2xl px-5 py-4">
              <summary className="cursor-pointer font-semibold">Reglamento</summary>
              <p className="text-texto-suave mt-2 whitespace-pre-line">{t.reglamento}</p>
            </details>
          )}
        </Tarjeta>
      )}
    </div>
  )
}

/** Datos del torneo sobre la portada: estado, formato, sede, fechas e inscripción. */
function DatosTorneo({ t }: { t: Torneo }) {
  return (
    <>
      <div className="flex flex-wrap gap-2">
        <Insignia tono={TONO_ESTADO_TORNEO[t.estado]}>{ETIQUETA_ESTADO_TORNEO[t.estado]}</Insignia>
        <InsigniaFoto>{ETIQUETA_FORMATO[t.formato]}</InsigniaFoto>
        <InsigniaFoto>Fútbol {t.jugadoresPorEquipo}</InsigniaFoto>
      </div>
      <p>
        {t.sedeNombre}
        {t.fechaInicio && (
          <span className="capitalize">
            {' '}
            · {formatearDia(t.fechaInicio)}
            {t.fechaFin ? ` → ${formatearDia(t.fechaFin)}` : ''}
          </span>
        )}
        {t.valorInscripcion > 0 && ` · Inscripción ${formatearCOP(t.valorInscripcion)}`}
      </p>
    </>
  )
}

export function PaginaTorneoDetalle() {
  const id = Number(useParams().id)
  const { data, isPending, isError, error } = useTorneo(id, false)
  const usuario = useSesion((s) => s.usuario)
  const solicitudes = useMisSolicitudes(usuario?.rol === 'CLIENTE')
  const [solicitando, setSolicitando] = useState(false)
  const puedeSolicitar =
    data?.torneo.estado === 'INSCRIPCIONES' && solicitudes.data?.habilitado === true
  const yaSolicito =
    solicitudes.data?.solicitudes.some(
      (x) => x.torneoId === id && (x.estado === 'SOLICITADA' || x.estado === 'APROBADA'),
    ) ?? false
  return (
    <div className="flex flex-col gap-8">
      {isPending && <Cargando texto="Cargando torneo…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && (
        <>
          <PortadaPagina
            titulo={data.torneo.nombre}
            imagen="/img/jugadores-movimiento.webp"
            volver={{ a: '/torneos', texto: 'Torneos' }}
            acciones={
              puedeSolicitar &&
              (yaSolicito ? (
                <Insignia tono="verde">Solicitud enviada · revisa «Mis torneos»</Insignia>
              ) : (
                <Boton variante="volt" tamano="lg" onClick={() => setSolicitando(true)}>
                  Solicitar inscripción
                </Boton>
              ))
            }
          >
            <DatosTorneo t={data.torneo} />
          </PortadaPagina>
          <ResumenTorneo t={data.torneo} />
          <PanelTorneo detalle={data} />
          {solicitando && (
            <ModalSolicitarInscripcion
              torneoId={id}
              minimoJugadores={data.torneo.jugadoresPorEquipo}
              alCerrar={() => setSolicitando(false)}
            />
          )}
        </>
      )}
    </div>
  )
}
