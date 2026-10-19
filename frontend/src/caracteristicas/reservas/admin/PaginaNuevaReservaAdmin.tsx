import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'

import { useClientes } from '@/caracteristicas/admin/consultasAdmin'
import { nombreCompleto } from '@/caracteristicas/autenticacion/almacenSesion'
import { useCotizacion, useSedesAdmin } from '@/caracteristicas/sedes/consultas'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, CampoTexto } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearHora, hoyEnBogota } from '@/lib/formato'
import { cn } from '@/lib/utils'

import { useDisponibilidad } from '../consultas'
import type { Slot } from '../tipos'
import { duracionesPosibles, sumarMinutos, textoDuracion } from '../utilidades'
import { useBuscarClientesMostrador, useCrearReservaAdmin } from './consultas'

/** Una reserva a nombre de un cliente registrado o de mostrador: nace confirmada, sin antelación mínima. */
export function PaginaNuevaReservaAdmin() {
  const [params] = useSearchParams()
  const navegar = useNavigate()
  const sedes = useSedesAdmin()
  const crear = useCrearReservaAdmin()

  const [sedeId, setSedeId] = useState<number | null>(
    params.get('sedeId') ? Number(params.get('sedeId')) : null,
  )
  const [canchaId, setCanchaId] = useState<number | null>(
    params.get('canchaId') ? Number(params.get('canchaId')) : null,
  )
  const [fecha, setFecha] = useState(params.get('fecha') ?? hoyEnBogota())
  const [horaInicio, setHoraInicio] = useState<string | null>(params.get('hora'))
  const [minutos, setMinutos] = useState(60)
  const [tipoCliente, setTipoCliente] = useState<'registrado' | 'mostrador'>('registrado')
  const [busqueda, setBusqueda] = useState('')
  const [clienteId, setClienteId] = useState<number | null>(null)
  const [nombre, setNombre] = useState('')
  const [telefono, setTelefono] = useState('')
  const [equipo, setEquipo] = useState('')
  const [notas, setNotas] = useState('')
  const [error, setError] = useState<string | null>(null)

  const sedeActual = sedeId ?? sedes.data?.[0]?.id ?? null
  const sede = sedes.data?.find((s) => s.id === sedeActual)
  const disponibilidad = useDisponibilidad(sedeActual, fecha, '')
  const cancha = disponibilidad.data?.canchas.find((c) => c.canchaId === canchaId)
  const slot: Slot | undefined = cancha?.slots.find((s) => formatearHora(s.inicio) === horaInicio)
  const duraciones = useMemo(
    () => (cancha && slot ? duracionesPosibles(cancha, slot, true) : []),
    [cancha, slot],
  )

  useEffect(() => {
    if (duraciones.length > 0 && !duraciones.includes(minutos)) setMinutos(duraciones[0])
  }, [duraciones, minutos])

  const fin = slot ? sumarMinutos(slot.inicio, minutos) : null
  const cotizacion = useCotizacion(canchaId ?? 0, slot?.inicio ?? null, fin)
  const clientes = useClientes({ busqueda, activo: 'activos', pagina: 0 })
  const mostradores = useBuscarClientesMostrador(tipoCliente === 'mostrador' ? nombre : '')

  const clienteListo =
    tipoCliente === 'registrado'
      ? clienteId !== null
      : nombre.trim().length > 1 && telefono.trim().length >= 7
  const puedeEnviar = !!cancha && !!slot && !!fin && clienteListo && duraciones.includes(minutos)

  const enviar = () => {
    if (!cancha || !slot || !fin) return
    setError(null)
    crear.mutate(
      {
        canchaId: cancha.canchaId,
        inicio: slot.inicio,
        fin,
        clienteId: tipoCliente === 'registrado' ? (clienteId ?? undefined) : undefined,
        mostrador:
          tipoCliente === 'mostrador'
            ? { nombre: nombre.trim(), telefono: telefono.trim() }
            : undefined,
        nombreEquipo: equipo || undefined,
        notas: notas || undefined,
      },
      {
        onSuccess: (r) => {
          avisar.exito(`Reserva ${r.codigo} creada y confirmada.`)
          navegar(`/admin/reservas/${r.id}`)
        },
        onError: (e) => setError(mensajeDeError(e)),
      },
    )
  }

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6">
      <EncabezadoPagina
        titulo="Nueva reserva"
        volver={{ a: '/admin/reservas', texto: 'Reservas' }}
        descripcion="Queda confirmada de inmediato. Puedes reservar con poca antelación y sin el límite de reservas por cliente."
      />

      <Tarjeta className="flex flex-col gap-4">
        <h2 className="text-xl font-extrabold uppercase">1. Cancha y horario</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <CampoSelect
            etiqueta="Sede"
            value={sedeActual ?? ''}
            onChange={(e) => (
              setSedeId(Number(e.target.value)),
              setCanchaId(null),
              setHoraInicio(null)
            )}
          >
            {sedes.data?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nombre}
              </option>
            ))}
          </CampoSelect>
          <Campo
            etiqueta="Fecha"
            type="date"
            value={fecha}
            onChange={(e) => e.target.value && (setFecha(e.target.value), setHoraInicio(null))}
          />
        </div>
        {sede && sede.canchas.length === 0 && (
          <Alerta tipo="aviso">Esta sede no tiene canchas.</Alerta>
        )}
        {disponibilidad.isPending && <Cargando texto="Cargando horarios…" />}
        <div className="flex flex-col gap-3">
          {disponibilidad.data?.canchas.map((c) => (
            <div
              key={c.canchaId}
              className={cn(
                'rounded-boton border p-3',
                canchaId === c.canchaId ? 'border-primario' : 'border-borde',
              )}
            >
              <p className="mb-2 text-sm font-medium">
                {c.nombre} <span className="text-texto-suave font-normal">· {c.modalidad}</span>
              </p>
              {c.cerrado && <p className="text-texto-suave text-xs">Cerrado este día.</p>}
              <ul className="grid grid-cols-3 gap-1 sm:grid-cols-6">
                {c.slots.map((s) => {
                  const hora = formatearHora(s.inicio)
                  const libre = s.estado === 'DISPONIBLE' || s.estado === 'NO_RESERVABLE'
                  const activo = canchaId === c.canchaId && horaInicio === hora
                  return (
                    <li key={s.inicio}>
                      <button
                        type="button"
                        disabled={!libre || disponibilidad.isPlaceholderData}
                        aria-pressed={activo}
                        onClick={() => (setCanchaId(c.canchaId), setHoraInicio(hora))}
                        className={cn(
                          'rounded-boton w-full border px-1 py-1 text-xs',
                          activo
                            ? 'border-primario bg-primario text-white'
                            : libre
                              ? 'border-primario bg-primario-claro text-primario-oscuro'
                              : 'border-borde bg-borde/40 text-texto-suave line-through',
                        )}
                      >
                        {hora}
                      </button>
                    </li>
                  )
                })}
              </ul>
            </div>
          ))}
        </div>
        {cancha && slot && fin && (
          <div className="bg-primario-claro/60 rounded-boton flex flex-wrap items-end gap-4 px-4 py-3 text-sm">
            <CampoSelect
              etiqueta="Duración"
              value={minutos}
              onChange={(e) => setMinutos(Number(e.target.value))}
            >
              {duraciones.map((m) => (
                <option key={m} value={m}>
                  {textoDuracion(m)}
                </option>
              ))}
            </CampoSelect>
            <p>
              {cancha.nombre} · {formatearHora(slot.inicio)} – {formatearHora(fin)} ·{' '}
              <strong>
                {cotizacion.data
                  ? formatearCOP(cotizacion.data.total)
                  : cotizacion.isFetching
                    ? 'calculando…'
                    : '—'}
              </strong>
            </p>
          </div>
        )}
      </Tarjeta>

      <Tarjeta className="flex flex-col gap-4">
        <h2 className="text-xl font-extrabold uppercase">2. Cliente</h2>
        <div role="radiogroup" aria-label="Tipo de cliente" className="flex gap-2">
          {(['registrado', 'mostrador'] as const).map((t) => (
            <button
              key={t}
              type="button"
              role="radio"
              aria-checked={tipoCliente === t}
              onClick={() => setTipoCliente(t)}
              className={cn(
                'rounded-boton border px-4 py-2 text-sm',
                tipoCliente === t
                  ? 'border-primario bg-primario text-white'
                  : 'border-borde bg-superficie',
              )}
            >
              {t === 'registrado' ? 'Cliente registrado' : 'Cliente de mostrador'}
            </button>
          ))}
        </div>

        {tipoCliente === 'registrado' ? (
          <div className="flex flex-col gap-2">
            <Campo
              etiqueta="Buscar cliente"
              type="search"
              placeholder="Nombre, correo, celular o documento"
              value={busqueda}
              onChange={(e) => (setBusqueda(e.target.value), setClienteId(null))}
            />
            {clientes.isFetching && <Cargando className="py-1" />}
            <ul className="border-borde divide-borde rounded-boton max-h-52 divide-y overflow-y-auto border text-sm">
              {clientes.data?.contenido.map((c) => (
                <li key={c.id}>
                  <button
                    type="button"
                    onClick={() => setClienteId(c.id)}
                    aria-pressed={clienteId === c.id}
                    className={cn(
                      'hover:bg-tenue w-full px-3 py-2 text-left',
                      clienteId === c.id && 'bg-primario-claro font-medium',
                    )}
                  >
                    {nombreCompleto(c)}
                    <span className="text-texto-suave block text-xs">
                      {c.correo} · {c.telefono ?? 'sin celular'}
                    </span>
                  </button>
                </li>
              ))}
              {clientes.data?.contenido.length === 0 && (
                <li className="text-texto-suave px-3 py-3 text-center">
                  No hay clientes con esa búsqueda.
                </li>
              )}
            </ul>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            <div className="grid gap-4 sm:grid-cols-2">
              <Campo
                etiqueta="Nombre"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                maxLength={150}
              />
              <Campo
                etiqueta="Teléfono"
                type="tel"
                value={telefono}
                onChange={(e) => setTelefono(e.target.value)}
                maxLength={30}
                ayuda="Si el teléfono ya existe, se reutiliza ese cliente."
              />
            </div>
            {mostradores.data && mostradores.data.length > 0 && (
              <div className="text-sm">
                <p className="text-texto-suave mb-1 text-xs">¿Es alguien que ya vino?</p>
                <div className="flex flex-wrap gap-2">
                  {mostradores.data.map((m) => (
                    <button
                      key={m.id}
                      type="button"
                      className="border-borde hover:bg-tenue rounded-full border px-3 py-1 text-xs"
                      onClick={() => (setNombre(m.nombre), setTelefono(m.telefono ?? ''))}
                    >
                      {m.nombre} · {m.telefono}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        <Campo
          etiqueta="Nombre del equipo (opcional)"
          value={equipo}
          onChange={(e) => setEquipo(e.target.value)}
          maxLength={100}
        />
        <CampoTexto
          etiqueta="Notas (opcional)"
          value={notas}
          onChange={(e) => setNotas(e.target.value)}
          maxLength={500}
        />
      </Tarjeta>

      {error && <Alerta tipo="error">{error}</Alerta>}
      <div className="flex justify-end gap-2">
        <Link to="/admin/reservas">
          <Boton variante="secundario" tabIndex={-1}>
            Cancelar
          </Boton>
        </Link>
        <Boton onClick={enviar} disabled={!puedeEnviar || crear.isPending}>
          {crear.isPending ? 'Creando…' : 'Crear reserva'}
        </Boton>
      </div>
    </div>
  )
}
