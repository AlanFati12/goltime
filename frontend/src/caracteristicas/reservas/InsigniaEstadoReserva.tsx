import { Insignia } from '@/componentes/ui/Insignia'

import type { EstadoReserva } from './tipos'
import { ETIQUETA_ESTADO_RESERVA, TONO_ESTADO_RESERVA } from './utilidades'

export function InsigniaEstadoReserva({ estado }: { estado: EstadoReserva }) {
  return <Insignia tono={TONO_ESTADO_RESERVA[estado]}>{ETIQUETA_ESTADO_RESERVA[estado]}</Insignia>
}
