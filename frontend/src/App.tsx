import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'

import { PaginaAgenda } from '@/caracteristicas/reservas/admin/PaginaAgenda'
import { PaginaBloqueos } from '@/caracteristicas/reservas/admin/PaginaBloqueos'
import { PaginaNuevaReservaAdmin } from '@/caracteristicas/reservas/admin/PaginaNuevaReservaAdmin'
import { PaginaReservaAdminDetalle } from '@/caracteristicas/reservas/admin/PaginaReservaAdminDetalle'
import { PaginaAuditoria } from '@/caracteristicas/auditoria/PaginaAuditoria'
import { PaginaReservasAdmin } from '@/caracteristicas/reservas/admin/PaginaReservasAdmin'
import { PaginaMisReservas } from '@/caracteristicas/reservas/PaginaMisReservas'
import { PaginaReservaDetalle } from '@/caracteristicas/reservas/PaginaReservaDetalle'
import { PaginaReservar } from '@/caracteristicas/reservas/PaginaReservar'
import { PaginaNotificaciones } from '@/caracteristicas/notificaciones/PaginaNotificaciones'
import { PaginaCanchaForm } from '@/caracteristicas/sedes/admin/PaginaCanchaForm'
import { PaginaCanchasAdmin } from '@/caracteristicas/sedes/admin/PaginaCanchasAdmin'
import { PaginaFestivos } from '@/caracteristicas/sedes/admin/PaginaFestivos'
import { PaginaSedeForm } from '@/caracteristicas/sedes/admin/PaginaSedeForm'
import { PaginaSedesAdmin } from '@/caracteristicas/sedes/admin/PaginaSedesAdmin'
import { PaginaCanchaDetalle } from '@/caracteristicas/sedes/PaginaCanchaDetalle'
import { PaginaSedeDetalle } from '@/caracteristicas/sedes/PaginaSedeDetalle'
import { PaginaSedes } from '@/caracteristicas/sedes/PaginaSedes'
import { PaginaEquipoAdmin } from '@/caracteristicas/torneos/admin/PaginaEquipoAdmin'
import { PaginaEquiposAdmin } from '@/caracteristicas/torneos/admin/PaginaEquiposAdmin'
import { PaginaPartidoAdmin } from '@/caracteristicas/torneos/admin/PaginaPartidoAdmin'
import { PaginaTorneoForm } from '@/caracteristicas/torneos/admin/PaginaTorneoForm'
import { PaginaTorneoGestion } from '@/caracteristicas/torneos/admin/PaginaTorneoGestion'
import { PaginaTorneosAdmin } from '@/caracteristicas/torneos/admin/PaginaTorneosAdmin'
import { PaginaEquipoPublico } from '@/caracteristicas/torneos/PaginaEquipoPublico'
import { PaginaJugadorPublico } from '@/caracteristicas/torneos/PaginaJugadorPublico'
import { PaginaPartido } from '@/caracteristicas/torneos/PaginaPartido'
import { PaginaTorneoDetalle } from '@/caracteristicas/torneos/PaginaTorneoDetalle'
import { PaginaTorneos } from '@/caracteristicas/torneos/PaginaTorneos'
import { PaginaClientes } from '@/caracteristicas/admin/PaginaClientes'
;('@/caracteristicas/admin/PaginaClientes')
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
              <Route path="sedes" element={<PaginaSedes />} />
              <Route path="sedes/:id" element={<PaginaSedeDetalle />} />
              <Route path="canchas/:id" element={<PaginaCanchaDetalle />} />
              <Route path="reservar" element={<PaginaReservar />} />
              <Route path="torneos" element={<PaginaTorneos />} />
              <Route path="torneos/partidos/:id" element={<PaginaPartido />} />
              <Route path="torneos/:id" element={<PaginaTorneoDetalle />} />
              <Route path="equipos/:id" element={<PaginaEquipoPublico />} />
              <Route path="jugadores/:id" element={<PaginaJugadorPublico />} />
              <Route path="ingresar" element={<PaginaIngresar />} />
              <Route path="registro" element={<PaginaRegistro />} />
              <Route path="olvide-clave" element={<PaginaOlvideClave />} />
              <Route path="restablecer-clave" element={<PaginaRestablecerClave />} />
              <Route path="verificar-correo" element={<PaginaVerificarCorreo />} />
              <Route element={<RutaProtegida />}>
                <Route path="perfil" element={<PaginaPerfil />} />
                <Route path="mis-reservas" element={<PaginaMisReservas />} />
                <Route path="mis-reservas/:id" element={<PaginaReservaDetalle />} />
                <Route path="notificaciones" element={<PaginaNotificaciones />} />
              </Route>
              <Route path="*" element={<PaginaNoEncontrada />} />
            </Route>

            <Route element={<RutaProtegida rol="ADMIN" />}>
              <Route path="admin" element={<LayoutAdmin />}>
                <Route index element={<PaginaPanelAdmin />} />
                <Route path="agenda" element={<PaginaAgenda />} />
                <Route path="reservas" element={<PaginaReservasAdmin />} />
                <Route path="reservas/nueva" element={<PaginaNuevaReservaAdmin />} />
                <Route path="reservas/:id" element={<PaginaReservaAdminDetalle />} />
                <Route path="bloqueos" element={<PaginaBloqueos />} />
                <Route path="auditoria" element={<PaginaAuditoria />} />
                <Route path="sedes" element={<PaginaSedesAdmin />} />
                <Route path="sedes/nueva" element={<PaginaSedeForm />} />
                <Route path="sedes/:id" element={<PaginaSedeForm />} />
                <Route path="canchas" element={<PaginaCanchasAdmin />} />
                <Route path="canchas/nueva" element={<PaginaCanchaForm />} />
                <Route path="canchas/:id" element={<PaginaCanchaForm />} />
                <Route path="festivos" element={<PaginaFestivos />} />
                <Route path="equipos" element={<PaginaEquiposAdmin />} />
                <Route path="equipos/:id" element={<PaginaEquipoAdmin />} />
                <Route path="torneos" element={<PaginaTorneosAdmin />} />
                <Route path="torneos/nuevo" element={<PaginaTorneoForm />} />
                <Route path="torneos/:id" element={<PaginaTorneoGestion />} />
                <Route path="torneos/:id/editar" element={<PaginaTorneoForm />} />
                <Route path="partidos/:id" element={<PaginaPartidoAdmin />} />
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
