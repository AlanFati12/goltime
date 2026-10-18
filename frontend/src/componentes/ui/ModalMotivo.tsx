import { useState } from 'react'

import { Alerta } from './Alerta'
import { Boton } from './Boton'
import { CampoTexto } from './Campo'
import { Modal } from './Modal'

interface PropsModalMotivo {
  titulo: string
  descripcion: string
  etiquetaMotivo?: string
  textoConfirmar: string
  tono?: 'peligro' | 'primario'
  /** Si es false el motivo es opcional (ej. cuando un cliente cancela). */
  motivoObligatorio?: boolean
  advertencia?: string
  alConfirmar: (motivo: string) => Promise<void>
  alCerrar: () => void
}

/** Diálogo que pide un motivo (obligatorio u opcional) antes de una acción importante. */
export function ModalMotivo({
  titulo,
  descripcion,
  etiquetaMotivo = 'Motivo',
  textoConfirmar,
  tono = 'peligro',
  motivoObligatorio = true,
  advertencia,
  alConfirmar,
  alCerrar,
}: PropsModalMotivo) {
  const [motivo, setMotivo] = useState('')
  const [enviando, setEnviando] = useState(false)
  const valido = !motivoObligatorio || motivo.trim().length >= 5

  const enviar = async () => {
    setEnviando(true)
    try {
      await alConfirmar(motivo.trim())
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal abierto titulo={titulo} alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <p className="text-texto-suave text-sm">{descripcion}</p>
        {advertencia && <Alerta tipo="aviso">{advertencia}</Alerta>}
        <CampoTexto
          etiqueta={
            motivoObligatorio ? `${etiquetaMotivo} (obligatorio)` : `${etiquetaMotivo} (opcional)`
          }
          value={motivo}
          onChange={(e) => setMotivo(e.target.value)}
          error={
            motivoObligatorio && motivo.length > 0 && !valido
              ? 'Describe el motivo (mínimo 5 caracteres)'
              : undefined
          }
        />
        <div className="flex justify-end gap-2 pt-1">
          <Boton variante="secundario" onClick={alCerrar}>
            Volver
          </Boton>
          <Boton variante={tono} disabled={!valido || enviando} onClick={enviar}>
            {enviando ? 'Guardando…' : textoConfirmar}
          </Boton>
        </div>
      </div>
    </Modal>
  )
}
