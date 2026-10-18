import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { nombreCompleto, type Usuario } from '@/caracteristicas/autenticacion/almacenSesion'
import { Alerta } from '@/componentes/ui/Alerta'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Boton } from '@/componentes/ui/Boton'
import { Casilla, CampoTexto } from '@/componentes/ui/Campo'
import { Cargando } from '@/componentes/ui/Cargando'
import { Modal } from '@/componentes/ui/Modal'
import { mensajeDeError } from '@/lib/api'
import { esquemaMotivo } from '@/lib/validaciones'

import { useAccionesCliente, usePrevisualizarDesactivacion } from './consultasAdmin'

/**
 * Desactivar un cliente sigue el patrón de conflictos: primero se previsualiza cuántas reservas
 * futuras se verían afectadas y, si hay, se exige una confirmación explícita antes de continuar.
 */
export function ModalDesactivar({
  cliente,
  alCerrar,
}: {
  cliente: Usuario | null
  alCerrar: () => void
}) {
  const {
    data: previsualizacion,
    isPending,
    isError,
    error,
  } = usePrevisualizarDesactivacion(cliente?.id ?? null)
  const { desactivar } = useAccionesCliente()
  const [confirmado, setConfirmado] = useState(false)
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<{ motivo: string }>({ resolver: zodResolver(esquemaMotivo) })

  const cerrar = () => {
    reset()
    setConfirmado(false)
    alCerrar()
  }

  const conflictos = previsualizacion?.reservasFuturasActivas ?? 0
  const bloqueado = conflictos > 0 && !confirmado

  const alEnviar = handleSubmit(async ({ motivo }) => {
    if (!cliente) return
    try {
      await desactivar.mutateAsync({ id: cliente.id, motivo, confirmarConflictos: confirmado })
      avisar.exito(`${nombreCompleto(cliente)} fue desactivado.`)
      cerrar()
    } catch (e) {
      avisar.error(mensajeDeError(e))
    }
  })

  return (
    <Modal abierto={cliente !== null} titulo="Desactivar cliente" alCerrar={cerrar}>
      {cliente && (
        <form onSubmit={alEnviar} noValidate className="flex flex-col gap-4">
          <p className="text-sm">
            <strong>{nombreCompleto(cliente)}</strong> ({cliente.correo}) no podrá iniciar sesión y
            sus sesiones abiertas se cerrarán de inmediato.
          </p>
          {isPending && <Cargando texto="Revisando reservas del cliente…" />}
          {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
          {conflictos > 0 && (
            <Alerta tipo="aviso">
              Este cliente tiene <strong>{conflictos} reservas futuras</strong>. Si continúas, serán
              canceladas y se notificará al cliente.
              <div className="mt-2">
                <Casilla
                  etiqueta="Entiendo y confirmo que se cancelarán."
                  checked={confirmado}
                  onChange={(e) => setConfirmado(e.target.checked)}
                />
              </div>
            </Alerta>
          )}
          <CampoTexto
            etiqueta="Motivo (obligatorio)"
            error={errors.motivo?.message}
            {...register('motivo')}
          />
          <div className="flex justify-end gap-2">
            <Boton variante="secundario" onClick={cerrar}>
              Cancelar
            </Boton>
            <Boton
              type="submit"
              variante="peligro"
              disabled={isPending || isError || bloqueado || desactivar.isPending}
            >
              {desactivar.isPending ? 'Desactivando…' : 'Desactivar'}
            </Boton>
          </div>
        </form>
      )}
    </Modal>
  )
}
