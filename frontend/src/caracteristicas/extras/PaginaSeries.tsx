import { useState } from 'react'

import { useClientes } from '@/caracteristicas/admin/consultasAdmin'
import { useSedesAdmin } from '@/caracteristicas/sedes/consultas'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, Casilla } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { ModalMotivo } from '@/componentes/ui/ModalMotivo'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { mensajeDeError } from '@/lib/api'
import { aIsoBogota, formatearCOP, formatearFechaHora, hoyEnBogota, sumarDias } from '@/lib/formato'
import { cn } from '@/lib/utils'

import { useAccionesSerie, useSeries, type DatosSerie, type OcurrenciaSerie } from './consultas'

const DIAS = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo']
const hora = (m: number) =>
  `${String(Math.floor(m / 60)).padStart(2, '0')}:${String(m % 60).padStart(2, '0')}`
const TONO = { DISPONIBLE: 'verde', OCUPADA: 'rojo', FUERA_DE_HORARIO: 'ambar' } as const
const TEXTO = {
  DISPONIBLE: 'Disponible',
  OCUPADA: 'Ocupada',
  FUERA_DE_HORARIO: 'Fuera de horario',
} as const

function FormularioSerie() {
  const sedes = useSedesAdmin()
  const { previsualizar, crear } = useAccionesSerie()
  const [sedeId, setSedeId] = useState('')
  const [canchaId, setCanchaId] = useState('')
  const [fecha, setFecha] = useState(sumarDias(hoyEnBogota(), 7))
  const [desde, setDesde] = useState('19:00')
  const [duracion, setDuracion] = useState('60')
  const [semanas, setSemanas] = useState('8')
  const [tipo, setTipo] = useState<'registrado' | 'mostrador'>('registrado')
  const [busqueda, setBusqueda] = useState('')
  const [clienteId, setClienteId] = useState<number | null>(null)
  const [nombre, setNombre] = useState('')
  const [telefono, setTelefono] = useState('')
  const [equipo, setEquipo] = useState('')
  const [omitir, setOmitir] = useState(false)
  const [vista, setVista] = useState<OcurrenciaSerie[] | null>(null)
  const clientes = useClientes({ busqueda, activo: 'activos', pagina: 0 })
  const sede = sedes.data?.find((s) => s.id === Number(sedeId))

  const datos = (): DatosSerie => {
    const inicio = aIsoBogota(fecha, desde)
    return {
      canchaId: Number(canchaId),
      inicio,
      fin: new Date(new Date(inicio).getTime() + Number(duracion) * 60_000).toISOString(),
      semanas: Number(semanas),
      clienteId: tipo === 'registrado' ? (clienteId ?? undefined) : undefined,
      mostradorNombre: tipo === 'mostrador' ? nombre.trim() : undefined,
      mostradorTelefono: tipo === 'mostrador' ? telefono.trim() : undefined,
      nombreEquipo: equipo.trim() || undefined,
      omitirOcupadas: omitir,
    }
  }
  const clienteOk =
    tipo === 'registrado' ? clienteId !== null : nombre.trim() !== '' && telefono.trim() !== ''
  const listo = canchaId !== '' && clienteOk && Number(semanas) >= 2
  const hayNoDisponibles = vista?.some((o) => o.estado !== 'DISPONIBLE') ?? false

  const revisar = () =>
    previsualizar.mutate(datos(), {
      onSuccess: setVista,
      onError: (e) => avisar.error(mensajeDeError(e)),
    })
  const crearSerie = () =>
    crear.mutate(datos(), {
      onSuccess: (r) => {
        avisar.exito(
          `Serie creada: ${r.reservas.length} reservas${r.omitidas.length ? `, ${r.omitidas.length} fechas omitidas` : ''}.`,
        )
        setVista(null)
        setOmitir(false)
      },
      onError: (e) => avisar.error(mensajeDeError(e)),
    })

  return (
    <Tarjeta className="flex flex-col gap-4">
      <h2 className="text-xl font-extrabold uppercase">Nueva serie</h2>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <CampoSelect
          etiqueta="Sede"
          value={sedeId}
          onChange={(e) => {
            setSedeId(e.target.value)
            setCanchaId('')
            setVista(null)
          }}
        >
          <option value="">Elige una sede</option>
          {sedes.data?.map((s) => (
            <option key={s.id} value={s.id}>
              {s.nombre}
            </option>
          ))}
        </CampoSelect>
        <CampoSelect
          etiqueta="Cancha"
          value={canchaId}
          disabled={!sede}
          onChange={(e) => {
            setCanchaId(e.target.value)
            setVista(null)
          }}
        >
          <option value="">Elige una cancha</option>
          {sede?.canchas.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombre}
            </option>
          ))}
        </CampoSelect>
        <Campo
          etiqueta="Primera fecha"
          type="date"
          value={fecha}
          onChange={(e) => {
            setFecha(e.target.value)
            setVista(null)
          }}
        />
        <Campo
          etiqueta="Hora de inicio"
          type="time"
          value={desde}
          onChange={(e) => {
            setDesde(e.target.value)
            setVista(null)
          }}
        />
        <Campo
          etiqueta="Duración (min)"
          type="number"
          min={30}
          step={30}
          value={duracion}
          onChange={(e) => {
            setDuracion(e.target.value)
            setVista(null)
          }}
        />
        <Campo
          etiqueta="Semanas (2 a 26)"
          type="number"
          min={2}
          max={26}
          value={semanas}
          onChange={(e) => {
            setSemanas(e.target.value)
            setVista(null)
          }}
        />
      </div>
      <div className="flex gap-2" role="group" aria-label="Tipo de cliente">
        {(['registrado', 'mostrador'] as const).map((t) => (
          <Boton
            key={t}
            tamano="sm"
            variante={tipo === t ? 'primario' : 'secundario'}
            onClick={() => setTipo(t)}
          >
            {t === 'registrado' ? 'Cliente registrado' : 'Cliente de mostrador'}
          </Boton>
        ))}
      </div>
      {tipo === 'registrado' ? (
        <div className="flex flex-col gap-2">
          <Campo
            etiqueta="Buscar cliente"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
          <ul className="flex max-h-40 flex-col gap-1 overflow-auto text-sm">
            {clientes.data?.contenido.map((c) => (
              <li key={c.id}>
                <button
                  type="button"
                  aria-pressed={clienteId === c.id}
                  onClick={() => setClienteId(c.id)}
                  className={cn(
                    'hover:bg-tenue rounded-boton w-full px-2 py-1 text-left',
                    clienteId === c.id && 'bg-primario-claro font-medium',
                  )}
                >
                  {c.nombres} {c.apellidos} · {c.correo}
                </button>
              </li>
            ))}
          </ul>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo etiqueta="Nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} />
          <Campo
            etiqueta="Teléfono"
            value={telefono}
            onChange={(e) => setTelefono(e.target.value)}
          />
        </div>
      )}
      <Campo
        etiqueta="Nombre del equipo (opcional)"
        value={equipo}
        onChange={(e) => setEquipo(e.target.value)}
        maxLength={100}
      />
      <div className="flex justify-end">
        <Boton disabled={!listo || previsualizar.isPending} onClick={revisar}>
          {previsualizar.isPending ? 'Revisando…' : 'Revisar fechas'}
        </Boton>
      </div>

      {vista && (
        <div className="flex flex-col gap-3">
          <ul className="flex flex-col gap-1 text-sm">
            {vista.map((o) => (
              <li
                key={o.numero}
                className="border-borde flex flex-wrap items-center justify-between gap-2 border-b py-1"
              >
                <span className="capitalize">
                  {o.numero}. {formatearFechaHora(o.inicio)}
                </span>
                <span className="flex items-center gap-2">
                  {o.estado === 'DISPONIBLE' ? (
                    formatearCOP(o.precio)
                  ) : (
                    <span className="text-texto-suave text-xs">{o.motivo}</span>
                  )}
                  <Insignia tono={TONO[o.estado]}>{TEXTO[o.estado]}</Insignia>
                </span>
              </li>
            ))}
          </ul>
          {hayNoDisponibles && (
            <Casilla
              etiqueta="Crear solo las fechas disponibles y omitir las demás"
              checked={omitir}
              onChange={(e) => setOmitir(e.target.checked)}
            />
          )}
          <div className="flex justify-end">
            <Boton
              disabled={
                crear.isPending ||
                (hayNoDisponibles && !omitir) ||
                vista.every((o) => o.estado !== 'DISPONIBLE')
              }
              onClick={crearSerie}
            >
              {crear.isPending ? 'Creando…' : 'Crear serie'}
            </Boton>
          </div>
        </div>
      )}
    </Tarjeta>
  )
}

export function PaginaSeries() {
  const { data, isPending, isError, error } = useSeries()
  const { cancelar } = useAccionesSerie()
  const [aCancelar, setACancelar] = useState<number | null>(null)

  return (
    <div className="flex max-w-4xl flex-col gap-6">
      <EncabezadoPagina
        titulo="Reservas recurrentes"
        descripcion="La misma reserva todas las semanas, confirmada de una vez. Primero se revisa cada fecha (horario, ocupación y precio) y puedes omitir las que no estén disponibles."
      />
      <FormularioSerie />
      <Tarjeta className="flex flex-col gap-3">
        <h2 className="text-xl font-extrabold uppercase">Series creadas</h2>
        {isPending && <Cargando texto="Cargando series…" />}
        {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
        {data?.length === 0 && (
          <p className="text-texto-suave py-6 text-center text-sm">Aún no hay series.</p>
        )}
        <ul className="flex flex-col gap-2">
          {data?.map((s) => (
            <li
              key={s.id}
              className="border-borde flex flex-wrap items-center justify-between gap-2 border-b pb-2 text-sm"
            >
              <span>
                <span className="font-medium">{s.cliente ?? 'Cliente'}</span> · {s.cancha}
                <span className="text-texto-suave block text-xs capitalize">
                  {DIAS[s.diaSemana - 1]} {hora(s.minutoInicio)} · {s.duracionMin} min ·{' '}
                  {s.reservasActivas} activas ({s.reservasFuturas} por jugar)
                </span>
              </span>
              {s.reservasFuturas > 0 && (
                <Boton tamano="sm" variante="fantasma" onClick={() => setACancelar(s.id)}>
                  Cancelar futuras
                </Boton>
              )}
            </li>
          ))}
        </ul>
      </Tarjeta>
      {aCancelar !== null && (
        <ModalMotivo
          titulo="Cancelar la serie"
          descripcion="Se cancelarán todas las reservas futuras que siguen activas y se liberarán las canchas."
          textoConfirmar="Cancelar reservas"
          alConfirmar={async (motivo) => {
            await cancelar.mutateAsync({ id: aCancelar, motivo }).then(
              (r) => {
                avisar.exito(`${r.canceladas} reservas canceladas.`)
                setACancelar(null)
              },
              (e) => avisar.error(mensajeDeError(e)),
            )
          }}
          alCerrar={() => setACancelar(null)}
        />
      )}
    </div>
  )
}
