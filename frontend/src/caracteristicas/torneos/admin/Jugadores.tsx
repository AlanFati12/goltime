import { Pencil, Plus, Trash2, UserRound } from 'lucide-react'
import { useState } from 'react'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { mensajeDeError } from '@/lib/api'

import type { DatosEdicionJugador } from '../consultas'
import type { JugadorEquipo } from '../tipos'

const VACIO: DatosEdicionJugador = {
  nombres: '',
  apellidos: '',
  documento: '',
  posicion: '',
  dorsal: null,
}

const desdeJugador = (j: JugadorEquipo): DatosEdicionJugador => ({
  nombres: j.nombres,
  apellidos: j.apellidos,
  documento: j.documento ?? '',
  posicion: j.posicion ?? '',
  dorsal: j.dorsal,
})

/** Campos de un jugador en una sola fila: sirve para crear uno nuevo o corregir uno existente. */
export function FormularioJugador({
  inicial = VACIO,
  textoGuardar,
  ocupado,
  alGuardar,
  alCancelar,
}: {
  inicial?: DatosEdicionJugador
  textoGuardar: string
  ocupado?: boolean
  alGuardar: (d: DatosEdicionJugador) => Promise<unknown>
  alCancelar?: () => void
}) {
  const [d, setD] = useState(inicial)
  const valido = d.nombres.trim().length > 0 && d.apellidos.trim().length > 0
  const cambiar = (parcial: Partial<DatosEdicionJugador>) => setD((x) => ({ ...x, ...parcial }))

  const guardar = () =>
    alGuardar(d).then(
      () => alCancelar === undefined && setD(VACIO),
      (e) => avisar.error(mensajeDeError(e)),
    )

  return (
    <div className="bg-tenue/60 rounded-boton flex flex-col gap-3 p-3">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-[1fr_1fr_8rem_8rem_5rem]">
        <Campo
          etiqueta="Nombres"
          value={d.nombres}
          maxLength={100}
          onChange={(e) => cambiar({ nombres: e.target.value })}
        />
        <Campo
          etiqueta="Apellidos"
          value={d.apellidos}
          maxLength={100}
          onChange={(e) => cambiar({ apellidos: e.target.value })}
        />
        <Campo
          etiqueta="Documento"
          value={d.documento}
          maxLength={30}
          onChange={(e) => cambiar({ documento: e.target.value })}
        />
        <Campo
          etiqueta="Posición"
          value={d.posicion}
          maxLength={30}
          placeholder="Ej. Arquero"
          onChange={(e) => cambiar({ posicion: e.target.value })}
        />
        <Campo
          etiqueta="Dorsal"
          inputMode="numeric"
          value={d.dorsal ?? ''}
          onChange={(e) => {
            const v = e.target.value.replace(/\D/g, '').slice(0, 2)
            cambiar({ dorsal: v === '' ? null : Number(v) })
          }}
        />
      </div>
      <div className="flex justify-end gap-2">
        {alCancelar && (
          <Boton tamano="sm" variante="fantasma" onClick={alCancelar}>
            Cancelar
          </Boton>
        )}
        <Boton tamano="sm" disabled={!valido || ocupado} onClick={guardar}>
          {textoGuardar}
        </Boton>
      </div>
    </div>
  )
}

/**
 * Plantilla de un equipo con edición en línea: cada jugador se puede corregir o quitar, y se pueden agregar nuevos.
 * Si no se pasan las acciones, la lista es de solo lectura.
 */
export function ListaJugadores({
  jugadores,
  ocupado,
  alEditar,
  alQuitar,
  alAgregar,
}: {
  jugadores: JugadorEquipo[]
  ocupado?: boolean
  alEditar?: (jugadorId: number, d: DatosEdicionJugador) => Promise<unknown>
  alQuitar?: (jugadorId: number) => Promise<unknown>
  alAgregar?: (d: DatosEdicionJugador) => Promise<unknown>
}) {
  const [editando, setEditando] = useState<number | null>(null)
  const [agregando, setAgregando] = useState(false)
  const ordenados = [...jugadores].sort(
    (a, b) => (a.dorsal ?? 999) - (b.dorsal ?? 999) || a.apellidos.localeCompare(b.apellidos),
  )

  return (
    <div className="flex flex-col gap-3">
      {ordenados.length === 0 ? (
        <p className="text-texto-suave py-4 text-center text-sm">Aún no hay jugadores.</p>
      ) : (
        <ul className="divide-borde/70 flex flex-col divide-y">
          {ordenados.map((j) =>
            editando === j.id && alEditar ? (
              <li key={j.id} className="py-2">
                <FormularioJugador
                  inicial={desdeJugador(j)}
                  textoGuardar="Guardar cambios"
                  ocupado={ocupado}
                  alGuardar={(d) => alEditar(j.id, d).then(() => setEditando(null))}
                  alCancelar={() => setEditando(null)}
                />
              </li>
            ) : (
              <li key={j.id} className="flex items-center gap-3 py-2.5 text-sm">
                <span className="bg-tenue text-texto-suave inline-flex size-8 shrink-0 items-center justify-center rounded-full text-xs font-semibold tabular-nums">
                  {j.dorsal ?? <UserRound className="size-4" aria-hidden />}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block font-medium">{j.nombreCompleto}</span>
                  <span className="text-texto-suave block text-xs">
                    {[j.posicion, j.documento && `Doc. ${j.documento}`]
                      .filter(Boolean)
                      .join(' · ') || 'Sin posición ni documento'}
                  </span>
                </span>
                {alEditar && (
                  <Boton
                    tamano="icono"
                    variante="fantasma"
                    aria-label={`Editar a ${j.nombreCompleto}`}
                    onClick={() => setEditando(j.id)}
                  >
                    <Pencil aria-hidden />
                  </Boton>
                )}
                {alQuitar && (
                  <Boton
                    tamano="icono"
                    variante="fantasma"
                    className="hover:text-peligro"
                    aria-label={`Quitar a ${j.nombreCompleto}`}
                    disabled={ocupado}
                    onClick={() =>
                      window.confirm(`¿Quitar a ${j.nombreCompleto} de la plantilla?`) &&
                      alQuitar(j.id).catch((e) => avisar.error(mensajeDeError(e)))
                    }
                  >
                    <Trash2 aria-hidden />
                  </Boton>
                )}
              </li>
            ),
          )}
        </ul>
      )}
      {alAgregar &&
        (agregando ? (
          <FormularioJugador
            textoGuardar="Agregar jugador"
            ocupado={ocupado}
            alGuardar={(d) => alAgregar(d).then(() => setAgregando(false))}
            alCancelar={() => setAgregando(false)}
          />
        ) : (
          <Boton
            variante="secundario"
            tamano="sm"
            className="self-start"
            onClick={() => setAgregando(true)}
          >
            <Plus aria-hidden />
            Agregar jugador
          </Boton>
        ))}
    </div>
  )
}
