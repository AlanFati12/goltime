import { useState } from 'react'

import { avisar } from '@/componentes/ui/almacenAvisos'
import { Alerta } from '@/componentes/ui/Alerta'
import { Boton } from '@/componentes/ui/Boton'
import { Campo, CampoTexto, Casilla } from '@/componentes/ui/Campo'
import { Cargando } from '@/componentes/ui/Cargando'
import { Tarjeta } from '@/componentes/ui/Tarjeta'
import { api, mensajeDeError } from '@/lib/api'
import { formatearFechaHora } from '@/lib/formato'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

interface Parametro {
  clave: string
  valor: string
  tipo: 'ENTERO' | 'DECIMAL' | 'BOOLEANO' | 'TEXTO'
  descripcion: string
  editable: boolean
  minimo?: number
  maximo?: number
  actualizadoEn: string
}

const GRUPOS: { titulo: string; claves: string[] }[] = [
  {
    titulo: 'Reservas y cancelaciones',
    claves: [
      'antelacion_minima_horas',
      'antelacion_maxima_dias',
      'max_reservas_activas_cliente',
      'horas_minimas_cancelacion',
      'max_reprogramaciones',
    ],
  },
  {
    titulo: 'Aprobación, estados y avisos',
    claves: [
      'horas_expiracion_aprobacion',
      'horas_expiracion_antes_inicio',
      'completar_reservas_automaticamente',
      'horas_recordatorio',
    ],
  },
  {
    titulo: 'Canchas y negocio',
    claves: ['modalidades_permitidas', 'contacto_negocio', 'terminos_y_condiciones'],
  },
]

function useParametros() {
  return useQuery({
    queryKey: ['configuracion'],
    queryFn: async () => (await api.get<Parametro[]>('/admin/configuracion')).data,
  })
}

function useGuardarParametro() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: async (p: { clave: string; valor: string }) =>
      (await api.put<Parametro>(`/admin/configuracion/${p.clave}`, { valor: p.valor })).data,
    onSuccess: () => void cliente.invalidateQueries({ queryKey: ['configuracion'] }),
  })
}

function FilaParametro({ p }: { p: Parametro }) {
  const guardar = useGuardarParametro()
  const [valor, setValor] = useState(p.valor)
  const cambio = valor !== p.valor

  const enviar = () =>
    guardar.mutate(
      { clave: p.clave, valor },
      {
        onSuccess: () => avisar.exito('Parámetro actualizado.'),
        onError: (e) => avisar.error(mensajeDeError(e)),
      },
    )

  return (
    <li className="border-borde flex flex-col gap-2 border-b pb-4 last:border-b-0">
      {p.tipo === 'BOOLEANO' ? (
        <Casilla
          etiqueta={p.descripcion}
          checked={valor === 'true'}
          onChange={(e) => setValor(e.target.checked ? 'true' : 'false')}
        />
      ) : p.clave === 'terminos_y_condiciones' || p.clave === 'contacto_negocio' ? (
        <CampoTexto
          etiqueta={p.descripcion}
          value={valor}
          maxLength={1000}
          onChange={(e) => setValor(e.target.value)}
        />
      ) : (
        <Campo
          etiqueta={p.descripcion}
          type={p.tipo === 'ENTERO' ? 'number' : 'text'}
          min={p.minimo}
          max={p.maximo}
          ayuda={p.minimo !== undefined ? `Entre ${p.minimo} y ${p.maximo}` : undefined}
          value={valor}
          onChange={(e) => setValor(e.target.value)}
        />
      )}
      <div className="flex items-center justify-between gap-2">
        <span className="text-texto-suave text-xs">
          Última actualización: {formatearFechaHora(p.actualizadoEn)}
        </span>
        <Boton tamano="sm" disabled={!cambio || guardar.isPending} onClick={enviar}>
          Guardar
        </Boton>
      </div>
    </li>
  )
}

/** Políticas del negocio. Los cambios aplican de inmediato a las reservas nuevas y quedan en la auditoría. */
export function PaginaConfiguracion() {
  const { data, isPending, isError, error } = useParametros()
  const porClave = new Map(data?.map((p) => [p.clave, p]))
  return (
    <div className="flex max-w-3xl flex-col gap-4">
      <h1 className="text-2xl font-bold">Configuración</h1>
      <Alerta tipo="info">
        Los cambios aplican a las reservas nuevas desde el momento de guardarlos; las que ya existen
        conservan lo que se les prometió. Cada cambio queda en la auditoría.
      </Alerta>
      {isPending && <Cargando texto="Cargando configuración…" />}
      {isError && <Alerta tipo="error">{mensajeDeError(error)}</Alerta>}
      {data &&
        GRUPOS.map((g) => (
          <Tarjeta key={g.titulo} className="flex flex-col gap-4">
            <h2 className="text-lg font-semibold">{g.titulo}</h2>
            <ul className="flex flex-col gap-4">
              {g.claves
                .map((c) => porClave.get(c))
                .filter((p): p is Parametro => p !== undefined && p.editable)
                .map((p) => (
                  <FilaParametro key={`${p.clave}-${p.valor}`} p={p} />
                ))}
            </ul>
          </Tarjeta>
        ))}
    </div>
  )
}
