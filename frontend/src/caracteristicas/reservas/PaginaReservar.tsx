import { Clock, MapPin, SearchX } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'

import { useSesion } from '@/caracteristicas/autenticacion/almacenSesion'
import { useSedesPublicas } from '@/caracteristicas/sedes/consultas'
import { HORAS } from '@/caracteristicas/sedes/constantes'
import { Alerta } from '@/componentes/ui/Alerta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { Insignia } from '@/componentes/ui/Insignia'
import { Plegable } from '@/componentes/ui/Plegable'
import { Campo } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearDia, formatearHora, hoyEnBogota } from '@/lib/formato'
import { cn } from '@/lib/utils'

import { BotonEspera } from '@/caracteristicas/extras/BotonEspera'
import { ModalReserva } from './ModalReserva'
import { useDisponibilidad, useSemana } from './consultas'
import type { DisponibilidadCancha, Slot } from './tipos'
import { sumarMinutos } from './utilidades'

const ETIQUETA_SLOT: Record<Slot['estado'], string> = {
  DISPONIBLE: '',
  OCUPADO: 'Ocupado',
  BLOQUEADO: 'Bloqueado',
  NO_RESERVABLE: 'No disponible',
}

export function PaginaReservar() {
  const [params, setParams] = useSearchParams()
  const usuario = useSesion((s) => s.usuario)
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const sedes = useSedesPublicas()

  const sedeId = params.get('sedeId') ? Number(params.get('sedeId')) : (sedes.data?.[0]?.id ?? null)
  const fecha = params.get('fecha') ?? hoyEnBogota()
  const modalidad = params.get('modalidad') ?? ''
  const canchaResaltada = params.get('canchaId') ? Number(params.get('canchaId')) : null

  const [desde, setDesde] = useState('00:00')
  const [hasta, setHasta] = useState('24:00')
  const [precioMax, setPrecioMax] = useState('')
  const [elegido, setElegido] = useState<{ cancha: DisponibilidadCancha; slot: Slot } | null>(null)

  const sede = sedes.data?.find((s) => s.id === sedeId)
  const modalidades = useMemo(
    () => [...new Set(sede?.canchas.map((c) => c.modalidad) ?? [])],
    [sede],
  )
  const disponibilidad = useDisponibilidad(sedeId, fecha, modalidad)
  const semana = useSemana(sedeId, modalidad)

  const cambiar = (parcial: Record<string, string>) => {
    const siguiente = new URLSearchParams(params)
    Object.entries(parcial).forEach(([k, v]) => (v ? siguiente.set(k, v) : siguiente.delete(k)))
    setParams(siguiente, { replace: true })
  }

  const filtrosExtra = desde !== '00:00' || hasta !== '24:00' || precioMax !== ''

  const pasaFiltros = (s: Slot) => {
    const hora = formatearHora(s.inicio)
    return hora >= desde && hora < hasta && (!precioMax || s.precio <= Number(precioMax))
  }

  const elegirSlot = (cancha: DisponibilidadCancha, slot: Slot) => {
    if (!usuario) {
      navegar('/ingresar', { state: { desde: ubicacion.pathname + ubicacion.search } })
      return
    }
    setElegido({ cancha, slot })
  }

  return (
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Reservar una cancha"
        descripcion={
          usuario
            ? 'Elige sede y fecha, y toca un horario libre. El administrador confirmará tu solicitud.'
            : 'Elige sede y fecha, y toca un horario libre. Para solicitarlo te pediremos iniciar sesión.'
        }
      />

      <Tarjeta className="flex flex-col gap-4 p-4 sm:p-5">
        <div className="grid gap-4 sm:grid-cols-3">
          <CampoSelect
            etiqueta="Sede"
            value={sedeId ?? ''}
            onChange={(e) => cambiar({ sedeId: e.target.value, modalidad: '', canchaId: '' })}
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
            min={hoyEnBogota()}
            value={fecha}
            onChange={(e) => e.target.value && cambiar({ fecha: e.target.value })}
          />
          <CampoSelect
            etiqueta="Modalidad"
            value={modalidad}
            onChange={(e) => cambiar({ modalidad: e.target.value })}
          >
            <option value="">Todas</option>
            {modalidades.map((m) => (
              <option key={m}>{m}</option>
            ))}
          </CampoSelect>
        </div>
        <Plegable
          titulo="Filtrar por hora o precio"
          resumen={filtrosExtra ? '(activo)' : undefined}
          abiertoInicial={filtrosExtra}
        >
          <div className="grid gap-4 sm:grid-cols-3">
            <CampoSelect
              etiqueta="Desde las"
              value={desde}
              onChange={(e) => setDesde(e.target.value)}
            >
              {HORAS.slice(0, 48).map((h) => (
                <option key={h}>{h}</option>
              ))}
            </CampoSelect>
            <CampoSelect
              etiqueta="Hasta las"
              value={hasta}
              onChange={(e) => setHasta(e.target.value)}
            >
              {HORAS.slice(1).map((h) => (
                <option key={h}>{h}</option>
              ))}
            </CampoSelect>
            <Campo
              etiqueta="Precio máximo por bloque (COP)"
              type="number"
              inputMode="numeric"
              min={0}
              step={5000}
              value={precioMax}
              onChange={(e) => setPrecioMax(e.target.value)}
            />
          </div>
        </Plegable>
      </Tarjeta>

      {semana.data && (
        <section aria-label="Próximos 7 días" className="flex gap-2 overflow-x-auto pb-1">
          {semana.data.map((d) => (
            <button
              key={d.fecha}
              type="button"
              onClick={() => cambiar({ fecha: d.fecha })}
              aria-pressed={d.fecha === fecha}
              className={cn(
                'rounded-tarjeta min-w-24 border px-4 py-2.5 text-center text-sm whitespace-nowrap transition-colors',
                d.fecha === fecha
                  ? 'border-texto bg-texto text-white'
                  : 'border-borde bg-superficie hover:border-texto-suave/40',
              )}
            >
              <span className="block font-semibold capitalize">{formatearDia(d.fecha)}</span>
              <span
                className={cn(
                  'text-xs',
                  d.fecha === fecha
                    ? 'text-white/70'
                    : d.horariosLibres > 0
                      ? 'text-primario'
                      : 'text-texto-suave',
                )}
              >
                {d.horariosLibres > 0 ? `${d.horariosLibres} libres` : 'Sin cupos'}
              </span>
            </button>
          ))}
        </section>
      )}

      <div className="text-texto-suave flex flex-wrap gap-4 text-xs" aria-hidden>
        <span className="flex items-center gap-1.5">
          <span className="border-primario/40 bg-superficie size-3 rounded border" /> Disponible
        </span>
        <span className="flex items-center gap-1.5">
          <span className="bg-tenue size-3 rounded" /> Ocupado o bloqueado
        </span>
      </div>

      {(sedes.isPending || disponibilidad.isPending) && sedeId !== null && (
        <Cargando texto="Cargando horarios…" />
      )}
      {sedes.isError && <Alerta tipo="error">{mensajeDeError(sedes.error)}</Alerta>}
      {disponibilidad.isError && (
        <Alerta tipo="error">{mensajeDeError(disponibilidad.error)}</Alerta>
      )}
      {sedes.data?.length === 0 && (
        <EstadoVacio icono={MapPin} titulo="Todavía no hay sedes para reservar" />
      )}
      {disponibilidad.data?.canchas.length === 0 && (
        <EstadoVacio
          icono={SearchX}
          titulo="No hay canchas con esas características"
          descripcion="Prueba con otra modalidad o con otra sede."
        />
      )}

      <div className="flex flex-col gap-4">
        {disponibilidad.data?.canchas.map((c) => {
          const visibles = c.slots.filter(pasaFiltros)
          return (
            <Tarjeta
              key={c.canchaId}
              className={cn(
                canchaResaltada === c.canchaId && 'border-primario ring-primario/15 ring-4',
              )}
            >
              <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <h2 className="text-base font-semibold">{c.nombre}</h2>
                  <Insignia tono="verde">{c.modalidad}</Insignia>
                </div>
                <span className="text-texto-suave flex items-center gap-1 text-xs">
                  <Clock className="size-3.5" aria-hidden />
                  Bloques de {c.duracionBloqueMin} min
                </span>
              </div>
              {c.cerrado && <p className="text-texto-suave text-sm">Cerrado este día.</p>}
              {!c.cerrado && visibles.length === 0 && (
                <p className="text-texto-suave text-sm">No hay horarios con esos filtros.</p>
              )}
              <ul className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-6">
                {visibles.map((s) => {
                  const libre = s.estado === 'DISPONIBLE'
                  return (
                    <li key={s.inicio}>
                      <button
                        type="button"
                        disabled={!libre || disponibilidad.isPlaceholderData}
                        onClick={() => elegirSlot(c, s)}
                        aria-label={`${c.nombre}, ${formatearHora(s.inicio)}, ${libre ? formatearCOP(s.precio) : ETIQUETA_SLOT[s.estado]}`}
                        className={cn(
                          'rounded-boton group w-full border px-2 py-2.5 text-center text-sm transition-all',
                          libre
                            ? 'border-primario/30 bg-superficie hover:border-primario hover:bg-primario hover:shadow-suave hover:text-white'
                            : 'bg-tenue text-texto-suave/70 cursor-not-allowed border-transparent',
                        )}
                      >
                        <span
                          className={cn(
                            'block font-semibold tabular-nums',
                            !libre && 'font-normal line-through',
                          )}
                        >
                          {formatearHora(s.inicio)}
                        </span>
                        <span
                          className={cn(
                            'block text-xs',
                            libre && 'text-texto-suave group-hover:text-white/85',
                          )}
                        >
                          {libre ? formatearCOP(s.precio) : ETIQUETA_SLOT[s.estado]}
                        </span>
                      </button>
                      {s.estado === 'OCUPADO' && usuario?.rol === 'CLIENTE' && (
                        <BotonEspera
                          canchaId={c.canchaId}
                          inicio={s.inicio}
                          fin={sumarMinutos(s.inicio, c.duracionBloqueMin)}
                        />
                      )}
                    </li>
                  )
                })}
              </ul>
            </Tarjeta>
          )
        })}
      </div>

      {elegido && (
        <ModalReserva
          cancha={elegido.cancha}
          slot={elegido.slot}
          alCerrar={() => setElegido(null)}
        />
      )}
    </div>
  )
}
