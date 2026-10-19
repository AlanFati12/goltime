import { Plus } from 'lucide-react'
import { useState } from 'react'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, Casilla } from '@/componentes/ui/Campo'
import { CampoSelect } from '@/componentes/ui/CampoSelect'
import { Cargando } from '@/componentes/ui/Cargando'
import { Insignia } from '@/componentes/ui/Insignia'
import { Modal } from '@/componentes/ui/Modal'
import { ModalMotivo } from '@/componentes/ui/ModalMotivo'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { EncabezadoPagina } from '@/componentes/ui/EncabezadoPagina'
import { mensajeDeError } from '@/lib/api'
import { formatearFechaHora } from '@/lib/formato'

import { useAccionesPremios, usePremiosOtorgados, useReglas } from './consultas'
import type { EstadoPremio, Premio, ReglaPremio } from './tipos'

const TONO: Record<EstadoPremio, 'verde' | 'gris' | 'ambar' | 'rojo'> = {
  DISPONIBLE: 'verde',
  USADO: 'gris',
  VENCIDO: 'ambar',
  REVOCADO: 'rojo',
}

function ModalRegla({ regla, alCerrar }: { regla: ReglaPremio | null; alCerrar: () => void }) {
  const { guardarRegla } = useAccionesPremios()
  const [nombre, setNombre] = useState(regla?.nombre ?? '')
  const [cada, setCada] = useState(regla?.reservasRequeridas.toString() ?? '5')
  const [tipo, setTipo] = useState(regla?.tipo ?? 'PORCENTAJE')
  const [valor, setValor] = useState(regla?.valor.toString() ?? '')
  const [dias, setDias] = useState(regla?.vigenciaDias.toString() ?? '60')
  const [activa, setActiva] = useState(regla?.activa ?? true)
  const valido =
    nombre.trim().length >= 3 && Number(cada) >= 1 && Number(valor) >= 1 && Number(dias) >= 1

  return (
    <Modal
      abierto
      titulo={regla ? 'Editar regla de premio' : 'Nueva regla de premio'}
      alCerrar={alCerrar}
    >
      <div className="flex flex-col gap-4">
        <Campo
          etiqueta="Nombre del premio"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          maxLength={100}
        />
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo
            etiqueta="Cada cuántas reservas completadas"
            type="number"
            min={1}
            value={cada}
            onChange={(e) => setCada(e.target.value)}
          />
          <Campo
            etiqueta="Vigencia del premio (días)"
            type="number"
            min={1}
            value={dias}
            onChange={(e) => setDias(e.target.value)}
          />
          <CampoSelect
            etiqueta="Beneficio"
            value={tipo}
            onChange={(e) => setTipo(e.target.value as ReglaPremio['tipo'])}
          >
            <option value="PORCENTAJE">Porcentaje de descuento</option>
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
        </div>
        <Casilla
          etiqueta="Regla activa"
          checked={activa}
          onChange={(e) => setActiva(e.target.checked)}
        />
        <p className="text-texto-suave text-xs">
          Cambiar la regla no altera los premios que los clientes ya ganaron. Solo cuentan las
          reservas completadas (no los partidos de torneo).
        </p>
        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={alCerrar}>
            Cancelar
          </Boton>
          <Boton
            disabled={!valido || guardarRegla.isPending}
            onClick={() =>
              guardarRegla.mutate(
                {
                  id: regla?.id ?? null,
                  datos: {
                    nombre: nombre.trim(),
                    reservasRequeridas: Number(cada),
                    tipo,
                    valor: Number(valor),
                    vigenciaDias: Number(dias),
                    activa,
                  },
                },
                {
                  onSuccess: () => {
                    avisar.exito('Regla guardada.')
                    alCerrar()
                  },
                  onError: (e) => avisar.error(mensajeDeError(e)),
                },
              )
            }
          >
            Guardar
          </Boton>
        </div>
      </div>
    </Modal>
  )
}

export function PaginaPremiosAdmin() {
  const reglas = useReglas()
  const [estado, setEstado] = useState<EstadoPremio | ''>('')
  const otorgados = usePremiosOtorgados(estado)
  const { revocar } = useAccionesPremios()
  const [regla, setRegla] = useState<{ regla: ReglaPremio | null } | null>(null)
  const [aRevocar, setARevocar] = useState<Premio | null>(null)

  return (
    <div className="flex flex-col gap-6">
      <EncabezadoPagina
        titulo="Premios de fidelidad"
        descripcion="Cada cierto número de reservas completadas, el cliente gana un descuento para una reserva posterior. Si se corrige una reserva completada y el cliente se queda sin las reservas necesarias, el premio sin usar se revoca solo."
        acciones={
          <Boton onClick={() => setRegla({ regla: null })}>
            <Plus aria-hidden />
            Nueva regla
          </Boton>
        }
      />

      <Tarjeta className="flex flex-col gap-3">
        <h2 className="text-xl font-extrabold uppercase">Reglas</h2>
        {reglas.isPending && <Cargando texto="Cargando reglas…" />}
        {reglas.isError && <Alerta tipo="error">{mensajeDeError(reglas.error)}</Alerta>}
        {reglas.data?.length === 0 && (
          <p className="text-texto-suave text-sm">Aún no hay reglas: nadie gana premios.</p>
        )}
        <ul className="flex flex-col gap-2">
          {reglas.data?.map((r) => (
            <li
              key={r.id}
              className="border-borde flex flex-wrap items-center justify-between gap-2 border-b pb-2"
            >
              <span>
                <span className="font-medium">{r.nombre}</span>{' '}
                <Insignia tono="verde">{r.beneficio}</Insignia>{' '}
                {!r.activa && <Insignia tono="rojo">Inactiva</Insignia>}
                <span className="text-texto-suave block text-xs">
                  Cada {r.reservasRequeridas} reservas completadas · vigencia {r.vigenciaDias} días
                </span>
              </span>
              <Boton tamano="sm" variante="secundario" onClick={() => setRegla({ regla: r })}>
                Editar
              </Boton>
            </li>
          ))}
        </ul>
      </Tarjeta>

      <Tarjeta className="flex flex-col gap-3">
        <div className="flex flex-wrap items-end justify-between gap-2">
          <h2 className="text-xl font-extrabold uppercase">Premios otorgados</h2>
          <CampoSelect
            className="w-44"
            etiqueta="Estado"
            value={estado}
            onChange={(e) => setEstado(e.target.value as EstadoPremio | '')}
          >
            <option value="">Todos</option>
            {(Object.keys(TONO) as EstadoPremio[]).map((e) => (
              <option key={e} value={e}>
                {e.charAt(0) + e.slice(1).toLowerCase()}
              </option>
            ))}
          </CampoSelect>
        </div>
        {otorgados.data?.length === 0 && (
          <p className="text-texto-suave text-sm">No hay premios.</p>
        )}
        <ul className="flex flex-col gap-2">
          {otorgados.data?.map((p) => (
            <li
              key={p.id}
              className="border-borde flex flex-wrap items-center justify-between gap-2 border-b pb-2 text-sm"
            >
              <span>
                <span className="font-medium">{p.nombre}</span>{' '}
                <Insignia tono={TONO[p.estado]}>{p.estado.toLowerCase()}</Insignia>
                <span className="text-texto-suave block text-xs">
                  Cliente #{p.clienteId} · {p.beneficio} · ganado {formatearFechaHora(p.otorgadoEn)}{' '}
                  · vence {formatearFechaHora(p.venceEn)}
                  {p.motivoRevocacion && ` · ${p.motivoRevocacion}`}
                </span>
              </span>
              {p.estado === 'DISPONIBLE' && (
                <Boton tamano="sm" variante="fantasma" onClick={() => setARevocar(p)}>
                  Revocar
                </Boton>
              )}
            </li>
          ))}
        </ul>
      </Tarjeta>

      {regla && <ModalRegla regla={regla.regla} alCerrar={() => setRegla(null)} />}
      {aRevocar && (
        <ModalMotivo
          titulo="Revocar premio"
          descripcion={`El cliente #${aRevocar.clienteId} dejará de poder usar "${aRevocar.nombre}".`}
          textoConfirmar="Revocar"
          alConfirmar={async (motivo) => {
            await revocar.mutateAsync({ id: aRevocar.id, motivo }).then(
              () => {
                avisar.exito('Premio revocado.')
                setARevocar(null)
              },
              (e) => avisar.error(mensajeDeError(e)),
            )
          }}
          alCerrar={() => setARevocar(null)}
        />
      )}
    </div>
  )
}
