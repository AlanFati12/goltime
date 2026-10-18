import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'

import { useAccionesEquipo, useBuscarJugadores, useEquipo } from '../consultas'
import type { Equipo } from '../tipos'

function DatosEquipo({ equipo }: { equipo: Equipo }) {
  const { actualizar, cambiarActivo } = useAccionesEquipo()
  const [nombre, setNombre] = useState(equipo.nombre)
  const [color, setColor] = useState(equipo.color ?? '#16a34a')
  const [delegado, setDelegado] = useState(equipo.delegadoNombre ?? '')
  const [telefono, setTelefono] = useState(equipo.delegadoTelefono ?? '')
  const [responsable, setResponsable] = useState(equipo.usuarioResponsableId?.toString() ?? '')

  const guardar = () =>
    actualizar.mutate(
      {
        id: equipo.id,
        datos: {
          nombre: nombre.trim(),
          color,
          delegadoNombre: delegado.trim() || undefined,
          delegadoTelefono: telefono.trim() || undefined,
          usuarioResponsableId: responsable.trim() ? Number(responsable) : null,
        },
      },
      {
        onSuccess: () => avisar.exito('Equipo actualizado.'),
        onError: (e) => avisar.error(mensajeDeError(e)),
      },
    )

  return (
    <Tarjeta className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-lg font-semibold">Datos del equipo</h2>
        {!equipo.activo && <Insignia tono="rojo">Desactivado</Insignia>}
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Campo
          etiqueta="Nombre"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          maxLength={100}
        />
        <Campo
          etiqueta="Color"
          type="color"
          value={color}
          onChange={(e) => setColor(e.target.value)}
        />
        <Campo etiqueta="Delegado" value={delegado} onChange={(e) => setDelegado(e.target.value)} />
        <Campo
          etiqueta="Teléfono del delegado"
          value={telefono}
          onChange={(e) => setTelefono(e.target.value)}
        />
        <Campo
          etiqueta="Id del cliente responsable (opcional)"
          ayuda="Recibirá avisos de inscripción, programación y resultados del equipo."
          inputMode="numeric"
          value={responsable}
          onChange={(e) => setResponsable(e.target.value.replace(/\D/g, ''))}
        />
      </div>
      <div className="flex flex-wrap justify-end gap-2">
        <Boton
          variante={equipo.activo ? 'peligro' : 'secundario'}
          onClick={() =>
            cambiarActivo.mutate(
              { id: equipo.id, activo: !equipo.activo },
              { onError: (e) => avisar.error(mensajeDeError(e)) },
            )
          }
        >
          {equipo.activo ? 'Desactivar' : 'Reactivar'}
        </Boton>
        <Boton disabled={nombre.trim().length < 2 || actualizar.isPending} onClick={guardar}>
          Guardar cambios
        </Boton>
      </div>
    </Tarjeta>
  )
}

function AgregarJugador({ equipoId }: { equipoId: number }) {
  const { agregarJugador } = useAccionesEquipo()
  const [nombres, setNombres] = useState('')
  const [apellidos, setApellidos] = useState('')
  const [documento, setDocumento] = useState('')
  const [posicion, setPosicion] = useState('')
  const [dorsal, setDorsal] = useState('')
  const [texto, setTexto] = useState('')
  const encontrados = useBuscarJugadores(texto)

  const alExito = () => {
    avisar.exito('Jugador agregado.')
    setNombres('')
    setApellidos('')
    setDocumento('')
    setPosicion('')
    setDorsal('')
    setTexto('')
  }
  const dorsalNumero = dorsal === '' ? null : Number(dorsal)

  return (
    <Tarjeta className="flex flex-col gap-4">
      <h2 className="text-lg font-semibold">Agregar jugador</h2>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
        <Campo etiqueta="Nombres" value={nombres} onChange={(e) => setNombres(e.target.value)} />
        <Campo
          etiqueta="Apellidos"
          value={apellidos}
          onChange={(e) => setApellidos(e.target.value)}
        />
        <Campo
          etiqueta="Documento"
          value={documento}
          onChange={(e) => setDocumento(e.target.value)}
        />
        <Campo etiqueta="Posición" value={posicion} onChange={(e) => setPosicion(e.target.value)} />
        <Campo
          etiqueta="Dorsal"
          inputMode="numeric"
          value={dorsal}
          onChange={(e) => setDorsal(e.target.value.replace(/\D/g, '').slice(0, 2))}
        />
      </div>
      <div className="flex justify-end">
        <Boton
          disabled={!nombres.trim() || !apellidos.trim() || agregarJugador.isPending}
          onClick={() =>
            agregarJugador.mutate(
              {
                id: equipoId,
                datos: { nombres, apellidos, documento, posicion, dorsal: dorsalNumero },
              },
              { onSuccess: alExito, onError: (e) => avisar.error(mensajeDeError(e)) },
            )
          }
        >
          Crear y agregar
        </Boton>
      </div>
      <div className="border-borde border-t pt-4">
        <Campo
          etiqueta="¿Ya existe? Búscalo por nombre o documento"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          ayuda="Reutiliza jugadores de otros equipos o torneos sin duplicarlos."
        />
        <ul className="mt-2 flex flex-col gap-1 text-sm">
          {encontrados.data?.map((j) => (
            <li key={j.id} className="flex items-center justify-between gap-2">
              <span>
                {j.nombreCompleto}
                {j.documento && <span className="text-texto-suave"> · {j.documento}</span>}
              </span>
              <Boton
                tamano="sm"
                variante="secundario"
                onClick={() =>
                  agregarJugador.mutate(
                    { id: equipoId, datos: { jugadorId: j.id, dorsal: dorsalNumero } },
                    { onSuccess: alExito, onError: (e) => avisar.error(mensajeDeError(e)) },
                  )
                }
              >
                Agregar
              </Boton>
            </li>
          ))}
          {encontrados.data?.length === 0 && (
            <li className="text-texto-suave">Sin coincidencias.</li>
          )}
        </ul>
      </div>
    </Tarjeta>
  )
}

export function PaginaEquipoAdmin() {
  const id = Number(useParams().id)
  const { data, isPending, isError, error } = useEquipo(id)
  const { quitarJugador } = useAccionesEquipo()

  return (
    <div className="flex flex-col gap-4">
      <Link to="/admin/equipos" className="text-primario text-sm hover:underline">
        ← Equipos
      </Link>
      {isPending && <Cargando texto="Cargando equipo…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && (
        <>
          <h1 className="text-2xl font-bold">{data.equipo.nombre}</h1>
          <DatosEquipo key={data.equipo.id + data.equipo.nombre} equipo={data.equipo} />
          <Tarjeta className="flex flex-col gap-3">
            <h2 className="text-lg font-semibold">Plantilla habitual ({data.plantilla.length})</h2>
            <p className="text-texto-suave text-xs">
              Al inscribir el equipo en un torneo se copia esta plantilla; los cambios posteriores
              no afectan torneos ya inscritos.
            </p>
            <ul className="flex flex-col gap-1 text-sm">
              {data.plantilla.map((j) => (
                <li
                  key={j.id}
                  className="border-borde flex items-center justify-between gap-2 border-b py-1"
                >
                  <span>
                    <span className="text-texto-suave inline-block w-8">{j.dorsal ?? ''}</span>
                    <span className="font-medium">{j.nombreCompleto}</span>
                    {j.posicion && <span className="text-texto-suave"> · {j.posicion}</span>}
                    {j.documento && <span className="text-texto-suave"> · {j.documento}</span>}
                  </span>
                  <Boton
                    tamano="sm"
                    variante="fantasma"
                    onClick={() =>
                      quitarJugador.mutate(
                        { id, jugadorId: j.id },
                        { onError: (e) => avisar.error(mensajeDeError(e)) },
                      )
                    }
                  >
                    Quitar
                  </Boton>
                </li>
              ))}
              {data.plantilla.length === 0 && (
                <li className="text-texto-suave">Aún no hay jugadores.</li>
              )}
            </ul>
          </Tarjeta>
          <AgregarJugador equipoId={id} />
        </>
      )}
    </div>
  )
}
