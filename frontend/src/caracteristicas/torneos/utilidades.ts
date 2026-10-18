import type { DetallePartido, Lado, Partido } from './tipos'

export function nombreLado(lado: Lado | null): string {
  return lado?.nombre ?? lado?.etiqueta ?? 'Por definir'
}

/** "2 - 1" o "—" si aún no hay resultado. */
export function marcador(p: Partido): string {
  if (p.golesLocal == null || p.golesVisitante == null) return '—'
  return `${p.golesLocal} - ${p.golesVisitante}`
}

/** Penales como "(4-3 pen.)" cuando el partido se definió así. */
export function textoPenales(p: Partido): string | null {
  return p.penalesLocal != null && p.penalesVisitante != null
    ? `(${p.penalesLocal}-${p.penalesVisitante} pen.)`
    : null
}

/** Etiqueta corta de la jornada, ronda o grupo del partido. */
export function textoRonda(p: Partido): string {
  const partes = [p.grupo ? `Grupo ${p.grupo}` : null, p.nombreRonda].filter(Boolean)
  return partes.join(' · ')
}

export function agruparPor<T, K extends string | number>(
  lista: T[],
  clave: (x: T) => K,
): Map<K, T[]> {
  const mapa = new Map<K, T[]>()
  for (const x of lista) {
    const k = clave(x)
    mapa.set(k, [...(mapa.get(k) ?? []), x])
  }
  return mapa
}

/** Goles de [local, visitante] según la planilla: el autogol suma al rival de quien lo hizo. */
export function golesDePlanilla(detalle: DetallePartido): [number, number] {
  const localId = detalle.partido.local?.inscripcionId
  let local = 0
  let visitante = 0
  for (const e of detalle.eventos) {
    if (e.tipo !== 'GOL' && e.tipo !== 'GOL_PENAL' && e.tipo !== 'AUTOGOL') continue
    const deLocal = e.inscripcionId === localId
    const paraLocal = e.tipo === 'AUTOGOL' ? !deLocal : deLocal
    if (paraLocal) local++
    else visitante++
  }
  return [local, visitante]
}
