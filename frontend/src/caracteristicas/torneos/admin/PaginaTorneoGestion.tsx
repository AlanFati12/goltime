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
import { estiloItemMenu } from '@/componentes/ui/estilosMenu'
import { Pestanas } from '@/componentes/ui/Pestanas'
import { Modal } from '@/componentes/ui/Modal'
import { ModalMotivo } from '@/componentes/ui/ModalMotivo'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'

import {
  useAccionesPlantilla,
  useAccionesTorneo,
  useEquipos,
  usePlantillaTorneo,
  useTorneo,
} from '../consultas'
import { PanelTorneo } from '../PanelTorneo'
import { ResumenTorneo } from '../PaginaTorneoDetalle'
import type { DetalleTorneo, EstadoTorneo, Inscripcion } from '../tipos'

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
  const plantilla = usePlantillaTorneo(torneoId, inscripcion.id)
  const { agregar, quitar } = useAccionesPlantilla(torneoId, inscripcion.id)
  const [nombres, setNombres] = useState('')
  const [apellidos, setApellidos] = useState('')
  const [dorsal, setDorsal] = useState('')
  const error = (e: unknown) => avisar.error(mensajeDeError(e))

  return (
    <Modal abierto titulo={`Plantilla de ${inscripcion.equipo}`} alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        {plantilla.isPending && <Cargando texto="Cargando plantilla…" />}
        <ul className="flex flex-col gap-1 text-sm">
          {plantilla.data?.map((j) => (
            <li key={j.id} className="border-borde flex items-center justify-between border-b py-1">
              <span>
                <span className="text-texto-suave inline-block w-8">{j.dorsal ?? ''}</span>
                {j.nombreCompleto}
              </span>
              {editable && (
                <Boton
                  tamano="sm"
                  variante="fantasma"
                  onClick={() => quitar.mutate(j.id, { onError: error })}
                >
                  Quitar
                </Boton>
              )}
            </li>
          ))}
        </ul>
        {editable && (
          <div className="grid grid-cols-[1fr_1fr_5rem_auto] items-end gap-2">
            <Campo
              etiqueta="Nombres"
              value={nombres}
              onChange={(e) => setNombres(e.target.value)}
            />
            <Campo
              etiqueta="Apellidos"
              value={apellidos}
              onChange={(e) => setApellidos(e.target.value)}
            />
            <Campo
              etiqueta="Dorsal"
              inputMode="numeric"
              value={dorsal}
              onChange={(e) => setDorsal(e.target.value.replace(/\D/g, '').slice(0, 2))}
            />
            <Boton
              disabled={!nombres.trim() || !apellidos.trim() || agregar.isPending}
              onClick={() =>
                agregar.mutate(
                  { nombres, apellidos, dorsal: dorsal === '' ? null : Number(dorsal) },
                  {
                    onSuccess: () => {
                      setNombres('')
                      setApellidos('')
                      setDorsal('')
                    },
                    onError: error,
                  },
                )
              }
            >
              Agregar
            </Boton>
          </div>
        )}
        <div className="flex justify-end">
          <Boton variante="secundario" onClick={alCerrar}>
            Cerrar
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
      <h2 className="text-lg font-semibold">
        Equipos inscritos ({detalle.inscripciones.length}
        {t.cupoEquipos ? ` de ${t.cupoEquipos}` : ''})
      </h2>
      <ul className="flex flex-col gap-2">
        {detalle.inscripciones.map((i) => (
          <li
            key={i.id}
            className="border-borde flex flex-wrap items-center justify-between gap-2 border-b pb-2"
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
  const { aprobar, rechazar } = useAccionesSolicitud()
  const [rechazando, setRechazando] = useState<Inscripcion | null>(null)
  const solicitudes = detalle.solicitudes ?? []
  if (solicitudes.length === 0) return null
  const error = (e: unknown) => avisar.error(mensajeDeError(e))
  return (
    <Tarjeta className="border-alerta flex flex-col gap-3 border-2">
      <h2 className="text-lg font-semibold">Solicitudes de inscripción ({solicitudes.length})</h2>
      <ul className="flex flex-col gap-2">
        {solicitudes.map((s) => (
          <li
            key={s.id}
            className="border-borde flex flex-wrap items-center justify-between gap-2 border-b pb-2"
          >
            <span>
              <span className="font-medium">{s.equipo}</span>{' '}
              <Insignia>{s.jugadores} jugadores</Insignia>
            </span>
            <span className="flex gap-2">
              <Boton
                tamano="sm"
                onClick={() =>
                  aprobar.mutate(
                    { torneoId: detalle.torneo.id, inscripcionId: s.id },
                    { onSuccess: () => avisar.exito('Equipo inscrito.'), onError: error },
                  )
                }
              >
                Aprobar
              </Boton>
              <Boton tamano="sm" variante="secundario" onClick={() => setRechazando(s)}>
                Rechazar
              </Boton>
            </span>
          </li>
        ))}
      </ul>
      {rechazando && (
        <ModalMotivo
          titulo="Rechazar solicitud"
          descripcion={`La inscripción de ${rechazando.equipo} no será aprobada y su responsable recibirá el motivo.`}
          textoConfirmar="Rechazar"
          alConfirmar={async (motivo) => {
            await rechazar
              .mutateAsync({ torneoId: detalle.torneo.id, inscripcionId: rechazando.id, motivo })
              .then(() => setRechazando(null), error)
          }}
          alCerrar={() => setRechazando(null)}
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
      <h2 className="text-lg font-semibold">Fixture</h2>
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
      <Link to="/admin/torneos" className="text-primario text-sm hover:underline">
        ← Torneos
      </Link>
      {isPending && <Cargando texto="Cargando torneo…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && (
        <>
          <EncabezadoPagina
            titulo={data.torneo.nombre}
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
                    boton={<span className="px-2 text-sm font-medium">Más ▾</span>}
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
              <p className="text-texto-suave text-sm">
                Abre un partido para programarlo, registrar el resultado y sus eventos.
              </p>
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
