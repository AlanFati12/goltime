import { useState } from 'react'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { CampoTexto } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Modal } from '@/componentes/ui/Modal'
import { mensajeDeError } from '@/lib/api'

import type { EstadoReserva, Reserva } from '../tipos'
import { ETIQUETA_ESTADO_RESERVA, efectosDelCambio } from '../utilidades'
import { useCambiarEstadoReserva, usePrevisualizacionEstado } from './consultas'

const ESTADOS = Object.keys(ETIQUETA_ESTADO_RESERVA) as EstadoReserva[]

/** El administrador lleva la reserva a cualquier estado, con motivo y viendo antes el efecto (ADR 0019). */
export function ModalCambiarEstado({
  reserva,
  alCerrar,
}: {
  reserva: Reserva
  alCerrar: () => void
}) {
  const [estado, setEstado] = useState<EstadoReserva | null>(null)
  const [motivo, setMotivo] = useState('')
  const previa = usePrevisualizacionEstado(reserva.id, estado)
  const cambiar = useCambiarEstadoReserva()

  const motivoValido = motivo.trim().length >= 5
  const choca = previa.data?.ocuparaHorario === true && !previa.data.horarioLibre
  const listo = estado !== null && previa.isSuccess && !choca && motivoValido && !cambiar.isPending

  const confirmar = async () => {
    if (estado === null) return
    try {
      await cambiar.mutateAsync({ id: reserva.id, estado, motivo: motivo.trim() })
      avisar.exito(`${reserva.codigo} quedó ${ETIQUETA_ESTADO_RESERVA[estado].toLowerCase()}.`)
      alCerrar()
    } catch (e) {
      avisar.error(mensajeDeError(e))
    }
  }

  return (
    <Modal abierto titulo="Cambiar estado" alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <p className="text-texto-suave text-sm">
          Ahora está <strong>{ETIQUETA_ESTADO_RESERVA[reserva.estado].toLowerCase()}</strong>.
          Puedes llevarla a cualquier estado, aunque todavía no llegue su fecha.
        </p>
        <CampoSelect
          etiqueta="Estado nuevo"
          value={estado ?? ''}
          onChange={(e) =>
            setEstado(e.target.value === '' ? null : (e.target.value as EstadoReserva))
          }
        >
          <option value="">Elige un estado…</option>
          {ESTADOS.filter((e) => e !== reserva.estado).map((e) => (
            <option key={e} value={e}>
              {ETIQUETA_ESTADO_RESERVA[e]}
            </option>
          ))}
        </CampoSelect>

        {estado !== null && previa.isPending && (
          <p className="text-texto-suave text-sm">Revisando el efecto…</p>
        )}
        {previa.isError && <Alerta tipo="error">{mensajeDeError(previa.error)}</Alerta>}
        {previa.data && choca && (
          <Alerta tipo="error">
            El horario ya lo tomó otra reserva o un bloqueo, así que no puede volver a ocupar la
            cancha. Mueve la otra reserva o elige un estado que no ocupe la cancha.
          </Alerta>
        )}
        {previa.data && !choca && (
          <Alerta tipo="aviso">
            <ul className="flex list-disc flex-col gap-1 pl-4">
              {efectosDelCambio(previa.data).map((t) => (
                <li key={t}>{t}</li>
              ))}
            </ul>
          </Alerta>
        )}

        <CampoTexto
          etiqueta="Motivo (obligatorio)"
          value={motivo}
          maxLength={500}
          onChange={(e) => setMotivo(e.target.value)}
          error={
            motivo.length > 0 && !motivoValido
              ? 'Describe el motivo (mínimo 5 caracteres)'
              : undefined
          }
        />
        <div className="flex justify-end gap-2 pt-1">
          <Boton variante="secundario" onClick={alCerrar}>
            Volver
          </Boton>
          <Boton disabled={!listo} onClick={confirmar}>
            {cambiar.isPending
              ? 'Guardando…'
              : estado
                ? `Cambiar a «${ETIQUETA_ESTADO_RESERVA[estado]}»`
                : 'Cambiar estado'}
          </Boton>
        </div>
      </div>
    </Modal>
  )
}
