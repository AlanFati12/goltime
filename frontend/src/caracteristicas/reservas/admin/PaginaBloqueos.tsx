import { useState } from 'react'

import { useSedesAdmin } from '@/caracteristicas/sedes/consultas'
import { DIAS_CORTOS } from '@/caracteristicas/sedes/constantes'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { Modal } from '@/componentes/ui/Modal'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearDia, formatearFechaHora, fechaEnBogota } from '@/lib/formato'

import type { Bloqueo } from '../tipos'
import { useAccionesBloqueo, useBloqueos } from './consultas'
import { ETIQUETA_MOTIVO, FormularioBloqueo } from './FormularioBloqueo'

function cuando(b: Bloqueo): string {
  if (b.tipo === 'RECURRENTE') {
    return `${b.diasSemana.map((d) => DIAS_CORTOS[d - 1]).join(', ')} · ${b.horaInicio}–${b.horaFin} · del ${formatearDia(b.fechaDesde!)} al ${formatearDia(b.fechaHasta!)}`
  }
  if (b.todoElDia) {
    const ultimo = fechaEnBogota(new Date(new Date(b.fin!).getTime() - 1000))
    const primero = fechaEnBogota(b.inicio!)
    return primero === ultimo
      ? `Todo el día ${formatearDia(primero)}`
      : `Todo el día, del ${formatearDia(primero)} al ${formatearDia(ultimo)}`
  }
  return `${formatearFechaHora(b.inicio!)} → ${formatearFechaHora(b.fin!)}`
}

export function PaginaBloqueos() {
  const sedes = useSedesAdmin()
  const [sedeId, setSedeId] = useState<number | null>(null)
  const [verEliminados, setVerEliminados] = useState(false)
  const [formulario, setFormulario] = useState<{ bloqueo: Bloqueo | null } | null>(null)
  const [aEliminar, setAEliminar] = useState<Bloqueo | null>(null)
  const { data, isPending, isError, error } = useBloqueos(sedeId, !verEliminados)
  const { eliminar } = useAccionesBloqueo()

  const sedePorId = new Map(sedes.data?.map((s) => [s.id, s]))
  const nombresCanchas = (b: Bloqueo) => {
    const sede = sedePorId.get(b.sedeId)
    if (b.canchaIds.length === 0) return 'Toda la sede'
    return b.canchaIds
      .map((id) => sede?.canchas.find((c) => c.id === id)?.nombre ?? `#${id}`)
      .join(', ')
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-bold">Bloqueos de horario</h1>
        <Boton onClick={() => setFormulario({ bloqueo: null })}>Nuevo bloqueo</Boton>
      </div>
      <p className="text-texto-suave max-w-3xl text-sm">
        Impide reservar en una sede o en canchas específicas (mantenimiento, evento privado,
        torneo…). Si hay reservas en ese horario, verás la lista y deberás confirmar antes de que se
        cancelen.
      </p>

      <Tarjeta className="flex flex-wrap items-end gap-4">
        <CampoSelect
          className="min-w-48"
          etiqueta="Sede"
          value={sedeId ?? ''}
          onChange={(e) => setSedeId(e.target.value ? Number(e.target.value) : null)}
        >
          <option value="">Todas</option>
          {sedes.data?.map((s) => (
            <option key={s.id} value={s.id}>
              {s.nombre}
            </option>
          ))}
        </CampoSelect>
        <label className="flex items-center gap-2 pb-2 text-sm">
          <input
            type="checkbox"
            className="accent-primario size-4"
            checked={verEliminados}
            onChange={(e) => setVerEliminados(e.target.checked)}
          />
          Mostrar eliminados
        </label>
      </Tarjeta>

      {isPending && <Cargando texto="Cargando bloqueos…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <Tarjeta className="text-texto-suave text-center text-sm">No hay bloqueos.</Tarjeta>
      )}
      <ul className="flex flex-col gap-3">
        {data?.map((b) => (
          <li key={b.id}>
            <Tarjeta className="flex flex-wrap items-start justify-between gap-3 p-4">
              <div className="flex flex-col gap-1">
                <div className="flex flex-wrap items-center gap-2">
                  <Insignia tono={b.activo ? 'ambar' : 'gris'}>
                    {ETIQUETA_MOTIVO[b.motivo]}
                  </Insignia>
                  <Insignia>{b.tipo === 'PUNTUAL' ? 'Puntual' : 'Recurrente'}</Insignia>
                  {!b.activo && <Insignia tono="rojo">Eliminado</Insignia>}
                </div>
                <p className="font-medium">
                  {sedePorId.get(b.sedeId)?.nombre} · {nombresCanchas(b)}
                </p>
                <p className="text-sm capitalize">{cuando(b)}</p>
                {b.detalle && <p className="text-texto-suave text-sm">{b.detalle}</p>}
              </div>
              {b.activo && (
                <div className="flex gap-2">
                  <Boton
                    tamano="sm"
                    variante="secundario"
                    onClick={() => setFormulario({ bloqueo: b })}
                  >
                    Editar
                  </Boton>
                  <Boton tamano="sm" variante="secundario" onClick={() => setAEliminar(b)}>
                    Eliminar
                  </Boton>
                </div>
              )}
            </Tarjeta>
          </li>
        ))}
      </ul>

      {formulario && (
        <FormularioBloqueo bloqueo={formulario.bloqueo} alCerrar={() => setFormulario(null)} />
      )}
      {aEliminar && (
        <Modal abierto titulo="Eliminar bloqueo" alCerrar={() => setAEliminar(null)}>
          <div className="flex flex-col gap-4">
            <p className="text-sm">
              Las canchas volverán a estar disponibles para reservar en ese horario. El cambio queda
              en la bitácora.
            </p>
            <div className="flex justify-end gap-2">
              <Boton variante="secundario" onClick={() => setAEliminar(null)}>
                Volver
              </Boton>
              <Boton
                variante="peligro"
                disabled={eliminar.isPending}
                onClick={() =>
                  eliminar.mutate(aEliminar.id, {
                    onSuccess: () => (avisar.exito('Bloqueo eliminado.'), setAEliminar(null)),
                    onError: (e) => avisar.error(mensajeDeError(e)),
                  })
                }
              >
                Eliminar bloqueo
              </Boton>
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}
