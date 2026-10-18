import { ArrowLeft } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { useSedesAdmin } from '@/caracteristicas/sedes/consultas'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, CampoTexto } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Modal } from '@/componentes/ui/Modal'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import {
  aIsoBogota,
  fechaEnBogota,
  formatearCOP,
  formatearFechaHora,
  formatearHora,
  hoyEnBogota,
} from '@/lib/formato'

import {
  useAccionesPartido,
  useAccionesTorneo,
  useDetallePartido,
  usePlantillaTorneo,
  useTorneo,
} from '../consultas'
import { EncabezadoPartido } from '../EncabezadoPartido'
import type { ConflictoHorario, DetallePartido, Partido, Torneo } from '../tipos'
import { golesDePlanilla, nombreLado } from '../utilidades'
import { PlanillaPartido } from './PlanillaPartido'

const error = (e: unknown) => avisar.error(mensajeDeError(e))

function ModalProgramacion({
  partido,
  torneo,
  canchas,
  alCerrar,
}: {
  partido: Partido
  torneo: Torneo
  canchas: { id: number; nombre: string }[]
  alCerrar: () => void
}) {
  const acciones = useAccionesPartido(partido.id)
  const [canchaId, setCanchaId] = useState(
    partido.canchaId?.toString() ?? canchas[0]?.id.toString() ?? '',
  )
  const [fecha, setFecha] = useState(partido.inicio ? fechaEnBogota(partido.inicio) : hoyEnBogota())
  const [hora, setHora] = useState(partido.inicio ? formatearHora(partido.inicio) : '18:00')
  const [duracion, setDuracion] = useState((partido.duracionMin ?? 60).toString())
  const [arbitro, setArbitro] = useState(partido.arbitro ?? '')
  const [conflictos, setConflictos] = useState<{
    lista: ConflictoHorario[]
    huella: string
  } | null>(null)

  const datos = (huella?: string) => ({
    canchaId: Number(canchaId),
    inicio: aIsoBogota(fecha, hora),
    duracionMin: Number(duracion),
    arbitro: arbitro.trim() || undefined,
    huellaConfirmada: huella,
  })
  const programar = (huella?: string) =>
    acciones.programar.mutate(datos(huella), {
      onSuccess: () => {
        avisar.exito('Partido programado y cancha reservada.')
        alCerrar()
      },
      onError: error,
    })
  const continuar = () =>
    acciones.previsualizar.mutate(datos(), {
      onSuccess: (p) =>
        p.requiereConfirmacion
          ? setConflictos({ lista: p.conflictos, huella: p.huella })
          : programar(),
      onError: error,
    })

  const editar = () => setConflictos(null)

  return (
    <Modal
      abierto
      titulo={partido.inicio ? 'Reprogramar partido' : 'Programar partido'}
      alCerrar={alCerrar}
    >
      <div className="flex flex-col gap-4">
        {!conflictos ? (
          <>
            <p className="text-sm">
              {nombreLado(partido.local)} vs {nombreLado(partido.visitante)}. Se reserva la cancha
              como un partido del torneo {torneo.nombre}.
            </p>
            <CampoSelect
              etiqueta="Cancha"
              value={canchaId}
              onChange={(e) => setCanchaId(e.target.value)}
            >
              {canchas.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombre}
                </option>
              ))}
            </CampoSelect>
            <div className="grid gap-4 sm:grid-cols-3">
              <Campo
                etiqueta="Fecha"
                type="date"
                value={fecha}
                onChange={(e) => setFecha(e.target.value)}
              />
              <Campo
                etiqueta="Hora de inicio"
                type="time"
                value={hora}
                onChange={(e) => setHora(e.target.value)}
              />
              <Campo
                etiqueta="Duración (min)"
                type="number"
                min={20}
                max={240}
                step={10}
                value={duracion}
                onChange={(e) => setDuracion(e.target.value)}
              />
            </div>
            <Campo
              etiqueta="Árbitro (opcional)"
              value={arbitro}
              onChange={(e) => setArbitro(e.target.value)}
            />
            <div className="flex justify-end gap-2">
              <Boton variante="secundario" onClick={alCerrar}>
                Cancelar
              </Boton>
              <Boton
                disabled={
                  !canchaId ||
                  !fecha ||
                  !hora ||
                  acciones.previsualizar.isPending ||
                  acciones.programar.isPending
                }
                onClick={continuar}
              >
                {acciones.previsualizar.isPending ? 'Revisando…' : 'Programar'}
              </Boton>
            </div>
          </>
        ) : (
          <>
            <Alerta tipo="aviso">
              Este horario choca con {conflictos.lista.length} reserva(s) de clientes. Si continúas,
              se cancelarán y se avisará a los clientes.
            </Alerta>
            <ul className="flex flex-col gap-2 text-sm">
              {conflictos.lista.map((c) => (
                <li key={c.reservaId} className="border-borde rounded-boton border p-3">
                  <p className="font-medium">
                    {c.codigo} · {c.cliente ?? 'Sin cliente'}
                  </p>
                  <p className="text-texto-suave">
                    {c.cancha} · {formatearFechaHora(c.inicio)} · {formatearCOP(c.precioFinal)}
                    {c.conPago ? ' · con pago registrado (quedará pendiente de reembolso)' : ''}
                  </p>
                </li>
              ))}
            </ul>
            <div className="flex justify-end gap-2">
              <Boton variante="secundario" onClick={editar}>
                Cambiar horario
              </Boton>
              <Boton
                variante="peligro"
                disabled={acciones.programar.isPending}
                onClick={() => programar(conflictos.huella)}
              >
                Cancelar reservas y programar
              </Boton>
            </div>
          </>
        )}
      </div>
    </Modal>
  )
}

function ModalResultado({
  detalle,
  torneoId,
  alCerrar,
}: {
  detalle: DetallePartido
  torneoId: number
  alCerrar: () => void
}) {
  const p = detalle.partido
  const acciones = useAccionesPartido(p.id)
  const local = usePlantillaTorneo(torneoId, p.local?.inscripcionId ?? null)
  const visitante = usePlantillaTorneo(torneoId, p.visitante?.inscripcionId ?? null)
  // Sin resultado aún, se propone el marcador que suma la planilla.
  const [planillaLocal, planillaVisitante] = golesDePlanilla(detalle)
  const [gl, setGl] = useState((p.golesLocal ?? planillaLocal).toString())
  const [gv, setGv] = useState((p.golesVisitante ?? planillaVisitante).toString())
  const [pl, setPl] = useState(p.penalesLocal?.toString() ?? '')
  const [pv, setPv] = useState(p.penalesVisitante?.toString() ?? '')
  const [figura, setFigura] = useState(p.figuraJugadorId?.toString() ?? '')
  const empate = gl !== '' && gl === gv
  const pidePenales = p.tipoFase === 'ELIMINATORIA' && empate
  const jugadores = [...(local.data ?? []), ...(visitante.data ?? [])]
  const menosQuePlanilla = Number(gl) < planillaLocal || Number(gv) < planillaVisitante

  const guardar = () =>
    acciones.resultado.mutate(
      {
        golesLocal: Number(gl),
        golesVisitante: Number(gv),
        penalesLocal: pidePenales ? Number(pl) : null,
        penalesVisitante: pidePenales ? Number(pv) : null,
        figuraJugadorId: figura ? Number(figura) : null,
      },
      {
        onSuccess: () => {
          avisar.exito('Resultado guardado.')
          alCerrar()
        },
        onError: error,
      },
    )

  return (
    <Modal abierto titulo="Resultado del partido" alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <div className="grid grid-cols-[1fr_5rem_5rem_1fr] items-end gap-2 text-center">
          <span className="pb-2 text-right font-medium">{nombreLado(p.local)}</span>
          <Campo
            etiqueta="Goles"
            type="number"
            min={0}
            value={gl}
            onChange={(e) => setGl(e.target.value)}
          />
          <Campo
            etiqueta="Goles"
            type="number"
            min={0}
            value={gv}
            onChange={(e) => setGv(e.target.value)}
          />
          <span className="pb-2 text-left font-medium">{nombreLado(p.visitante)}</span>
        </div>
        {pidePenales && (
          <div className="flex flex-col gap-2">
            <p className="text-sm font-medium">
              Empate en eliminatoria: define los penales (sin empate).
            </p>
            <div className="grid grid-cols-2 gap-2">
              <Campo
                etiqueta={`Penales ${nombreLado(p.local)}`}
                type="number"
                min={0}
                value={pl}
                onChange={(e) => setPl(e.target.value)}
              />
              <Campo
                etiqueta={`Penales ${nombreLado(p.visitante)}`}
                type="number"
                min={0}
                value={pv}
                onChange={(e) => setPv(e.target.value)}
              />
            </div>
          </div>
        )}
        {menosQuePlanilla && (
          <Alerta tipo="aviso">
            La planilla ya tiene {planillaLocal} - {planillaVisitante}: el marcador no puede ser
            menor. Borra goles de la planilla si fue un error.
          </Alerta>
        )}
        <CampoSelect
          etiqueta="Figura del partido (opcional)"
          value={figura}
          onChange={(e) => setFigura(e.target.value)}
        >
          <option value="">Sin figura</option>
          {jugadores.map((j) => (
            <option key={j.id} value={j.id}>
              {j.dorsal ? `${j.dorsal} · ` : ''}
              {j.nombreCompleto}
            </option>
          ))}
        </CampoSelect>
        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton
            disabled={
              gl === '' ||
              gv === '' ||
              (pidePenales && (pl === '' || pv === '')) ||
              menosQuePlanilla ||
              acciones.resultado.isPending
            }
            onClick={guardar}
          >
            Guardar resultado
          </Boton>
        </div>
      </div>
    </Modal>
  )
}

function ModalWO({ partido, alCerrar }: { partido: Partido; alCerrar: () => void }) {
  const acciones = useAccionesPartido(partido.id)
  const [ganador, setGanador] = useState<'LOCAL' | 'VISITANTE'>('LOCAL')
  const [motivo, setMotivo] = useState('')
  return (
    <Modal abierto titulo="Dar por W.O." alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <p className="text-sm">
          Gana el equipo que se presentó con el marcador de W.O. del torneo.
        </p>
        <CampoSelect
          etiqueta="¿Quién se presentó?"
          value={ganador}
          onChange={(e) => setGanador(e.target.value as 'LOCAL' | 'VISITANTE')}
        >
          <option value="LOCAL">{nombreLado(partido.local)}</option>
          <option value="VISITANTE">{nombreLado(partido.visitante)}</option>
        </CampoSelect>
        <CampoTexto
          etiqueta="Motivo (opcional)"
          value={motivo}
          onChange={(e) => setMotivo(e.target.value)}
        />
        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton
            variante="peligro"
            disabled={acciones.wo.isPending}
            onClick={() =>
              acciones.wo.mutate(
                { ganador, motivo: motivo.trim() || undefined },
                {
                  onSuccess: () => {
                    avisar.exito('Partido dado por W.O.')
                    alCerrar()
                  },
                  onError: error,
                },
              )
            }
          >
            Confirmar W.O.
          </Boton>
        </div>
      </div>
    </Modal>
  )
}

export function PaginaPartidoAdmin() {
  const id = Number(useParams().id)
  const partido = useDetallePartido(id, true)
  const detalle = partido.data
  const torneoId = detalle?.partido.torneoId ?? 0
  const torneo = useTorneo(torneoId, true)
  const sedes = useSedesAdmin()
  const acciones = useAccionesPartido(id)
  const { cambiarEstado } = useAccionesTorneo()
  const [dialogo, setDialogo] = useState<'programar' | 'resultado' | 'wo' | null>(null)

  const t = torneo.data?.torneo
  const p = detalle?.partido
  const sede = sedes.data?.find((s) => s.id === t?.sedeId)
  const permitidas = torneo.data?.canchaIds ?? []
  const canchas = (sede?.canchas ?? []).filter(
    (c) => permitidas.length === 0 || permitidas.includes(c.id),
  )
  const activo = t && t.estado !== 'FINALIZADO' && t.estado !== 'CANCELADO'
  const enCurso = t?.estado === 'EN_CURSO'
  const jugado = p?.estado === 'FINALIZADO' || p?.estado === 'WO'
  const listo = !!p?.local?.inscripcionId && !!p?.visitante?.inscripcionId && p.estado !== 'BYE'

  return (
    <div className="flex max-w-4xl flex-col gap-6">
      <Link
        to={t ? `/admin/torneos/${t.id}` : '/admin/torneos'}
        className="text-texto-suave hover:text-texto inline-flex items-center gap-1.5 self-start text-sm font-medium"
      >
        <ArrowLeft className="size-4" aria-hidden />
        {t?.nombre ?? 'Torneos'}
      </Link>
      {partido.isPending && <Cargando texto="Cargando partido…" />}
      {partido.isError && <Alerta tipo="error">{mensajeDeError(partido.error)}</Alerta>}
      {detalle && p && t && (
        <>
          <EncabezadoPartido
            detalle={detalle}
            admin
            mostrarEventos={!(enCurso && listo && !p.porWO)}
          />
          {!listo && (
            <Alerta tipo="info">
              Este partido aún tiene rivales por definir: se llenan solos al jugarse los partidos
              previos.
            </Alerta>
          )}
          <Tarjeta className="flex flex-wrap gap-2 p-4 sm:p-4">
            {activo && !jugado && p.estado !== 'BYE' && (
              <Boton onClick={() => setDialogo('programar')}>
                {p.inicio ? 'Reprogramar' : 'Programar'}
              </Boton>
            )}
            {activo && !jugado && p.inicio && (
              <>
                <Boton
                  variante="secundario"
                  onClick={() => acciones.desprogramar.mutate(true, { onError: error })}
                >
                  Aplazar
                </Boton>
                <Boton
                  variante="secundario"
                  onClick={() => acciones.desprogramar.mutate(false, { onError: error })}
                >
                  Quitar programación
                </Boton>
              </>
            )}
            {enCurso && listo && !jugado && p.estado !== 'EN_JUEGO' && (
              <Boton
                variante="secundario"
                onClick={() => acciones.iniciar.mutate(undefined, { onError: error })}
              >
                Marcar en juego
              </Boton>
            )}
            {enCurso && listo && (
              <Boton onClick={() => setDialogo('resultado')}>
                {jugado ? 'Editar resultado' : 'Registrar resultado'}
              </Boton>
            )}
            {enCurso && listo && !jugado && (
              <Boton variante="peligro" onClick={() => setDialogo('wo')}>
                W.O.
              </Boton>
            )}
            {enCurso && jugado && (
              <Boton
                variante="secundario"
                onClick={() => acciones.reabrir.mutate(undefined, { onError: error })}
              >
                Deshacer resultado
              </Boton>
            )}
          </Tarjeta>
          {!enCurso && activo && (
            <Alerta tipo="info">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <span>
                  Para registrar el resultado, los goles, las asistencias y las tarjetas, el torneo
                  debe estar en curso.
                </span>
                <Boton
                  tamano="sm"
                  disabled={cambiarEstado.isPending}
                  onClick={() =>
                    cambiarEstado.mutate(
                      { id: t.id, estado: 'EN_CURSO' },
                      { onSuccess: () => avisar.exito('Torneo iniciado.'), onError: error },
                    )
                  }
                >
                  Iniciar torneo
                </Boton>
              </div>
            </Alerta>
          )}
          {t.estado === 'FINALIZADO' && (
            <Alerta tipo="info">
              El torneo ya finalizó: sus resultados y la planilla quedan como registro.
            </Alerta>
          )}
          {enCurso && listo && !p.porWO && (
            <PlanillaPartido
              detalle={detalle}
              torneoId={t.id}
              alPedirResultado={() => setDialogo('resultado')}
            />
          )}
          {dialogo === 'programar' && (
            <ModalProgramacion
              partido={p}
              torneo={t}
              canchas={canchas}
              alCerrar={() => setDialogo(null)}
            />
          )}
          {dialogo === 'resultado' && (
            <ModalResultado detalle={detalle} torneoId={t.id} alCerrar={() => setDialogo(null)} />
          )}
          {dialogo === 'wo' && <ModalWO partido={p} alCerrar={() => setDialogo(null)} />}
        </>
      )}
    </div>
  )
}
