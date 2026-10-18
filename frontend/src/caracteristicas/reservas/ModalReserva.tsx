import { useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, CampoTexto } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Modal } from '@/componentes/ui/Modal'
import { codigoDeError, mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearFechaHora } from '@/lib/formato'
import { useCotizacion } from '@/caracteristicas/sedes/consultas'

import { useSolicitarReserva } from './consultas'
import type { DisponibilidadCancha, Slot } from './tipos'
import { duracionesPosibles, rangoHorario, sumarMinutos, textoDuracion } from './utilidades'

interface PropsModal {
  cancha: DisponibilidadCancha
  slot: Slot
  alCerrar: () => void
}

/** Resumen de la solicitud: duración, precio calculado por el servidor, equipo y notas. */
export function ModalReserva({ cancha, slot, alCerrar }: PropsModal) {
  const navegar = useNavigate()
  const solicitar = useSolicitarReserva()
  const duraciones = useMemo(() => duracionesPosibles(cancha, slot), [cancha, slot])
  const [minutos, setMinutos] = useState(duraciones[0] ?? cancha.duracionMinMin)
  const [equipo, setEquipo] = useState('')
  const [notas, setNotas] = useState('')
  const [error, setError] = useState<string | null>(null)
  // Una clave por apertura del diálogo: si el usuario pulsa dos veces, el servidor devuelve la misma reserva.
  const clave = useRef(crypto.randomUUID())

  const fin = sumarMinutos(slot.inicio, minutos)
  const {
    data: cotizacion,
    isFetching,
    isError: falloCotizacion,
    error: errorCotizacion,
  } = useCotizacion(cancha.canchaId, slot.inicio, fin)

  const enviar = () => {
    setError(null)
    solicitar.mutate(
      {
        datos: {
          canchaId: cancha.canchaId,
          inicio: slot.inicio,
          fin,
          nombreEquipo: equipo || undefined,
          notas: notas || undefined,
        },
        clave: clave.current,
      },
      {
        onSuccess: (reserva) => {
          avisar.exito('Solicitud enviada. El administrador la confirmará pronto.')
          navegar(`/mis-reservas/${reserva.id}`)
        },
        onError: (e) => {
          setError(mensajeDeError(e))
          if (codigoDeError(e) === 'RESERVA_SOLAPADA')
            avisar.error('Ese horario acaba de ser tomado. Elige otro.')
        },
      },
    )
  }

  return (
    <Modal abierto titulo="Solicitar reserva" alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <div className="bg-primario-claro rounded-boton p-3 text-sm">
          <p className="font-semibold">
            {cancha.sedeNombre} · {cancha.nombre} ({cancha.modalidad})
          </p>
          <p>
            {formatearFechaHora(slot.inicio)} · {rangoHorario(slot.inicio, fin)}
          </p>
        </div>

        <CampoSelect
          etiqueta="Duración"
          value={minutos}
          onChange={(e) => setMinutos(Number(e.target.value))}
        >
          {duraciones.map((m) => (
            <option key={m} value={m}>
              {textoDuracion(m)}
            </option>
          ))}
        </CampoSelect>

        <div aria-live="polite" className="text-sm">
          {isFetching && <Cargando texto="Calculando precio…" className="py-1" />}
          {falloCotizacion && <Alerta tipo="error">{mensajeDeError(errorCotizacion)}</Alerta>}
          {cotizacion && !isFetching && (
            <div>
              <p className="text-primario-oscuro text-2xl font-bold">
                {formatearCOP(cotizacion.total)}
              </p>
              {cotizacion.tramos.length > 1 && (
                <p className="text-texto-suave text-xs">
                  Incluye {cotizacion.tramos.length} franjas de precio.
                </p>
              )}
            </div>
          )}
        </div>

        <Campo
          etiqueta="Nombre del equipo (opcional)"
          value={equipo}
          onChange={(e) => setEquipo(e.target.value)}
          maxLength={100}
        />
        <CampoTexto
          etiqueta="Notas para el administrador (opcional)"
          value={notas}
          onChange={(e) => setNotas(e.target.value)}
          maxLength={500}
        />

        <Alerta tipo="info">
          Tu reserva quedará <strong>pendiente</strong> hasta que el administrador la apruebe. Si no
          responde a tiempo, el horario se libera.
        </Alerta>
        {error && <Alerta tipo="error">{error}</Alerta>}

        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton
            onClick={enviar}
            disabled={solicitar.isPending || isFetching || !cotizacion || duraciones.length === 0}
          >
            {solicitar.isPending ? 'Enviando…' : 'Solicitar reserva'}
          </Boton>
        </div>
      </div>
    </Modal>
  )
}
