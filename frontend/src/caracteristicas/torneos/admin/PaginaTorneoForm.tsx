import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, Casilla, CampoTexto } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { useSedesAdmin } from '@/caracteristicas/sedes/consultas'
import { mensajeDeError } from '@/lib/api'

import { useAccionesTorneo, useTorneo, type DatosTorneo } from '../consultas'
import { ETIQUETA_CRITERIO, ETIQUETA_FORMATO, ETIQUETA_MODO } from '../etiquetas'
import type { DetalleTorneo, FormatoTorneo, ModoEmparejamiento } from '../tipos'

const aNumero = (v: string): number | undefined => (v.trim() === '' ? undefined : Number(v))

function Formulario({ detalle }: { detalle: DetalleTorneo | null }) {
  const navegar = useNavigate()
  const sedes = useSedesAdmin()
  const { crear, actualizar } = useAccionesTorneo()
  const t = detalle?.torneo
  const bloqueado = (detalle?.partidosTotales ?? 0) > 0

  const [nombre, setNombre] = useState(t?.nombre ?? '')
  const [descripcion, setDescripcion] = useState(t?.descripcion ?? '')
  const [sedeId, setSedeId] = useState(t?.sedeId.toString() ?? '')
  const [canchas, setCanchas] = useState<number[]>(detalle?.canchaIds ?? [])
  const [jugadores, setJugadores] = useState((t?.jugadoresPorEquipo ?? 5).toString())
  const [formato, setFormato] = useState<FormatoTorneo>(t?.formato ?? 'LIGA')
  const [modo, setModo] = useState<ModoEmparejamiento>(t?.modoEmparejamiento ?? 'ALEATORIO')
  const [cupo, setCupo] = useState(t?.cupoEquipos?.toString() ?? '')
  const [valor, setValor] = useState(t?.valorInscripcion?.toString() ?? '0')
  const [inicio, setInicio] = useState(t?.fechaInicio ?? '')
  const [fin, setFin] = useState(t?.fechaFin ?? '')
  const [idaYVuelta, setIdaYVuelta] = useState(t?.idaYVuelta ?? false)
  const [tercer, setTercer] = useState(t?.tercerPuesto ?? false)
  const [grupos, setGrupos] = useState(t?.numGrupos?.toString() ?? '2')
  const [clasifican, setClasifican] = useState(t?.clasificadosPorGrupo?.toString() ?? '2')
  const [pv, setPv] = useState((t?.puntosVictoria ?? 3).toString())
  const [pe, setPe] = useState((t?.puntosEmpate ?? 1).toString())
  const [pd, setPd] = useState((t?.puntosDerrota ?? 0).toString())
  const [wo, setWo] = useState((t?.marcadorWoGoles ?? 3).toString())
  const [premios, setPremios] = useState(t?.premiosTexto ?? '')
  const [reglamento, setReglamento] = useState(t?.reglamento ?? '')

  const sede = sedes.data?.find((s) => s.id === Number(sedeId))
  const valido = nombre.trim().length >= 3 && sedeId !== '' && Number(jugadores) >= 2

  const guardar = () => {
    const datos: DatosTorneo = {
      nombre: nombre.trim(),
      descripcion: descripcion.trim() || undefined,
      sedeId: Number(sedeId),
      jugadoresPorEquipo: Number(jugadores),
      formato,
      modoEmparejamiento: modo,
      cupoEquipos: aNumero(cupo),
      valorInscripcion: aNumero(valor) ?? 0,
      fechaInicio: inicio || undefined,
      fechaFin: fin || undefined,
      idaYVuelta: formato !== 'ELIMINACION_DIRECTA' && idaYVuelta,
      tercerPuesto: formato !== 'LIGA' && tercer,
      numGrupos: formato === 'GRUPOS_Y_ELIMINACION' ? aNumero(grupos) : undefined,
      clasificadosPorGrupo: formato === 'GRUPOS_Y_ELIMINACION' ? aNumero(clasifican) : undefined,
      puntosVictoria: aNumero(pv),
      puntosEmpate: aNumero(pe),
      puntosDerrota: aNumero(pd),
      marcadorWoGoles: aNumero(wo),
      premiosTexto: premios.trim() || undefined,
      reglamento: reglamento.trim() || undefined,
      criteriosDesempate: t?.criteriosDesempate,
      canchaIds: canchas,
    }
    const opciones = {
      onSuccess: (d: DetalleTorneo) => {
        avisar.exito(
          t ? 'Torneo actualizado.' : 'Torneo creado como borrador. Ahora inscribe los equipos.',
        )
        navegar(`/admin/torneos/${d.torneo.id}`)
      },
      onError: (e: unknown) => avisar.error(mensajeDeError(e)),
    }
    if (t) actualizar.mutate({ id: t.id, datos }, opciones)
    else crear.mutate(datos, opciones)
  }

  return (
    <div className="flex flex-col gap-4">
      <Tarjeta className="flex flex-col gap-4">
        <h2 className="text-lg font-semibold">1. Datos del torneo</h2>
        <Campo
          etiqueta="Nombre"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          maxLength={150}
        />
        <CampoTexto
          etiqueta="Descripción (opcional)"
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <CampoSelect
            etiqueta="Sede"
            value={sedeId}
            disabled={bloqueado}
            onChange={(e) => {
              setSedeId(e.target.value)
              setCanchas([])
            }}
          >
            <option value="">Elige una sede</option>
            {sedes.data?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nombre}
              </option>
            ))}
          </CampoSelect>
          <Campo
            etiqueta="Jugadores por equipo en cancha (2 a 11)"
            type="number"
            min={2}
            max={11}
            value={jugadores}
            onChange={(e) => setJugadores(e.target.value)}
            disabled={bloqueado}
          />
        </div>
        {sede && (
          <fieldset className="flex flex-col gap-2">
            <legend className="text-sm font-medium">
              Canchas del torneo (ninguna = cualquiera de la sede)
            </legend>
            <div className="flex flex-wrap gap-4">
              {sede.canchas.map((c) => (
                <Casilla
                  key={c.id}
                  etiqueta={c.nombre}
                  checked={canchas.includes(c.id)}
                  onChange={(e) =>
                    setCanchas(
                      e.target.checked ? [...canchas, c.id] : canchas.filter((x) => x !== c.id),
                    )
                  }
                />
              ))}
            </div>
          </fieldset>
        )}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Campo
            etiqueta="Inicio"
            type="date"
            value={inicio}
            onChange={(e) => setInicio(e.target.value)}
          />
          <Campo etiqueta="Fin" type="date" value={fin} onChange={(e) => setFin(e.target.value)} />
          <Campo
            etiqueta="Cupo de equipos"
            type="number"
            min={2}
            value={cupo}
            onChange={(e) => setCupo(e.target.value)}
          />
          <Campo
            etiqueta="Valor de inscripción (COP)"
            type="number"
            min={0}
            step={1000}
            value={valor}
            onChange={(e) => setValor(e.target.value)}
          />
        </div>
      </Tarjeta>

      <Tarjeta className="flex flex-col gap-4">
        <h2 className="text-lg font-semibold">2. Formato y reglas</h2>
        {bloqueado && (
          <Alerta tipo="info">
            El fixture ya está generado: formato y estructura no se pueden cambiar. Descártalo desde
            la gestión del torneo para modificarlos.
          </Alerta>
        )}
        <div className="grid gap-4 sm:grid-cols-2">
          <CampoSelect
            etiqueta="Formato"
            value={formato}
            disabled={bloqueado}
            onChange={(e) => setFormato(e.target.value as FormatoTorneo)}
          >
            {(Object.keys(ETIQUETA_FORMATO) as FormatoTorneo[]).map((f) => (
              <option key={f} value={f}>
                {ETIQUETA_FORMATO[f]}
              </option>
            ))}
          </CampoSelect>
          <CampoSelect
            etiqueta="Emparejamiento"
            value={modo}
            disabled={bloqueado}
            onChange={(e) => setModo(e.target.value as ModoEmparejamiento)}
          >
            {(Object.keys(ETIQUETA_MODO) as ModoEmparejamiento[]).map((m) => (
              <option key={m} value={m}>
                {ETIQUETA_MODO[m]}
              </option>
            ))}
          </CampoSelect>
        </div>
        {formato === 'GRUPOS_Y_ELIMINACION' && (
          <div className="grid gap-4 sm:grid-cols-2">
            <Campo
              etiqueta="Número de grupos"
              type="number"
              min={2}
              value={grupos}
              disabled={bloqueado}
              onChange={(e) => setGrupos(e.target.value)}
            />
            <Campo
              etiqueta="Clasificados por grupo"
              type="number"
              min={1}
              value={clasifican}
              disabled={bloqueado}
              onChange={(e) => setClasifican(e.target.value)}
            />
          </div>
        )}
        <div className="flex flex-wrap gap-6">
          {formato !== 'ELIMINACION_DIRECTA' && (
            <Casilla
              etiqueta="Ida y vuelta"
              checked={idaYVuelta}
              disabled={bloqueado}
              onChange={(e) => setIdaYVuelta(e.target.checked)}
            />
          )}
          {formato !== 'LIGA' && (
            <Casilla
              etiqueta="Partido por el tercer puesto"
              checked={tercer}
              disabled={bloqueado}
              onChange={(e) => setTercer(e.target.checked)}
            />
          )}
        </div>
        {formato !== 'ELIMINACION_DIRECTA' && (
          <>
            <div className="grid gap-4 sm:grid-cols-4">
              <Campo
                etiqueta="Puntos por victoria"
                type="number"
                value={pv}
                onChange={(e) => setPv(e.target.value)}
              />
              <Campo
                etiqueta="Puntos por empate"
                type="number"
                value={pe}
                onChange={(e) => setPe(e.target.value)}
              />
              <Campo
                etiqueta="Puntos por derrota"
                type="number"
                value={pd}
                onChange={(e) => setPd(e.target.value)}
              />
              <Campo
                etiqueta="Goles de un W.O."
                type="number"
                min={1}
                value={wo}
                onChange={(e) => setWo(e.target.value)}
              />
            </div>
            <p className="text-texto-suave text-xs">
              Desempate, en orden:{' '}
              {(
                t?.criteriosDesempate ?? [
                  'PUNTOS',
                  'DIFERENCIA_GOL',
                  'GOLES_A_FAVOR',
                  'ENFRENTAMIENTO_DIRECTO',
                  'JUEGO_LIMPIO',
                  'SORTEO',
                ]
              )
                .map((c) => ETIQUETA_CRITERIO[c])
                .join(' → ')}
              .
            </p>
          </>
        )}
        {formato === 'ELIMINACION_DIRECTA' && (
          <Campo
            etiqueta="Goles de un W.O."
            type="number"
            min={1}
            value={wo}
            onChange={(e) => setWo(e.target.value)}
          />
        )}
        <CampoTexto
          etiqueta="Premios (opcional)"
          value={premios}
          onChange={(e) => setPremios(e.target.value)}
        />
        <CampoTexto
          etiqueta="Reglamento (opcional)"
          rows={5}
          value={reglamento}
          onChange={(e) => setReglamento(e.target.value)}
        />
      </Tarjeta>

      <div className="flex justify-end gap-2">
        <Link
          to={t ? `/admin/torneos/${t.id}` : '/admin/torneos'}
          className="border-borde rounded-boton inline-flex h-10 items-center border px-4 text-sm"
        >
          Cancelar
        </Link>
        <Boton disabled={!valido || crear.isPending || actualizar.isPending} onClick={guardar}>
          {t ? 'Guardar cambios' : 'Crear torneo'}
        </Boton>
      </div>
    </div>
  )
}

export function PaginaTorneoForm() {
  const { id } = useParams()
  const torneoId = id ? Number(id) : null
  const { data, isPending, isError, error } = useTorneo(torneoId ?? 0, true)
  return (
    <div className="flex max-w-4xl flex-col gap-4">
      <h1 className="text-2xl font-bold">{torneoId ? 'Editar torneo' : 'Nuevo torneo'}</h1>
      {torneoId && isPending && <Cargando texto="Cargando torneo…" />}
      {torneoId && isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {!torneoId && <Formulario detalle={null} />}
      {torneoId && data && <Formulario detalle={data} />}
    </div>
  )
}
