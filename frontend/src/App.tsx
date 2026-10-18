import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'

import { RutaProtegida } from '@/caracteristicas/autenticacion/RutaProtegida'
import { LayoutPublico } from '@/componentes/layout/LayoutPublico'
import { PaginaInicio } from '@/paginas/PaginaInicio'
import { PaginaNoEncontrada } from '@/paginas/PaginaNoEncontrada'

const clienteConsultas = new QueryClient({
  defaultOptions: { queries: { staleTime: 30_000, refetchOnWindowFocus: false } },
})

export default function App() {
  return (
    <QueryClientProvider client={clienteConsultas}>
      <BrowserRouter>
        <Routes>
          <Route element={<LayoutPublico />}>
            <Route index element={<PaginaInicio />} />
            {/* Las rutas de cliente y admin se agregan en las fases siguientes. */}
            <Route element={<RutaProtegida />} />
            <Route path="*" element={<PaginaNoEncontrada />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
