import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'

import { PaginaClientes } from '@/caracteristicas/admin/PaginaClientes'
import { PaginaAdministradores } from '@/caracteristicas/admin/PaginaAdministradores'
import { PaginaPanelAdmin } from '@/caracteristicas/admin/PaginaPanelAdmin'
import { PaginaIngresar } from '@/caracteristicas/autenticacion/PaginaIngresar'
import { PaginaOlvideClave } from '@/caracteristicas/autenticacion/PaginaOlvideClave'
import { PaginaRegistro } from '@/caracteristicas/autenticacion/PaginaRegistro'
import { PaginaRestablecerClave } from '@/caracteristicas/autenticacion/PaginaRestablecerClave'
import { PaginaVerificarCorreo } from '@/caracteristicas/autenticacion/PaginaVerificarCorreo'
import { ProveedorSesion } from '@/caracteristicas/autenticacion/ProveedorSesion'
import { RutaProtegida } from '@/caracteristicas/autenticacion/RutaProtegida'
import { PaginaPerfil } from '@/caracteristicas/perfil/PaginaPerfil'
import { LayoutAdmin } from '@/componentes/layout/LayoutAdmin'
import { LayoutPublico } from '@/componentes/layout/LayoutPublico'
import { ContenedorAvisos } from '@/componentes/ui/ContenedorAvisos'
import { PaginaInicio } from '@/paginas/PaginaInicio'
import { PaginaNoEncontrada } from '@/paginas/PaginaNoEncontrada'

const clienteConsultas = new QueryClient({
  defaultOptions: { queries: { staleTime: 30_000, refetchOnWindowFocus: false } },
})

export default function App() {
  return (
    <QueryClientProvider client={clienteConsultas}>
      <BrowserRouter>
        <ProveedorSesion>
          <Routes>
            <Route element={<LayoutPublico />}>
              <Route index element={<PaginaInicio />} />
              <Route path="ingresar" element={<PaginaIngresar />} />
              <Route path="registro" element={<PaginaRegistro />} />
              <Route path="olvide-clave" element={<PaginaOlvideClave />} />
              <Route path="restablecer-clave" element={<PaginaRestablecerClave />} />
              <Route path="verificar-correo" element={<PaginaVerificarCorreo />} />
              <Route element={<RutaProtegida />}>
                <Route path="perfil" element={<PaginaPerfil />} />
              </Route>
              <Route path="*" element={<PaginaNoEncontrada />} />
            </Route>

            <Route element={<RutaProtegida rol="ADMIN" />}>
              <Route path="admin" element={<LayoutAdmin />}>
                <Route index element={<PaginaPanelAdmin />} />
                <Route path="clientes" element={<PaginaClientes />} />
                <Route path="administradores" element={<PaginaAdministradores />} />
              </Route>
            </Route>
          </Routes>
        </ProveedorSesion>
      </BrowserRouter>
      <ContenedorAvisos />
    </QueryClientProvider>
  )
}
