import { useEffect, useState } from 'react'

import { Alerta } from '@/componentes/ui/Alerta'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Boton } from '@/componentes/ui/Boton'
import { CampoTexto, Casilla } from '@/componentes/ui/Campo'
import { Cargando } from '@/componentes/ui/Cargando'
import { Modal } from '@/componentes/ui/Modal'
import { mensajeDeError } from '@/lib/api'

import type { PrevisualizacionEstado } from './tipos'

interface PropsModal {
  abierto: boolean
  titulo: string
  descripcion: string
  /** Si es false (ej. al reactivar) no se pide motivo ni se previsualiza. */
  requiereMotivo: boolean
  textoConfirmar: string
  tono?: 'peligro' | 'primario'
  previsualizar: () => Promise<PrevisualizacionEstado>
  confirmar: (motivo: string, confirmarConflictos: boolean) => Promise<void>
  alCerrar: () => void
}

/**
 * Cambio de estado de una sede o cancha con el patrón de conflictos: se previsualiza cuántas reservas
 * futuras se verían afectadas y, si hay, se exige confirmación explícita antes de continuar.
 */
export function ModalCambioEstado({
  abierto,
  titulo,
  descripcion,
  requiereMotivo,
  textoConfirmar,
  tono = 'peligro',
  previsualizar,
  confirmar,
  alCerrar,
}: PropsModal) {
  const [motivo, setMotivo] = useState('')
  const [confirmado, setConfirmado] = useState(false)
  const [previa, setPrevia] = useState<PrevisualizacionEstado | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    if (!abierto) return
    setMotivo('')
    setConfirmado(false)
    setPrevia(null)
    setError(null)
    if (!requiereMotivo) {
      setPrevia({ reservasFuturasActivas: 0, requiereConfirmacion: false })
      return
    }
    let vigente = true
    previsualizar()
      .then((p) => vigente && setPrevia(p))
      .catch((e) => vigente && setError(mensajeDeError(e)))
    return () => {
      vigente = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [abierto])

  const conflictos = previa?.reservasFuturasActivas ?? 0
  const motivoValido = !requiereMotivo || motivo.trim().length >= 5
  const bloqueado = !previa || !motivoValido || (conflictos > 0 && !confirmado) || enviando

  const alEnviar = async () => {
    setEnviando(true)
    try {
      await confirmar(motivo.trim(), confirmado)
      alCerrar()
    } catch (e) {
      avisar.error(mensajeDeError(e))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal abierto={abierto} titulo={titulo} alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <p className="text-sm">{descripcion}</p>
        {!previa && !error && <Cargando texto="Revisando reservas afectadas…" />}
        {error && <Alerta tipo="error">{error}</Alerta>}
        {conflictos > 0 && (
          <Alerta tipo="aviso">
            Hay <strong>{conflictos} reservas futuras</strong> afectadas. Si continúas serán
            canceladas y se notificará a los clientes.
            <div className="mt-2">
              <Casilla
                etiqueta="Entiendo y confirmo que se cancelarán."
                checked={confirmado}
                onChange={(e) => setConfirmado(e.target.checked)}
              />
            </div>
          </Alerta>
        )}
        {requiereMotivo && (
          <CampoTexto
            etiqueta="Motivo (obligatorio)"
            value={motivo}
            onChange={(e) => setMotivo(e.target.value)}
            error={
              motivo.length > 0 && !motivoValido
                ? 'Describe el motivo (mínimo 5 caracteres)'
                : undefined
            }
          />
        )}
        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton variante={tono} disabled={bloqueado} onClick={alEnviar}>
            {enviando ? 'Guardando…' : textoConfirmar}
          </Boton>
        </div>
      </div>
    </Modal>
  )
}
