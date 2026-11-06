import { useQuery } from '@tanstack/react-query'

import { api } from '@/lib/api'

interface DisponibilidadBot {
  disponible: boolean
  enlace?: string
}

/** ¿Hay un bot de WhatsApp atendiendo? Si no hay, la web no muestra nada (falla en silencio). */
export const useBotWhatsapp = () =>
  useQuery({
    queryKey: ['publico', 'whatsapp'],
    queryFn: async () => (await api.get<DisponibilidadBot>('/publico/whatsapp')).data,
    staleTime: 5 * 60_000,
    retry: false,
  })
