import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo } from '@/componentes/ui/Campo'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { Modal } from '@/componentes/ui/Modal'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'

import { useAccionesEquipo, useEquipos } from '../consultas'

function ModalNuevoEquipo({ alCerrar }: { alCerrar: () => void }) {
  const navegar = useNavigate()
  const { crear } = useAccionesEquipo()
  const [nombre, setNombre] = useState('')
  const [color, setColor] = useState('#16a34a')
  const [delegado, setDelegado] = useState('')
  const [telefono, setTelefono] = useState('')

  const guardar = () =>
    crear.mutate(
      {
        nombre: nombre.trim(),
        color,
        delegadoNombre: delegado.trim() || undefined,
        delegadoTelefono: telefono.trim() || undefined,
      },
      {
        onSuccess: (d) => {
          avisar.exito('Equipo creado. Agrega sus jugadores.')
          navegar(`/admin/equipos/${d.equipo.id}`)
        },
        onError: (e) => avisar.error(mensajeDeError(e)),
      },
    )

  return (
    <Modal abierto titulo="Nuevo equipo" alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <Campo
          etiqueta="Nombre del equipo"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          maxLength={100}
        />
        <Campo
          etiqueta="Color representativo"
          type="color"
          value={color}
          onChange={(e) => setColor(e.target.value)}
        />
        <Campo
          etiqueta="Delegado (opcional)"
          value={delegado}
          onChange={(e) => setDelegado(e.target.value)}
          maxLength={150}
        />
        <Campo
          etiqueta="Teléfono del delegado (opcional)"
          value={telefono}
          onChange={(e) => setTelefono(e.target.value)}
          maxLength={30}
        />
        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton disabled={nombre.trim().length < 2 || crear.isPending} onClick={guardar}>
            {crear.isPending ? 'Creando…' : 'Crear equipo'}
          </Boton>
        </div>
      </div>
    </Modal>
  )
}

export function PaginaEquiposAdmin() {
  const [busqueda, setBusqueda] = useState('')
  const [inactivos, setInactivos] = useState(false)
  const [nuevo, setNuevo] = useState(false)
  const { data, isPending, isError, error } = useEquipos(busqueda, inactivos)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-bold">Equipos</h1>
        <Boton onClick={() => setNuevo(true)}>Nuevo equipo</Boton>
      </div>
      <p className="text-texto-suave max-w-3xl text-sm">
        Los equipos y sus jugadores se crean una vez y se reutilizan en todos los torneos.
      </p>
      <Tarjeta className="flex flex-wrap items-end gap-4">
        <Campo
          className="min-w-56"
          etiqueta="Buscar por nombre"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
        />
        <label className="flex items-center gap-2 pb-2 text-sm">
          <input
            type="checkbox"
            className="accent-primario size-4"
            checked={inactivos}
            onChange={(e) => setInactivos(e.target.checked)}
          />
          Mostrar desactivados
        </label>
      </Tarjeta>
      {isPending && <Cargando texto="Cargando equipos…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && (
        <Tarjeta className="text-texto-suave text-center text-sm">No hay equipos.</Tarjeta>
      )}
      <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {data?.map((r) => (
          <li key={r.equipo.id}>
            <Tarjeta className="flex flex-col gap-2 p-4">
              <div className="flex items-center gap-2">
                <span
                  className="size-4 rounded-full border"
                  style={{ backgroundColor: r.equipo.color ?? 'transparent' }}
                  aria-hidden="true"
                />
                <Link
                  to={`/admin/equipos/${r.equipo.id}`}
                  className="font-semibold hover:underline"
                >
                  {r.equipo.nombre}
                </Link>
                {!r.equipo.activo && <Insignia tono="rojo">Desactivado</Insignia>}
              </div>
              <p className="text-texto-suave text-sm">
                {r.jugadores} jugadores · {r.torneos} torneos
              </p>
            </Tarjeta>
          </li>
        ))}
      </ul>
      {nuevo && <ModalNuevoEquipo alCerrar={() => setNuevo(false)} />}
    </div>
  )
}
