import { useQuery } from '@tanstack/react-query'

import { api } from '@/lib/api'

interface EstadoApi {
  aplicacion: string
  estado: string
  zonaHoraria: string
  fechaServidor: string
}

/** Consulta pública para comprobar que la API responde. */
export function useEstadoApi() {
  return useQuery({
    queryKey: ['estado-api'],
    queryFn: async () => (await api.get<EstadoApi>('/publico/estado')).data,
    retry: 1,
  })
}
