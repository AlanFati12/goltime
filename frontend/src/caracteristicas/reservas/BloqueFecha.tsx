import { ZONA_HORARIA } from '@/lib/formato'

/** Fecha compacta tipo calendario (día de la semana, número y mes) para listas de reservas. */
export function BloqueFecha({ fecha }: { fecha: string }) {
  const d = new Date(fecha)
  const parte = (opciones: Intl.DateTimeFormatOptions) =>
    new Intl.DateTimeFormat('es-CO', { timeZone: ZONA_HORARIA, ...opciones })
      .format(d)
      .replace('.', '')
  return (
    <div className="border-borde bg-superficie flex w-14 shrink-0 flex-col items-center overflow-hidden rounded-2xl border text-center">
      <span className="bg-noche text-volt-vivo w-full py-0.5 text-[0.65rem] font-bold uppercase">
        {parte({ month: 'short' })}
      </span>
      <span className="font-titulo cifras pt-1 text-2xl leading-none font-extrabold">
        {parte({ day: 'numeric' })}
      </span>
      <span className="text-texto-suave pb-1 text-[0.65rem] capitalize">
        {parte({ weekday: 'short' })}
      </span>
    </div>
  )
}
