import { useState } from 'react'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, Casilla } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Modal } from '@/componentes/ui/Modal'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP } from '@/lib/formato'

import type { MetodoPago, Reserva } from '../tipos'
import { useAccionesPago } from './consultas'

const METODOS: { valor: MetodoPago; texto: string }[] = [
  { valor: 'EFECTIVO', texto: 'Efectivo' },
  { valor: 'TRANSFERENCIA', texto: 'Transferencia' },
  { valor: 'NEQUI', texto: 'Nequi' },
  { valor: 'DAVIPLATA', texto: 'Daviplata' },
  { valor: 'TARJETA', texto: 'Tarjeta (en sitio)' },
]

/** Registrar un pago o abono: el monto sugerido es el saldo pendiente y no puede pasarse de él. */
export function ModalPago({ reserva, alCerrar }: { reserva: Reserva; alCerrar: () => void }) {
  const saldo = Math.max(0, reserva.precioFinal - reserva.montoPagado)
  const { pagar } = useAccionesPago()
  const [monto, setMonto] = useState(String(saldo))
  const [metodo, setMetodo] = useState<MetodoPago>('EFECTIVO')
  const [referencia, setReferencia] = useState('')
  const [error, setError] = useState<string | null>(null)

  const valor = Number(monto)
  const valido = Number.isInteger(valor) && valor > 0 && valor <= saldo

  const enviar = () =>
    pagar.mutate(
      { id: reserva.id, monto: valor, metodo, referencia },
      {
        onSuccess: (r) => {
          avisar.exito(
            r.estadoPago === 'PAGADO'
              ? 'Pago registrado: la reserva quedó pagada.'
              : 'Abono registrado.',
          )
          alCerrar()
        },
        onError: (e) => setError(mensajeDeError(e)),
      },
    )

  return (
    <Modal abierto titulo="Registrar pago" alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <p className="text-sm">
          Valor {formatearCOP(reserva.precioFinal)} · pagado {formatearCOP(reserva.montoPagado)} ·
          saldo <strong>{formatearCOP(saldo)}</strong>
        </p>
        <Campo
          etiqueta="Monto (COP)"
          type="number"
          inputMode="numeric"
          min={1}
          max={saldo}
          value={monto}
          onChange={(e) => setMonto(e.target.value)}
          error={
            monto !== '' && !valido ? `Debe ser un valor entero entre 1 y ${saldo}` : undefined
          }
          ayuda="Si es menor al saldo queda como abono."
        />
        <CampoSelect
          etiqueta="Método de pago"
          value={metodo}
          onChange={(e) => setMetodo(e.target.value as MetodoPago)}
        >
          {METODOS.map((m) => (
            <option key={m.valor} value={m.valor}>
              {m.texto}
            </option>
          ))}
        </CampoSelect>
        <Campo
          etiqueta="Referencia (opcional)"
          value={referencia}
          onChange={(e) => setReferencia(e.target.value)}
          maxLength={100}
          ayuda="Número de transferencia, comprobante, etc."
        />
        {error && <Alerta tipo="error">{error}</Alerta>}
        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton onClick={enviar} disabled={!valido || pagar.isPending}>
            {pagar.isPending ? 'Registrando…' : 'Registrar'}
          </Boton>
        </div>
      </div>
    </Modal>
  )
}

/** Registrar una devolución: solo existe cuando hay un reembolso pendiente. */
export function ModalReembolso({ reserva, alCerrar }: { reserva: Reserva; alCerrar: () => void }) {
  const { reembolsar } = useAccionesPago()
  const [monto, setMonto] = useState(String(reserva.montoPagado))
  const [metodo, setMetodo] = useState<MetodoPago>('EFECTIVO')
  const [referencia, setReferencia] = useState('')
  const [cerrar, setCerrar] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const valor = Number(monto)
  const valido = Number.isInteger(valor) && valor > 0 && valor <= reserva.montoPagado
  const retiene = reserva.montoPagado - valor

  const enviar = () =>
    reembolsar.mutate(
      { id: reserva.id, monto: valor, metodo, referencia, cerrar },
      {
        onSuccess: (r) => {
          avisar.exito(
            r.estadoPago === 'REEMBOLSADO'
              ? 'Reembolso registrado y saldado.'
              : 'Reembolso parcial registrado.',
          )
          alCerrar()
        },
        onError: (e) => setError(mensajeDeError(e)),
      },
    )

  return (
    <Modal abierto titulo="Registrar reembolso" alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <p className="text-sm">
          La reserva se canceló con <strong>{formatearCOP(reserva.montoPagado)}</strong> pagados.
          Registra cuánto se devuelve al cliente.
        </p>
        <Campo
          etiqueta="Monto a devolver (COP)"
          type="number"
          inputMode="numeric"
          min={1}
          max={reserva.montoPagado}
          value={monto}
          onChange={(e) => setMonto(e.target.value)}
          error={
            monto !== '' && !valido
              ? `Debe ser un valor entero entre 1 y ${reserva.montoPagado}`
              : undefined
          }
        />
        <CampoSelect
          etiqueta="Método de devolución"
          value={metodo}
          onChange={(e) => setMetodo(e.target.value as MetodoPago)}
        >
          {METODOS.map((m) => (
            <option key={m.valor} value={m.valor}>
              {m.texto}
            </option>
          ))}
        </CampoSelect>
        <Campo
          etiqueta="Referencia (opcional)"
          value={referencia}
          onChange={(e) => setReferencia(e.target.value)}
          maxLength={100}
        />
        {valido && retiene > 0 && (
          <Casilla
            etiqueta={`Dar el reembolso por saldado y retener ${formatearCOP(retiene)} (por ejemplo, penalidad por cancelación tardía)`}
            checked={cerrar}
            onChange={(e) => setCerrar(e.target.checked)}
          />
        )}
        {valido && retiene > 0 && !cerrar && (
          <Alerta tipo="info">Quedarán {formatearCOP(retiene)} pendientes de devolver.</Alerta>
        )}
        {error && <Alerta tipo="error">{error}</Alerta>}
        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton onClick={enviar} disabled={!valido || reembolsar.isPending}>
            {reembolsar.isPending ? 'Registrando…' : 'Registrar reembolso'}
          </Boton>
        </div>
      </div>
    </Modal>
  )
}
