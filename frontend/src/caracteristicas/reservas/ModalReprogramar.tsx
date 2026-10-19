import { useEffect, useState } from 'react'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, CampoTexto } from '@/componentes/ui/Campo'
import { Cargando } from '@/componentes/ui/Cargando'
import { Modal } from '@/componentes/ui/Modal'
import { mensajeDeError } from '@/lib/api'
import {
  fechaEnBogota,
  formatearCOP,
  formatearFechaHora,
  formatearHora,
  hoyEnBogota,
} from '@/lib/formato'
import { cn } from '@/lib/utils'

import {
  useDisponibilidad,
  usePrevisualizarReprogramacion,
  useReprogramarReserva,
} from './consultas'
import type { Reserva } from './tipos'
import { rangoHorario, sumarMinutos } from './utilidades'

/** Reprogramar: elige fecha y un horario libre (de cualquier cancha de la misma modalidad) y ve la diferencia de precio antes de confirmar. */
export function ModalReprogramar({
  reserva,
  alCerrar,
  admin = false,
}: {
  reserva: Reserva
  alCerrar: () => void
  admin?: boolean
}) {
  const [fecha, setFecha] = useState(fechaEnBogota(reserva.inicio))
  const [elegido, setElegido] = useState<{
    canchaId: number
    nombre: string
    inicio: string
  } | null>(null)
  const disponibilidad = useDisponibilidad(reserva.sede.id, fecha, reserva.cancha.modalidad)
  const previsualizar = usePrevisualizarReprogramacion()
  const reprogramar = useReprogramarReserva()
  const [error, setError] = useState<string | null>(null)
  const [motivo, setMotivo] = useState('')

  const fin = elegido ? sumarMinutos(elegido.inicio, reserva.duracionMin) : null

  useEffect(() => {
    if (!elegido || !fin) return
    setError(null)
    previsualizar.mutate(
      { id: reserva.id, datos: { canchaId: elegido.canchaId, inicio: elegido.inicio, fin }, admin },
      { onError: (e) => setError(mensajeDeError(e)) },
    )
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [elegido])

  const previa = elegido ? previsualizar.data : undefined

  const confirmar = () => {
    if (!elegido || !fin) return
    reprogramar.mutate(
      {
        id: reserva.id,
        datos: { canchaId: elegido.canchaId, inicio: elegido.inicio, fin },
        admin,
        motivo: motivo.trim(),
      },
      {
        onSuccess: () => {
          avisar.exito('Reserva reprogramada.')
          alCerrar()
        },
        onError: (e) => setError(mensajeDeError(e)),
      },
    )
  }

  return (
    <Modal abierto titulo="Reprogramar reserva" alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <p className="text-sm">
          Reserva actual:{' '}
          <strong className="capitalize">{formatearFechaHora(reserva.inicio)}</strong> en{' '}
          {reserva.cancha.nombre}. Duración: {reserva.duracionMin / 60} h. Elige el nuevo horario
          libre.
        </p>
        <Campo
          etiqueta="Nueva fecha"
          type="date"
          min={hoyEnBogota()}
          value={fecha}
          onChange={(e) => e.target.value && (setFecha(e.target.value), setElegido(null))}
        />

        {disponibilidad.isPending && <Cargando texto="Cargando horarios…" className="py-2" />}
        <div className="flex max-h-64 flex-col gap-3 overflow-y-auto">
          {disponibilidad.data?.canchas.map((c) => (
            <div key={c.canchaId}>
              <p className="mb-1 text-sm font-medium">{c.nombre}</p>
              {c.cerrado && <p className="text-texto-suave text-xs">Cerrado.</p>}
              <ul className="grid grid-cols-3 gap-1 sm:grid-cols-4">
                {c.slots.map((s) => {
                  const libre = s.estado === 'DISPONIBLE'
                  const activo = elegido?.canchaId === c.canchaId && elegido.inicio === s.inicio
                  return (
                    <li key={s.inicio}>
                      <button
                        type="button"
                        disabled={!libre}
                        aria-pressed={activo}
                        onClick={() =>
                          setElegido({ canchaId: c.canchaId, nombre: c.nombre, inicio: s.inicio })
                        }
                        className={cn(
                          'rounded-boton w-full border px-1 py-1 text-xs',
                          activo
                            ? 'border-primario bg-primario text-sobre-primario'
                            : libre
                              ? 'border-primario bg-primario-claro text-primario-oscuro'
                              : 'border-borde bg-borde/40 text-texto-suave line-through',
                        )}
                      >
                        {formatearHora(s.inicio)}
                      </button>
                    </li>
                  )
                })}
              </ul>
            </div>
          ))}
        </div>

        {elegido && fin && (
          <div aria-live="polite" className="bg-primario-claro/60 rounded-boton px-4 py-3 text-sm">
            <p>
              Nuevo horario:{' '}
              <strong className="capitalize">{formatearFechaHora(elegido.inicio)}</strong> ·{' '}
              {rangoHorario(elegido.inicio, fin)} en {elegido.nombre}
            </p>
            {previsualizar.isPending && <Cargando texto="Calculando…" className="py-1" />}
            {previa && !previa.disponible && (
              <p className="text-peligro mt-1">Ese horario ya no está disponible.</p>
            )}
            {previa?.disponible && (
              <p className="mt-1">
                Precio: {formatearCOP(previa.precioActual)} →{' '}
                <strong>{formatearCOP(previa.precioNuevo)}</strong>{' '}
                {previa.diferencia !== 0 && (
                  <span className={previa.diferencia > 0 ? 'text-alerta' : 'text-primario-oscuro'}>
                    ({previa.diferencia > 0 ? '+' : '−'}
                    {formatearCOP(Math.abs(previa.diferencia))})
                  </span>
                )}
              </p>
            )}
          </div>
        )}
        {admin && (
          <CampoTexto
            etiqueta="Motivo (obligatorio, se informa al cliente)"
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            error={
              motivo.length > 0 && motivo.trim().length < 5
                ? 'Describe el motivo (mínimo 5 caracteres)'
                : undefined
            }
          />
        )}
        {error && <Alerta tipo="error">{error}</Alerta>}

        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton
            onClick={confirmar}
            disabled={
              !previa?.disponible || reprogramar.isPending || (admin && motivo.trim().length < 5)
            }
          >
            {reprogramar.isPending ? 'Reprogramando…' : 'Confirmar cambio'}
          </Boton>
        </div>
      </div>
    </Modal>
  )
}
