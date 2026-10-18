import { useState } from 'react'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { Modal } from '@/componentes/ui/Modal'
import { mensajeDeError } from '@/lib/api'

import { useAccionesSolicitud } from './consultas'

interface FilaJugador {
  nombres: string
  apellidos: string
  documento: string
}

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
  const [color, setColor] = useState('#16a34a')
  const [telefono, setTelefono] = useState('')
  const [jugadores, setJugadores] = useState<FilaJugador[]>(() =>
    Array.from({ length: minimoJugadores }, () => ({ nombres: '', apellidos: '', documento: '' })),
  )
  const completos = jugadores.filter((j) => j.nombres.trim() && j.apellidos.trim())
  const valido = equipo.trim().length >= 2 && completos.length >= minimoJugadores

  const cambiar = (i: number, campo: keyof FilaJugador, valor: string) =>
    setJugadores(jugadores.map((j, n) => (n === i ? { ...j, [campo]: valor } : j)))

  const enviar = () =>
    solicitar.mutate(
      {
        torneoId,
        datos: {
          nombreEquipo: equipo.trim(),
          color,
          delegadoTelefono: telefono.trim() || undefined,
          jugadores: completos.map((j, i) => ({
            nombres: j.nombres.trim(),
            apellidos: j.apellidos.trim(),
            documento: j.documento.trim() || undefined,
            dorsal: i + 1,
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
    <Modal abierto titulo="Solicitar inscripción" alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <Campo
          etiqueta="Nombre del equipo"
          value={equipo}
          onChange={(e) => setEquipo(e.target.value)}
          maxLength={100}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo
            etiqueta="Color"
            type="color"
            value={color}
            onChange={(e) => setColor(e.target.value)}
          />
          <Campo
            etiqueta="Teléfono de contacto (opcional)"
            value={telefono}
            onChange={(e) => setTelefono(e.target.value)}
          />
        </div>
        <fieldset className="flex flex-col gap-2">
          <legend className="text-sm font-medium">Jugadores (mínimo {minimoJugadores})</legend>
          {jugadores.map((j, i) => (
            <div key={i} className="grid grid-cols-[1fr_1fr_7rem] gap-2">
              <Campo
                etiqueta={`Nombres ${i + 1}`}
                value={j.nombres}
                onChange={(e) => cambiar(i, 'nombres', e.target.value)}
              />
              <Campo
                etiqueta={`Apellidos ${i + 1}`}
                value={j.apellidos}
                onChange={(e) => cambiar(i, 'apellidos', e.target.value)}
              />
              <Campo
                etiqueta="Documento"
                value={j.documento}
                onChange={(e) => cambiar(i, 'documento', e.target.value)}
              />
            </div>
          ))}
          <Boton
            variante="fantasma"
            tamano="sm"
            className="self-start"
            onClick={() =>
              setJugadores([...jugadores, { nombres: '', apellidos: '', documento: '' }])
            }
          >
            + Agregar jugador
          </Boton>
        </fieldset>
        <div className="flex justify-end gap-2">
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
