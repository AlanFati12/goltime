import { Plus, Trash2 } from 'lucide-react'
import { useState } from 'react'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { Modal } from '@/componentes/ui/Modal'
import { mensajeDeError } from '@/lib/api'

import { useAccionesSolicitud } from './consultas'

interface FilaJugador {
  /** Clave estable para React: las filas se pueden quitar de cualquier posición. */
  clave: number
  nombres: string
  apellidos: string
  documento: string
  posicion: string
  dorsal: string
}

let contadorFilas = 0
const filaVacia = (): FilaJugador => ({
  clave: ++contadorFilas,
  nombres: '',
  apellidos: '',
  documento: '',
  posicion: '',
  dorsal: '',
})

/** Solicitud de inscripción de un equipo nuevo con su plantilla; queda pendiente hasta que el administrador la apruebe. */
export function ModalSolicitarInscripcion({
  torneoId,
  minimoJugadores,
  alCerrar,
}: {
  torneoId: number
  minimoJugadores: number
  alCerrar: () => void
}) {
  const { solicitar } = useAccionesSolicitud()
  const [equipo, setEquipo] = useState('')
  const [color, setColor] = useState('#059669')
  const [telefono, setTelefono] = useState('')
  const [jugadores, setJugadores] = useState<FilaJugador[]>(() =>
    Array.from({ length: minimoJugadores }, filaVacia),
  )

  const completos = jugadores.filter((j) => j.nombres.trim() && j.apellidos.trim())
  const incompletos = jugadores.filter(
    (j) => (j.nombres.trim() || j.apellidos.trim()) && !(j.nombres.trim() && j.apellidos.trim()),
  ).length
  const dorsales = completos.map((j) => j.dorsal).filter(Boolean)
  const dorsalRepetido = new Set(dorsales).size !== dorsales.length
  const valido =
    equipo.trim().length >= 2 &&
    completos.length >= minimoJugadores &&
    incompletos === 0 &&
    !dorsalRepetido

  const cambiar = (clave: number, campo: keyof Omit<FilaJugador, 'clave'>, valor: string) =>
    setJugadores((lista) => lista.map((j) => (j.clave === clave ? { ...j, [campo]: valor } : j)))
  const quitar = (clave: number) => setJugadores((lista) => lista.filter((j) => j.clave !== clave))

  const enviar = () =>
    solicitar.mutate(
      {
        torneoId,
        datos: {
          nombreEquipo: equipo.trim(),
          color,
          delegadoTelefono: telefono.trim() || undefined,
          jugadores: completos.map((j) => ({
            nombres: j.nombres.trim(),
            apellidos: j.apellidos.trim(),
            documento: j.documento.trim() || undefined,
            posicion: j.posicion.trim() || undefined,
            dorsal: j.dorsal ? Number(j.dorsal) : undefined,
          })),
        },
      },
      {
        onSuccess: () => {
          avisar.exito('Solicitud enviada. El administrador la revisará pronto.')
          alCerrar()
        },
        onError: (e) => avisar.error(mensajeDeError(e)),
      },
    )

  return (
    <Modal abierto ancho="lg" titulo="Solicitar inscripción" alCerrar={alCerrar}>
      <div className="flex flex-col gap-5">
        <div className="grid gap-4 sm:grid-cols-[1fr_6rem_12rem]">
          <Campo
            etiqueta="Nombre del equipo"
            value={equipo}
            onChange={(e) => setEquipo(e.target.value)}
            maxLength={100}
          />
          <Campo
            etiqueta="Color"
            type="color"
            className="[&_input]:p-1"
            value={color}
            onChange={(e) => setColor(e.target.value)}
          />
          <Campo
            etiqueta="Teléfono (opcional)"
            type="tel"
            value={telefono}
            onChange={(e) => setTelefono(e.target.value)}
          />
        </div>

        <fieldset className="flex flex-col gap-3">
          <legend className="mb-1 flex w-full items-baseline justify-between gap-2">
            <span className="text-sm font-semibold">Jugadores</span>
            <span
              className={
                completos.length >= minimoJugadores
                  ? 'text-primario text-xs font-medium'
                  : 'text-texto-suave text-xs'
              }
            >
              {completos.length} de mínimo {minimoJugadores}
            </span>
          </legend>
          <ol className="flex flex-col gap-2">
            {jugadores.map((j, i) => (
              <li
                key={j.clave}
                className="border-borde rounded-boton grid grid-cols-[1.5rem_1fr_auto] items-start gap-2 border p-3"
              >
                <span className="text-texto-suave pt-8 text-xs font-semibold">{i + 1}</span>
                <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-[1fr_1fr_7rem_7rem_4.5rem]">
                  <Campo
                    etiqueta="Nombres"
                    value={j.nombres}
                    onChange={(e) => cambiar(j.clave, 'nombres', e.target.value)}
                  />
                  <Campo
                    etiqueta="Apellidos"
                    value={j.apellidos}
                    onChange={(e) => cambiar(j.clave, 'apellidos', e.target.value)}
                  />
                  <Campo
                    etiqueta="Documento"
                    value={j.documento}
                    onChange={(e) => cambiar(j.clave, 'documento', e.target.value)}
                  />
                  <Campo
                    etiqueta="Posición"
                    value={j.posicion}
                    onChange={(e) => cambiar(j.clave, 'posicion', e.target.value)}
                  />
                  <Campo
                    etiqueta="Dorsal"
                    inputMode="numeric"
                    value={j.dorsal}
                    onChange={(e) =>
                      cambiar(j.clave, 'dorsal', e.target.value.replace(/\D/g, '').slice(0, 2))
                    }
                  />
                </div>
                <Boton
                  tamano="icono"
                  variante="fantasma"
                  className="hover:text-peligro mt-6"
                  aria-label={`Quitar jugador ${i + 1}`}
                  disabled={jugadores.length <= 1}
                  onClick={() => quitar(j.clave)}
                >
                  <Trash2 aria-hidden />
                </Boton>
              </li>
            ))}
          </ol>
          <Boton
            variante="secundario"
            tamano="sm"
            className="self-start"
            onClick={() => setJugadores((lista) => [...lista, filaVacia()])}
          >
            <Plus aria-hidden />
            Agregar jugador
          </Boton>
          {incompletos > 0 && (
            <Alerta tipo="aviso">
              Hay {incompletos} jugador(es) con nombre o apellido sin completar: complétalos o
              quítalos.
            </Alerta>
          )}
          {dorsalRepetido && <Alerta tipo="aviso">Hay dorsales repetidos.</Alerta>}
        </fieldset>

        <div className="border-borde -mx-6 -mb-6 flex justify-end gap-2 border-t px-6 py-4">
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton disabled={!valido || solicitar.isPending} onClick={enviar}>
            {solicitar.isPending ? 'Enviando…' : 'Enviar solicitud'}
          </Boton>
        </div>
      </div>
    </Modal>
  )
}
