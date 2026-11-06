import { Megaphone, Plus } from 'lucide-react'
import { useState } from 'react'

import { useSedesAdmin } from '@/caracteristicas/sedes/consultas'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, CampoTexto, Casilla } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { Modal } from '@/componentes/ui/Modal'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { EstadoVacio } from '@/componentes/ui/EstadoVacio'
import { mensajeDeError } from '@/lib/api'
import { formatearCOP, formatearDia, hoyEnBogota, sumarDias } from '@/lib/formato'

import { useAccionesPromocion, useDetallePromocion, usePromocionesAdmin } from './consultas'
import type { Promocion } from './tipos'

const DIAS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

const aHora = (minutos: number | null) =>
  minutos == null
    ? ''
    : `${String(Math.floor(minutos / 60)).padStart(2, '0')}:${String(minutos % 60).padStart(2, '0')}`
const aMinutos = (hora: string): number | null => {
  if (!hora) return null
  const [h, m] = hora.split(':').map(Number)
  return h * 60 + m
}

function ModalPromocion({
  promocion,
  alCerrar,
}: {
  promocion: Promocion | null
  alCerrar: () => void
}) {
  const sedes = useSedesAdmin()
  const { guardar } = useAccionesPromocion()
  const [nombre, setNombre] = useState(promocion?.nombre ?? '')
  const [descripcion, setDescripcion] = useState(promocion?.descripcion ?? '')
  const [codigo, setCodigo] = useState(promocion?.codigo ?? '')
  const [tipo, setTipo] = useState(promocion?.tipo ?? 'PORCENTAJE')
  const [valor, setValor] = useState(promocion?.valor.toString() ?? '')
  const [desde, setDesde] = useState(promocion?.fechaDesde ?? hoyEnBogota())
  const [hasta, setHasta] = useState(promocion?.fechaHasta ?? sumarDias(hoyEnBogota(), 30))
  const [dias, setDias] = useState<number[]>(promocion?.diasSemana ?? [])
  const [horaDesde, setHoraDesde] = useState(aHora(promocion?.minutoInicio ?? null))
  const [horaHasta, setHoraHasta] = useState(aHora(promocion?.minutoFin ?? null))
  const [sedeId, setSedeId] = useState(promocion?.sedeId?.toString() ?? '')
  const [canchaId, setCanchaId] = useState(promocion?.canchaId?.toString() ?? '')
  const [usosMax, setUsosMax] = useState(promocion?.usosMaximos?.toString() ?? '')
  const [usosCliente, setUsosCliente] = useState(promocion?.usosPorCliente?.toString() ?? '')
  const [primera, setPrimera] = useState(promocion?.soloPrimeraReserva ?? false)
  const [publica, setPublica] = useState(promocion?.publica ?? true)
  const sede = sedes.data?.find((s) => s.id === Number(sedeId))
  const franjaIncompleta = (horaDesde === '') !== (horaHasta === '')
  const valido =
    nombre.trim().length >= 3 && Number(valor) >= 1 && desde <= hasta && !franjaIncompleta

  const enviar = () =>
    guardar.mutate(
      {
        id: promocion?.id ?? null,
        datos: {
          nombre: nombre.trim(),
          descripcion: descripcion.trim() || undefined,
          codigo: codigo.trim() || undefined,
          tipo,
          valor: Number(valor),
          fechaDesde: desde,
          fechaHasta: hasta,
          diasSemana: dias,
          minutoInicio: aMinutos(horaDesde),
          minutoFin: aMinutos(horaHasta),
          sedeId: sedeId ? Number(sedeId) : null,
          canchaId: canchaId ? Number(canchaId) : null,
          usosMaximos: usosMax ? Number(usosMax) : null,
          usosPorCliente: usosCliente ? Number(usosCliente) : null,
          soloPrimeraReserva: primera,
          publica,
        },
      },
      {
        onSuccess: () => {
          avisar.exito(promocion ? 'Promoción actualizada.' : 'Promoción creada.')
          alCerrar()
        },
        onError: (e) => avisar.error(mensajeDeError(e)),
      },
    )

  return (
    <Modal abierto titulo={promocion ? 'Editar promoción' : 'Nueva promoción'} alCerrar={alCerrar}>
      <div className="flex flex-col gap-4">
        <Campo
          etiqueta="Nombre"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          maxLength={100}
        />
        <CampoTexto
          etiqueta="Descripción (opcional)"
          value={descripcion}
          onChange={(e) => setDescripcion(e.target.value)}
        />
        <Campo
          etiqueta="Código (opcional)"
          ayuda="Con código la usa quien lo escribe. Sin código se aplica sola a las reservas que cumplan las condiciones."
          value={codigo}
          onChange={(e) => setCodigo(e.target.value.toUpperCase())}
          maxLength={40}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <CampoSelect
            etiqueta="Tipo de descuento"
            value={tipo}
            onChange={(e) => setTipo(e.target.value as Promocion['tipo'])}
          >
            <option value="PORCENTAJE">Porcentaje</option>
            <option value="MONTO_FIJO">Monto fijo (COP)</option>
          </CampoSelect>
          <Campo
            etiqueta={tipo === 'PORCENTAJE' ? 'Porcentaje (1 a 100)' : 'Monto (COP)'}
            type="number"
            min={1}
            max={tipo === 'PORCENTAJE' ? 100 : undefined}
            value={valor}
            onChange={(e) => setValor(e.target.value)}
          />
          <Campo
            etiqueta="Vigente desde"
            type="date"
            value={desde}
            onChange={(e) => setDesde(e.target.value)}
          />
          <Campo
            etiqueta="Vigente hasta"
            type="date"
            value={hasta}
            onChange={(e) => setHasta(e.target.value)}
          />
        </div>
        <fieldset className="flex flex-col gap-2">
          <legend className="text-sm font-medium">Días de la semana (ninguno = todos)</legend>
          <div className="flex flex-wrap gap-3">
            {DIAS.map((d, i) => (
              <Casilla
                key={d}
                etiqueta={d}
                checked={dias.includes(i + 1)}
                onChange={(e) =>
                  setDias(e.target.checked ? [...dias, i + 1] : dias.filter((x) => x !== i + 1))
                }
              />
            ))}
          </div>
        </fieldset>
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo
            etiqueta="Franja desde (opcional)"
            type="time"
            value={horaDesde}
            onChange={(e) => setHoraDesde(e.target.value)}
          />
          <Campo
            etiqueta="Franja hasta"
            type="time"
            value={horaHasta}
            error={franjaIncompleta ? 'Indica inicio y fin de la franja' : undefined}
            onChange={(e) => setHoraHasta(e.target.value)}
          />
          <CampoSelect
            etiqueta="Sede (opcional)"
            value={sedeId}
            onChange={(e) => {
              setSedeId(e.target.value)
              setCanchaId('')
            }}
          >
            <option value="">Todas</option>
            {sedes.data?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.nombre}
              </option>
            ))}
          </CampoSelect>
          <CampoSelect
            etiqueta="Cancha (opcional)"
            value={canchaId}
            disabled={!sede}
            onChange={(e) => setCanchaId(e.target.value)}
          >
            <option value="">Todas</option>
            {sede?.canchas.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
          </CampoSelect>
          <Campo
            etiqueta="Usos máximos en total (opcional)"
            type="number"
            min={1}
            value={usosMax}
            onChange={(e) => setUsosMax(e.target.value)}
          />
          <Campo
            etiqueta="Usos por cliente (opcional)"
            type="number"
            min={1}
            value={usosCliente}
            onChange={(e) => setUsosCliente(e.target.value)}
          />
        </div>
        <div className="flex flex-wrap gap-6">
          <Casilla
            etiqueta="Solo para la primera reserva del cliente"
            checked={primera}
            onChange={(e) => setPrimera(e.target.checked)}
          />
          <Casilla
            etiqueta="Mostrar en la página pública de promociones"
            checked={publica}
            onChange={(e) => setPublica(e.target.checked)}
          />
        </div>
        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton disabled={!valido || guardar.isPending} onClick={enviar}>
            {guardar.isPending ? 'Guardando…' : 'Guardar'}
          </Boton>
        </div>
      </div>
    </Modal>
  )
}

function ModalUsos({ id, alCerrar }: { id: number; alCerrar: () => void }) {
  const { data } = useDetallePromocion(id)
  return (
    <Modal abierto titulo="Uso de la promoción" alCerrar={alCerrar}>
      {!data ? (
        <Cargando texto="Cargando…" variante="tarjetas" />
      ) : (
        <div className="flex flex-col gap-3 text-sm">
          <p>
            <strong>{data.usosActivos}</strong> usos activos · {formatearCOP(data.montoDescontado)}{' '}
            descontados
          </p>
          <ul className="flex max-h-64 flex-col gap-1 overflow-auto">
            {data.ultimosUsos.map((u) => (
              <li key={u.reservaId} className="border-borde flex justify-between border-b py-1">
                <span>
                  Reserva #{u.reservaId} · cliente #{u.clienteId}
                </span>
                <span>
                  {formatearCOP(u.monto)}{' '}
                  {u.estado === 'LIBERADO' && <Insignia tono="gris">liberado</Insignia>}
                </span>
              </li>
            ))}
            {data.ultimosUsos.length === 0 && (
              <li className="text-texto-suave">Aún no se ha usado.</li>
            )}
          </ul>
          <div className="flex justify-end">
            <Boton variante="secundario" onClick={alCerrar}>
              Cerrar
            </Boton>
          </div>
        </div>
      )}
    </Modal>
  )
}

export function PaginaPromocionesAdmin() {
  const { data, isPending, isError, error } = usePromocionesAdmin()
  const { cambiarActiva } = useAccionesPromocion()
  const [editando, setEditando] = useState<{ promocion: Promocion | null } | null>(null)
  const [usos, setUsos] = useState<number | null>(null)
  const hoy = hoyEnBogota()

  return (
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Promociones"
        descripcion="Descuentos por código o automáticos. En cada reserva se aplica un solo descuento: el de mayor valor entre las promociones, el código y los premios de fidelidad."
        acciones={
          <Boton onClick={() => setEditando({ promocion: null })}>
            <Plus aria-hidden />
            Nueva promoción
          </Boton>
        }
      />
      {isPending && <Cargando texto="Cargando promociones…" variante="tarjetas" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data?.length === 0 && <EstadoVacio icono={Megaphone} titulo="Aún no hay promociones" />}
      <ul className="flex flex-col gap-3">
        {data?.map((p) => (
          <li key={p.id}>
            <Tarjeta className="flex flex-wrap items-start justify-between gap-3 p-4">
              <div className="flex flex-col gap-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-semibold">{p.nombre}</span>
                  <Insignia tono="verde">{p.descuentoTexto}</Insignia>
                  {p.codigo ? (
                    <Insignia tono="ambar">Código {p.codigo}</Insignia>
                  ) : (
                    <Insignia>Automática</Insignia>
                  )}
                  {!p.activa && <Insignia tono="rojo">Inactiva</Insignia>}
                  {p.activa && p.fechaHasta < hoy && <Insignia tono="gris">Vencida</Insignia>}
                  {!p.publica && <Insignia>Privada</Insignia>}
                </div>
                <p className="text-sm capitalize">
                  {formatearDia(p.fechaDesde)} → {formatearDia(p.fechaHasta)}
                  {p.diasSemana.length > 0 &&
                    ` · ${p.diasSemana.map((d) => DIAS[d - 1]).join(', ')}`}
                  {p.minutoInicio != null && ` · ${aHora(p.minutoInicio)}–${aHora(p.minutoFin)}`}
                </p>
                <p className="text-texto-suave text-xs">
                  {[
                    p.usosMaximos ? `${p.usosMaximos} usos en total` : null,
                    p.usosPorCliente ? `${p.usosPorCliente} por cliente` : null,
                    p.soloPrimeraReserva ? 'primera reserva' : null,
                  ]
                    .filter(Boolean)
                    .join(' · ') || 'Sin límite de usos'}
                </p>
              </div>
              <div className="flex gap-2">
                <Boton tamano="sm" variante="secundario" onClick={() => setUsos(p.id)}>
                  Usos
                </Boton>
                <Boton
                  tamano="sm"
                  variante="secundario"
                  onClick={() => setEditando({ promocion: p })}
                >
                  Editar
                </Boton>
                <Boton
                  tamano="sm"
                  variante={p.activa ? 'peligro' : 'primario'}
                  onClick={() =>
                    cambiarActiva.mutate(
                      { id: p.id, activa: !p.activa },
                      { onError: (e) => avisar.error(mensajeDeError(e)) },
                    )
                  }
                >
                  {p.activa ? 'Desactivar' : 'Activar'}
                </Boton>
              </div>
            </Tarjeta>
          </li>
        ))}
      </ul>
      {editando && (
        <ModalPromocion promocion={editando.promocion} alCerrar={() => setEditando(null)} />
      )}
      {usos !== null && <ModalUsos id={usos} alCerrar={() => setUsos(null)} />}
    </div>
  )
}
