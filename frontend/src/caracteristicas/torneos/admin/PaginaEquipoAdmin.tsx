import { useState } from 'react'
import { useParams } from 'react-router-dom'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta, TituloTarjeta } from '@/componentes/ui/Tarjeta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { mensajeDeError } from '@/lib/api'

import { useAccionesEquipo, useBuscarJugadores, useEquipo } from '../consultas'
import type { Equipo } from '../tipos'
import { ListaJugadores } from './Jugadores'

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
        <h2 className="text-xl font-extrabold uppercase">Datos del equipo</h2>
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
          ayuda="Lo ves en Clientes. Recibirá avisos de inscripción, programación y resultados."
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

/** Busca un jugador que ya existe (de otro equipo o torneo) para sumarlo sin duplicarlo. */
function JugadorExistente({ equipoId }: { equipoId: number }) {
  const { agregarJugador } = useAccionesEquipo()
  const [texto, setTexto] = useState('')
  const encontrados = useBuscarJugadores(texto)

  return (
    <Tarjeta className="flex flex-col gap-3">
      <TituloTarjeta
        className="mb-0"
        titulo="Sumar un jugador que ya existe"
        descripcion="Búscalo por nombre o documento para reutilizarlo sin duplicarlo."
      />
      <Campo
        etiqueta="Buscar jugador"
        type="search"
        value={texto}
        onChange={(e) => setTexto(e.target.value)}
      />
      <ul className="divide-borde/70 flex flex-col divide-y text-sm">
        {encontrados.data?.map((j) => (
          <li key={j.id} className="flex items-center justify-between gap-2 py-2">
            <span>
              {j.nombreCompleto}
              {j.documento && <span className="text-texto-suave"> · {j.documento}</span>}
            </span>
            <Boton
              tamano="sm"
              variante="secundario"
              disabled={agregarJugador.isPending}
              onClick={() =>
                agregarJugador.mutate(
                  { id: equipoId, datos: { jugadorId: j.id } },
                  {
                    onSuccess: () => {
                      avisar.exito('Jugador agregado.')
                      setTexto('')
                    },
                    onError: (e) => avisar.error(mensajeDeError(e)),
                  },
                )
              }
            >
              Agregar
            </Boton>
          </li>
        ))}
        {encontrados.data?.length === 0 && (
          <li className="text-texto-suave py-2">Sin coincidencias.</li>
        )}
      </ul>
    </Tarjeta>
  )
}

export function PaginaEquipoAdmin() {
  const id = Number(useParams().id)
  const { data, isPending, isError, error } = useEquipo(id)
  const { quitarJugador, editarJugador, agregarJugador } = useAccionesEquipo()

  return (
    <div className="flex flex-col gap-6">
      {isPending && <Cargando texto="Cargando equipo…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && (
        <>
          <EncabezadoPagina
            titulo={data.equipo.nombre}
            volver={{ a: '/admin/equipos', texto: 'Equipos' }}
          />
          <DatosEquipo key={data.equipo.id + data.equipo.nombre} equipo={data.equipo} />
          <Tarjeta className="flex flex-col gap-3">
            <TituloTarjeta
              className="mb-0"
              titulo={`Plantilla habitual (${data.plantilla.length})`}
              descripcion="Al inscribir el equipo en un torneo se copia esta plantilla. Para cambiar la de un torneo ya inscrito, hazlo desde el torneo."
            />
            <ListaJugadores
              jugadores={data.plantilla}
              ocupado={editarJugador.isPending || quitarJugador.isPending}
              alEditar={(jugadorId, datos) => editarJugador.mutateAsync({ id, jugadorId, datos })}
              alQuitar={(jugadorId) => quitarJugador.mutateAsync({ id, jugadorId })}
              alAgregar={(d) =>
                agregarJugador
                  .mutateAsync({
                    id,
                    datos: {
                      nombres: d.nombres.trim(),
                      apellidos: d.apellidos.trim(),
                      documento: d.documento.trim() || undefined,
                      posicion: d.posicion.trim() || undefined,
                      dorsal: d.dorsal,
                    },
                  })
                  .then(() => avisar.exito('Jugador agregado.'))
              }
            />
          </Tarjeta>
          <JugadorExistente equipoId={id} />
        </>
      )}
    </div>
  )
}
