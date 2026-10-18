import { fromZonedTime } from 'date-fns-tz'
import { useMemo, useState } from 'react'

import { Alerta } from '@/componentes/ui/Alerta'
import { Campo } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearHora, ZONA_HORARIA } from '@/lib/formato'

import { HORAS } from './constantes'
import { useCotizacion } from './consultas'
import type { Cancha } from './tipos'

const ORIGEN = { BASE: 'Precio base', TARIFA: 'Franja', FESTIVO: 'Tarifa de festivo' } as const

/** Calcula cuánto cuesta jugar: el precio lo calcula siempre el servidor (franjas, festivos y proporciones). */
export function Cotizador({ cancha }: { cancha: Cancha }) {
  const hoy = new Date().toLocaleDateString('en-CA', { timeZone: ZONA_HORARIA })
  const [fecha, setFecha] = useState(hoy)
  const [hora, setHora] = useState('19:00')
  const [minutos, setMinutos] = useState(cancha.duracionMinMin)

  const duraciones = useMemo(() => {
    const lista: number[] = []
    for (let m = cancha.duracionMinMin; m <= cancha.duracionMaxMin; m += cancha.duracionBloqueMin)
      lista.push(m)
    return lista
  }, [cancha])

  const { inicio, fin } = useMemo(() => {
    if (!fecha) return { inicio: null, fin: null }
    const desde = fromZonedTime(`${fecha}T${hora}:00`, ZONA_HORARIA)
    return {
      inicio: desde.toISOString(),
      fin: new Date(desde.getTime() + minutos * 60_000).toISOString(),
    }
  }, [fecha, hora, minutos])

  const { data, isFetching, isError, error } = useCotizacion(cancha.id, inicio, fin)

  return (
    <Tarjeta className="flex flex-col gap-4">
      <div>
        <h2 className="text-lg font-semibold">¿Cuánto cuesta?</h2>
        <p className="text-texto-suave text-sm">
          Elige fecha, hora y duración para ver el precio con su desglose.
        </p>
      </div>
      <div className="grid gap-3 sm:grid-cols-3">
        <Campo
          etiqueta="Fecha"
          type="date"
          min={hoy}
          value={fecha}
          onChange={(e) => setFecha(e.target.value)}
        />
        <CampoSelect
          etiqueta="Hora de inicio"
          value={hora}
          onChange={(e) => setHora(e.target.value)}
        >
          {HORAS.slice(0, 48).map((h) => (
            <option key={h}>{h}</option>
          ))}
        </CampoSelect>
        <CampoSelect
          etiqueta="Duración"
          value={minutos}
          onChange={(e) => setMinutos(Number(e.target.value))}
        >
          {duraciones.map((m) => (
            <option key={m} value={m}>
              {m % 60 === 0 ? `${m / 60} h` : `${Math.floor(m / 60)} h ${m % 60} min`}
            </option>
          ))}
        </CampoSelect>
      </div>
      {isFetching && <Cargando texto="Calculando…" className="py-2" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data && !isFetching && (
        <div aria-live="polite" className="flex flex-col gap-2">
          <p className="text-primario-oscuro text-2xl font-bold">{formatearCOP(data.total)}</p>
          <ul className="text-texto-suave text-sm">
            {data.tramos.map((t) => (
              <li key={t.inicio}>
                {formatearHora(t.inicio)} – {formatearHora(t.fin)} · {ORIGEN[t.origen]} a{' '}
                {formatearCOP(t.precioHora)}/h
              </li>
            ))}
          </ul>
        </div>
      )}
    </Tarjeta>
  )
}
