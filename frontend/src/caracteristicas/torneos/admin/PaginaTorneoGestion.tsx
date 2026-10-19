import { ChevronDown, Eye } from 'lucide-react'
import { useAccionesSolicitud } from '@/caracteristicas/extras/consultas'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { Insignia } from '@/componentes/ui/Insignia'
import { MenuDesplegable } from '@/componentes/ui/MenuDesplegable'
import { estilosBoton } from '@/componentes/ui/estilosBoton'
import { estiloItemMenu } from '@/componentes/ui/estilosMenu'
import { Pestanas } from '@/componentes/ui/Pestanas'
import { Modal } from '@/componentes/ui/Modal'
import { ModalMotivo } from '@/componentes/ui/ModalMotivo'
import { Tarjeta, TituloTarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'

import {
  useAccionesPlantilla,
  useAccionesTorneo,
  useDetalleSolicitud,
  useEquipos,
  usePlantillaTorneo,
  useTorneo,
  type DatosEdicionJugador,
} from '../consultas'
import { PanelTorneo } from '../PanelTorneo'
import { ResumenTorneo } from '../PaginaTorneoDetalle'
import type { DetalleTorneo, EstadoTorneo, Inscripcion } from '../tipos'
import { ListaJugadores } from './Jugadores'
import { ListaPartidosAdmin } from './ListaPartidosAdmin'

const datosNuevo = (d: DatosEdicionJugador) => ({
  nombres: d.nombres.trim(),
  apellidos: d.apellidos.trim(),
  documento: d.documento.trim() || undefined,
  posicion: d.posicion.trim() || undefined,
  dorsal: d.dorsal,
})

/** Plantilla del equipo en este torneo, editable mientras el torneo no haya terminado. */
function PlantillaEditable({
  torneoId,
  inscripcionId,
  editable,
}: {
  torneoId: number
  inscripcionId: number
  editable: boolean
}) {
  const plantilla = usePlantillaTorneo(torneoId, inscripcionId)
  const { agregar, quitar, editar } = useAccionesPlantilla(torneoId, inscripcionId)
  if (plantilla.isPending) return <Cargando texto="Cargando plantilla…" />
  if (plantilla.isError) return <Alerta tipo="error">{mensajeDeError(plantilla.error)}</Alerta>
  return (
    <ListaJugadores
      jugadores={plantilla.data}
      ocupado={agregar.isPending || quitar.isPending || editar.isPending}
      alEditar={
        editable ? (jugadorId, datos) => editar.mutateAsync({ jugadorId, datos }) : undefined
      }
      alQuitar={editable ? (jugadorId) => quitar.mutateAsync(jugadorId) : undefined}
      alAgregar={editable ? (d) => agregar.mutateAsync(datosNuevo(d)) : undefined}
    />
  )
}

function ModalPlantilla({
  torneoId,
  inscripcion,
  editable,
  alCerrar,
}: {
  torneoId: number
  inscripcion: Inscripcion
  editable: boolean
  alCerrar: () => void
}) {
  return (
    <Modal abierto ancho="lg" titulo={`Plantilla de ${inscripcion.equipo}`} alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <p className="text-texto-suave text-sm">
          Cambios solo para este torneo. Los datos personales del jugador (nombre, documento) se
          corrigen en todos sus equipos.
        </p>
        <PlantillaEditable torneoId={torneoId} inscripcionId={inscripcion.id} editable={editable} />
        <div className="flex justify-end">
          <Boton variante="secundario" onClick={alCerrar}>
            Cerrar
          </Boton>
        </div>
      </div>
    </Modal>
  )
}

/** Todo lo de una solicitud para decidir: equipo, responsable y plantilla (que se puede corregir antes de aprobar). */
function ModalRevisarSolicitud({
  detalle,
  inscripcion,
  alCerrar,
}: {
  detalle: DetalleTorneo
  inscripcion: Inscripcion
  alCerrar: () => void
}) {
  const t = detalle.torneo
  const solicitud = useDetalleSolicitud(t.id, inscripcion.id)
  const { aprobar, rechazar } = useAccionesSolicitud()
  const [rechazando, setRechazando] = useState(false)
  const error = (e: unknown) => avisar.error(mensajeDeError(e))
  const d = solicitud.data
  const faltan = d ? Math.max(0, t.jugadoresPorEquipo - d.jugadores.length) : 0
  const conDocumento = d?.jugadores.filter((j) => j.documento).length ?? 0

  if (rechazando) {
    return (
      <ModalMotivo
        titulo="Rechazar solicitud"
        descripcion={`La inscripción de ${inscripcion.equipo} no será aprobada y su responsable recibirá el motivo.`}
        textoConfirmar="Rechazar"
        alConfirmar={async (motivo) => {
          await rechazar
            .mutateAsync({ torneoId: t.id, inscripcionId: inscripcion.id, motivo })
            .then(() => {
              avisar.exito('Solicitud rechazada.')
              alCerrar()
            }, error)
        }}
        alCerrar={() => setRechazando(false)}
      />
    )
  }

  return (
    <Modal abierto ancho="lg" titulo={`Solicitud de ${inscripcion.equipo}`} alCerrar={alCerrar}>
      <div className="flex flex-col gap-5">
        {solicitud.isPending && <Cargando texto="Cargando solicitud…" />}
        {solicitud.isError && <Alerta tipo="error">{mensajeDeError(solicitud.error)}</Alerta>}
        {d && (
          <>
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="border-borde rounded-boton border p-4">
                <p className="text-texto-suave mb-2 text-xs font-medium tracking-wide uppercase">
                  Equipo
                </p>
                <p className="flex items-center gap-2 font-semibold">
                  <span
                    className="ring-borde size-4 rounded ring-1"
                    style={{ backgroundColor: d.equipo.color ?? 'transparent' }}
                    aria-hidden
                  />
                  {d.equipo.nombre}
                </p>
                <dl className="text-texto-suave mt-2 flex flex-col gap-1 text-sm">
                  <div>
                    <dt className="inline">Delegado: </dt>
                    <dd className="text-texto inline">{d.equipo.delegadoNombre ?? '—'}</dd>
                  </div>
                  <div>
                    <dt className="inline">Teléfono: </dt>
                    <dd className="text-texto inline">{d.equipo.delegadoTelefono ?? '—'}</dd>
                  </div>
                </dl>
              </div>
              <div className="border-borde rounded-boton border p-4">
                <p className="text-texto-suave mb-2 text-xs font-medium tracking-wide uppercase">
                  Quién lo solicita
                </p>
                {d.responsable ? (
                  <>
                    <p className="font-semibold">{d.responsable.nombre}</p>
                    <p className="text-texto-suave mt-1 text-sm break-all">
                      {d.responsable.correo}
                    </p>
                    <p className="text-texto-suave text-sm">
                      {d.responsable.telefono ?? 'Sin celular'}
                    </p>
                    {!d.responsable.activo && (
                      <Insignia tono="rojo" className="mt-2">
                        Cuenta desactivada
                      </Insignia>
                    )}
                  </>
                ) : (
                  <p className="text-texto-suave text-sm">Sin cliente responsable.</p>
                )}
              </div>
            </div>

            <div className="flex flex-wrap gap-2">
              <Insignia tono={faltan > 0 ? 'ambar' : 'verde'}>
                {d.jugadores.length} jugadores (mínimo {t.jugadoresPorEquipo})
              </Insignia>
              <Insignia tono={conDocumento === d.jugadores.length ? 'verde' : 'gris'}>
                {conDocumento} de {d.jugadores.length} con documento
              </Insignia>
              {t.cupoEquipos != null && (
                <Insignia>
                  Cupos: {detalle.inscripciones.length} de {t.cupoEquipos}
                </Insignia>
              )}
            </div>
            {faltan > 0 && (
              <Alerta tipo="aviso">
                Le faltan {faltan} jugador(es) para el mínimo del torneo. Puedes agregarlos aquí
                antes de aprobar.
              </Alerta>
            )}

            <div>
              <h3 className="mb-2 text-sm font-semibold">Plantilla</h3>
              <PlantillaEditable torneoId={t.id} inscripcionId={inscripcion.id} editable />
            </div>
          </>
        )}
        <div className="border-borde -mx-6 -mb-6 flex flex-wrap justify-end gap-2 border-t px-6 py-4">
          <Boton variante="secundario" onClick={alCerrar}>
            Cerrar
          </Boton>
          <Boton variante="peligro" disabled={!d} onClick={() => setRechazando(true)}>
            Rechazar
          </Boton>
          <Boton
            disabled={!d || aprobar.isPending}
            onClick={() =>
              aprobar.mutate(
                { torneoId: t.id, inscripcionId: inscripcion.id },
                {
                  onSuccess: () => {
                    avisar.exito(`${inscripcion.equipo} quedó inscrito.`)
                    alCerrar()
                  },
                  onError: error,
                },
              )
            }
          >
            Aprobar inscripción
          </Boton>
        </div>
      </div>
    </Modal>
  )
}

function SeccionEquipos({ detalle }: { detalle: DetalleTorneo }) {
  const t = detalle.torneo
  const { inscribir, retirar } = useAccionesTorneo()
  const equipos = useEquipos('', false)
  const [equipoId, setEquipoId] = useState('')
  const [semilla, setSemilla] = useState('')
  const [plantilla, setPlantilla] = useState<Inscripcion | null>(null)
  const abiertas =
    (t.estado === 'BORRADOR' || t.estado === 'INSCRIPCIONES') && detalle.partidosTotales === 0
  const inscritos = new Set(detalle.inscripciones.map((i) => i.equipoId))
  const disponibles = equipos.data?.filter((r) => !inscritos.has(r.equipo.id)) ?? []
  const error = (e: unknown) => avisar.error(mensajeDeError(e))

  return (
    <Tarjeta className="flex flex-col gap-3">
      <h2 className="text-xl font-extrabold uppercase">
        Equipos inscritos ({detalle.inscripciones.length}
        {t.cupoEquipos ? ` de ${t.cupoEquipos}` : ''})
      </h2>
      <ul className="flex flex-col gap-2">
        {detalle.inscripciones.map((i) => (
          <li
            key={i.id}
            className="border-borde/70 flex flex-wrap items-center justify-between gap-2 border-b pb-2 last:border-0 last:pb-0"
          >
            <span className="flex items-center gap-2">
              <span
                className="size-3 rounded-full border"
                style={{ backgroundColor: i.color ?? 'transparent' }}
                aria-hidden="true"
              />
              <span className="font-medium">{i.equipo}</span>
              {i.semilla && <Insignia>Semilla {i.semilla}</Insignia>}
              <Insignia tono={i.jugadores < t.jugadoresPorEquipo ? 'ambar' : 'verde'}>
                {i.jugadores} jugadores
              </Insignia>
            </span>
            <span className="flex gap-2">
              <Boton tamano="sm" variante="secundario" onClick={() => setPlantilla(i)}>
                Plantilla
              </Boton>
              {abiertas && (
                <Boton
                  tamano="sm"
                  variante="fantasma"
                  onClick={() =>
                    retirar.mutate({ id: t.id, inscripcionId: i.id }, { onError: error })
                  }
                >
                  Retirar
                </Boton>
              )}
            </span>
          </li>
        ))}
        {detalle.inscripciones.length === 0 && (
          <li className="text-texto-suave text-sm">Aún no hay equipos.</li>
        )}
      </ul>
      {abiertas ? (
        <div className="grid grid-cols-[1fr_6rem_auto] items-end gap-2">
          <CampoSelect
            etiqueta="Inscribir equipo"
            value={equipoId}
            onChange={(e) => setEquipoId(e.target.value)}
          >
            <option value="">Elige un equipo</option>
            {disponibles.map((r) => (
              <option key={r.equipo.id} value={r.equipo.id}>
                {r.equipo.nombre} ({r.jugadores} jug.)
              </option>
            ))}
          </CampoSelect>
          <Campo
            etiqueta="Semilla"
            inputMode="numeric"
            value={semilla}
            onChange={(e) => setSemilla(e.target.value.replace(/\D/g, ''))}
          />
          <Boton
            disabled={!equipoId || inscribir.isPending}
            onClick={() =>
              inscribir.mutate(
                {
                  id: t.id,
                  equipoId: Number(equipoId),
                  semilla: semilla ? Number(semilla) : undefined,
                },
                {
                  onSuccess: () => {
                    setEquipoId('')
                    setSemilla('')
                    avisar.exito('Equipo inscrito.')
                  },
                  onError: error,
                },
              )
            }
          >
            Inscribir
          </Boton>
        </div>
      ) : (
        <p className="text-texto-suave text-xs">
          Los equipos no se pueden cambiar con el fixture generado o el torneo iniciado.
        </p>
      )}
      {plantilla && (
        <ModalPlantilla
          torneoId={t.id}
          inscripcion={plantilla}
          editable={t.estado !== 'FINALIZADO' && t.estado !== 'CANCELADO'}
          alCerrar={() => setPlantilla(null)}
        />
      )}
    </Tarjeta>
  )
}

function SeccionSolicitudes({ detalle }: { detalle: DetalleTorneo }) {
  const [revisando, setRevisando] = useState<Inscripcion | null>(null)
  const solicitudes = detalle.solicitudes ?? []
  if (solicitudes.length === 0) return null
  return (
    <Tarjeta className="flex flex-col gap-3 border-amber-300 bg-amber-50/40">
      <TituloTarjeta
        className="mb-0"
        titulo={`Solicitudes de inscripción (${solicitudes.length})`}
        descripcion="Revisa el equipo, quién lo inscribe y sus jugadores antes de aprobar o rechazar."
      />
      <ul className="flex flex-col gap-2">
        {solicitudes.map((s) => (
          <li
            key={s.id}
            className="border-borde/70 flex flex-wrap items-center justify-between gap-2 border-b pb-2 last:border-0 last:pb-0"
          >
            <span className="flex items-center gap-2">
              <span
                className="ring-borde size-3 rounded-full ring-1"
                style={{ backgroundColor: s.color ?? 'transparent' }}
                aria-hidden="true"
              />
              <span className="font-medium">{s.equipo}</span>
              <Insignia tono={s.jugadores < detalle.torneo.jugadoresPorEquipo ? 'ambar' : 'gris'}>
                {s.jugadores} jugadores
              </Insignia>
            </span>
            <Boton tamano="sm" onClick={() => setRevisando(s)}>
              <Eye aria-hidden />
              Revisar
            </Boton>
          </li>
        ))}
      </ul>
      {revisando && (
        <ModalRevisarSolicitud
          detalle={detalle}
          inscripcion={revisando}
          alCerrar={() => setRevisando(null)}
        />
      )}
    </Tarjeta>
  )
}

function SeccionFixture({ detalle }: { detalle: DetalleTorneo }) {
  const t = detalle.torneo
  const { generarFixture, descartarFixture } = useAccionesTorneo()
  const hay = detalle.partidosTotales > 0
  const terminado = t.estado === 'FINALIZADO' || t.estado === 'CANCELADO'
  const sinJugar = detalle.partidosJugados === 0
  const error = (e: unknown) => avisar.error(mensajeDeError(e))

  return (
    <Tarjeta className="flex flex-col gap-3">
      <h2 className="text-xl font-extrabold uppercase">Fixture</h2>
      <p className="text-texto-suave text-sm">
        {hay
          ? `${detalle.partidosTotales} partidos generados, ${detalle.partidosJugados} jugados.`
          : 'Aún no se ha generado el fixture.'}{' '}
        Se puede regenerar mientras no se haya jugado ningún partido.
      </p>
      {!terminado && (
        <div className="flex flex-wrap gap-2">
          <Boton
            disabled={
              generarFixture.isPending || (hay && !sinJugar) || detalle.inscripciones.length < 2
            }
            onClick={() =>
              generarFixture.mutate(
                { id: t.id },
                {
                  onSuccess: (r) =>
                    avisar.exito(
                      `Fixture generado: ${r.partidos} partidos${r.descansos ? `, ${r.descansos} descansos` : ''}.`,
                    ),
                  onError: error,
                },
              )
            }
          >
            {hay ? 'Regenerar fixture' : 'Generar fixture'}
          </Boton>
          {hay && sinJugar && (
            <Boton
              variante="secundario"
              onClick={() => descartarFixture.mutate(t.id, { onError: error })}
            >
              Descartar fixture
            </Boton>
          )}
        </div>
      )}
    </Tarjeta>
  )
}

const ACCIONES: Record<
  EstadoTorneo,
  { destino: EstadoTorneo; texto: string; peligro?: boolean }[]
> = {
  BORRADOR: [
    { destino: 'INSCRIPCIONES', texto: 'Abrir inscripciones' },
    { destino: 'EN_CURSO', texto: 'Iniciar torneo' },
    { destino: 'CANCELADO', texto: 'Cancelar torneo', peligro: true },
  ],
  INSCRIPCIONES: [
    { destino: 'EN_CURSO', texto: 'Iniciar torneo' },
    { destino: 'CANCELADO', texto: 'Cancelar torneo', peligro: true },
  ],
  EN_CURSO: [
    { destino: 'FINALIZADO', texto: 'Finalizar torneo' },
    { destino: 'CANCELADO', texto: 'Cancelar torneo', peligro: true },
  ],
  FINALIZADO: [],
  CANCELADO: [],
}

export function PaginaTorneoGestion() {
  const id = Number(useParams().id)
  const { data, isPending, isError, error } = useTorneo(id, true)
  const { cambiarEstado } = useAccionesTorneo()
  const [cancelando, setCancelando] = useState(false)
  const [elegida, setPestana] = useState<'equipos' | 'fixture' | 'partidos' | null>(null)
  const terminado = data?.torneo.estado === 'FINALIZADO' || data?.torneo.estado === 'CANCELADO'
  // Sin fixture lo siguiente es armar los equipos; con fixture, lo habitual es registrar partidos.
  const pestana = elegida ?? ((data?.partidosTotales ?? 0) > 0 ? 'partidos' : 'equipos')

  const cambiar = (destino: EstadoTorneo, motivo?: string) =>
    cambiarEstado.mutateAsync({ id, estado: destino, motivo }).then(
      () => {
        avisar.exito('Estado del torneo actualizado.')
        setCancelando(false)
      },
      (e) => avisar.error(mensajeDeError(e)),
    )

  return (
    <div className="flex flex-col gap-6">
      {isPending && <Cargando texto="Cargando torneo…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && (
        <>
          <EncabezadoPagina
            titulo={data.torneo.nombre}
            volver={{ a: '/admin/torneos', texto: 'Torneos' }}
            acciones={
              <>
                {ACCIONES[data.torneo.estado]
                  .filter((a) => !a.peligro)
                  .map((a) => (
                    <Boton
                      key={a.destino}
                      disabled={cambiarEstado.isPending}
                      onClick={() => void cambiar(a.destino)}
                    >
                      {a.texto}
                    </Boton>
                  ))}
                {!terminado && (
                  <MenuDesplegable
                    etiqueta="Más acciones del torneo"
                    claseBoton={estilosBoton({ variante: 'secundario' })}
                    boton={
                      <>
                        Más
                        <ChevronDown aria-hidden />
                      </>
                    }
                  >
                    <Link
                      role="menuitem"
                      to={`/admin/torneos/${id}/editar`}
                      className={estiloItemMenu}
                    >
                      Editar torneo
                    </Link>
                    <button
                      type="button"
                      role="menuitem"
                      className={`${estiloItemMenu} text-peligro`}
                      onClick={() => setCancelando(true)}
                    >
                      Cancelar torneo
                    </button>
                  </MenuDesplegable>
                )}
              </>
            }
          />
          <ResumenTorneo t={data.torneo} />
          <Pestanas
            etiqueta="Gestión del torneo"
            pestanas={[
              {
                id: 'equipos',
                texto: 'Equipos',
                conteo: data.solicitudes?.length ?? 0,
              },
              { id: 'fixture', texto: 'Fixture' },
              { id: 'partidos', texto: 'Partidos y resultados' },
            ]}
            actual={pestana}
            alCambiar={setPestana}
          />
          {pestana === 'equipos' && (
            <>
              <SeccionSolicitudes detalle={data} />
              <SeccionEquipos detalle={data} />
            </>
          )}
          {pestana === 'fixture' && <SeccionFixture detalle={data} />}
          {pestana === 'partidos' && (
            <>
              {data.torneo.estado !== 'EN_CURSO' && data.partidosTotales > 0 && !terminado && (
                <Alerta tipo="info">
                  Para cargar resultados, goles y tarjetas, inicia el torneo con el botón «Iniciar
                  torneo» de arriba.
                </Alerta>
              )}
              <ListaPartidosAdmin torneoId={id} enCurso={data.torneo.estado === 'EN_CURSO'} />
              <PanelTorneo detalle={data} admin />
            </>
          )}
          {cancelando && (
            <ModalMotivo
              titulo="Cancelar torneo"
              descripcion="Se liberarán las canchas reservadas para los partidos pendientes y se avisará a los responsables de los equipos."
              textoConfirmar="Cancelar torneo"
              alConfirmar={(motivo) => cambiar('CANCELADO', motivo)}
              alCerrar={() => setCancelando(false)}
            />
          )}
        </>
      )}
    </div>
  )
}
