import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Suspense } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router-dom'

import { ProveedorSesion } from '@/caracteristicas/autenticacion/ProveedorSesion'
import { RutaProtegida } from '@/caracteristicas/autenticacion/RutaProtegida'
import { LayoutPublico } from '@/componentes/layout/LayoutPublico'
import { Cargando } from '@/componentes/ui/Cargando'
import { ContenedorAvisos } from '@/componentes/ui/ContenedorAvisos'
import { perezosa } from '@/lib/perezosa'
import { PaginaInicio } from '@/paginas/PaginaInicio'
import { PaginaNoEncontrada } from '@/paginas/PaginaNoEncontrada'

// Cada página fuera de la portada se descarga al visitarla (ver lib/perezosa.ts).
const PaginaAgenda = perezosa(
  () => import('@/caracteristicas/reservas/admin/PaginaAgenda'),
  'PaginaAgenda',
)
const PaginaBloqueos = perezosa(
  () => import('@/caracteristicas/reservas/admin/PaginaBloqueos'),
  'PaginaBloqueos',
)
const PaginaNuevaReservaAdmin = perezosa(
  () => import('@/caracteristicas/reservas/admin/PaginaNuevaReservaAdmin'),
  'PaginaNuevaReservaAdmin',
)
const PaginaReservaAdminDetalle = perezosa(
  () => import('@/caracteristicas/reservas/admin/PaginaReservaAdminDetalle'),
  'PaginaReservaAdminDetalle',
)
const PaginaAuditoria = perezosa(
  () => import('@/caracteristicas/auditoria/PaginaAuditoria'),
  'PaginaAuditoria',
)
const PaginaReservasAdmin = perezosa(
  () => import('@/caracteristicas/reservas/admin/PaginaReservasAdmin'),
  'PaginaReservasAdmin',
)
const PaginaMisReservas = perezosa(
  () => import('@/caracteristicas/reservas/PaginaMisReservas'),
  'PaginaMisReservas',
)
const PaginaReservaDetalle = perezosa(
  () => import('@/caracteristicas/reservas/PaginaReservaDetalle'),
  'PaginaReservaDetalle',
)
const PaginaReservar = perezosa(
  () => import('@/caracteristicas/reservas/PaginaReservar'),
  'PaginaReservar',
)
const PaginaNotificaciones = perezosa(
  () => import('@/caracteristicas/notificaciones/PaginaNotificaciones'),
  'PaginaNotificaciones',
)
const PaginaCanchaForm = perezosa(
  () => import('@/caracteristicas/sedes/admin/PaginaCanchaForm'),
  'PaginaCanchaForm',
)
const PaginaCanchasAdmin = perezosa(
  () => import('@/caracteristicas/sedes/admin/PaginaCanchasAdmin'),
  'PaginaCanchasAdmin',
)
const PaginaFestivos = perezosa(
  () => import('@/caracteristicas/sedes/admin/PaginaFestivos'),
  'PaginaFestivos',
)
const PaginaSedeForm = perezosa(
  () => import('@/caracteristicas/sedes/admin/PaginaSedeForm'),
  'PaginaSedeForm',
)
const PaginaSedesAdmin = perezosa(
  () => import('@/caracteristicas/sedes/admin/PaginaSedesAdmin'),
  'PaginaSedesAdmin',
)
const PaginaCanchaDetalle = perezosa(
  () => import('@/caracteristicas/sedes/PaginaCanchaDetalle'),
  'PaginaCanchaDetalle',
)
const PaginaSedeDetalle = perezosa(
  () => import('@/caracteristicas/sedes/PaginaSedeDetalle'),
  'PaginaSedeDetalle',
)
const PaginaSedes = perezosa(() => import('@/caracteristicas/sedes/PaginaSedes'), 'PaginaSedes')
const PaginaEquipoAdmin = perezosa(
  () => import('@/caracteristicas/torneos/admin/PaginaEquipoAdmin'),
  'PaginaEquipoAdmin',
)
const PaginaEquiposAdmin = perezosa(
  () => import('@/caracteristicas/torneos/admin/PaginaEquiposAdmin'),
  'PaginaEquiposAdmin',
)
const PaginaPartidoAdmin = perezosa(
  () => import('@/caracteristicas/torneos/admin/PaginaPartidoAdmin'),
  'PaginaPartidoAdmin',
)
const PaginaTorneoForm = perezosa(
  () => import('@/caracteristicas/torneos/admin/PaginaTorneoForm'),
  'PaginaTorneoForm',
)
const PaginaTorneoGestion = perezosa(
  () => import('@/caracteristicas/torneos/admin/PaginaTorneoGestion'),
  'PaginaTorneoGestion',
)
const PaginaTorneosAdmin = perezosa(
  () => import('@/caracteristicas/torneos/admin/PaginaTorneosAdmin'),
  'PaginaTorneosAdmin',
)
const PaginaEquipoPublico = perezosa(
  () => import('@/caracteristicas/torneos/PaginaEquipoPublico'),
  'PaginaEquipoPublico',
)
const PaginaJugadorPublico = perezosa(
  () => import('@/caracteristicas/torneos/PaginaJugadorPublico'),
  'PaginaJugadorPublico',
)
const PaginaPartido = perezosa(
  () => import('@/caracteristicas/torneos/PaginaPartido'),
  'PaginaPartido',
)
const PaginaTorneoDetalle = perezosa(
  () => import('@/caracteristicas/torneos/PaginaTorneoDetalle'),
  'PaginaTorneoDetalle',
)
const PaginaTorneos = perezosa(
  () => import('@/caracteristicas/torneos/PaginaTorneos'),
  'PaginaTorneos',
)
const PaginaEstadisticas = perezosa(
  () => import('@/caracteristicas/estadisticas/PaginaEstadisticas'),
  'PaginaEstadisticas',
)
const PaginaMisPremios = perezosa(
  () => import('@/caracteristicas/promociones/PaginaMisPremios'),
  'PaginaMisPremios',
)
const PaginaPremiosAdmin = perezosa(
  () => import('@/caracteristicas/promociones/PaginaPremiosAdmin'),
  'PaginaPremiosAdmin',
)
const PaginaPromociones = perezosa(
  () => import('@/caracteristicas/promociones/PaginaPromociones'),
  'PaginaPromociones',
)
const PaginaPromocionesAdmin = perezosa(
  () => import('@/caracteristicas/promociones/PaginaPromocionesAdmin'),
  'PaginaPromocionesAdmin',
)
const PaginaFavoritos = perezosa(
  () => import('@/caracteristicas/extras/PaginasCliente'),
  'PaginaFavoritos',
)
const PaginaListaEspera = perezosa(
  () => import('@/caracteristicas/extras/PaginasCliente'),
  'PaginaListaEspera',
)
const PaginaMiCuenta = perezosa(
  () => import('@/caracteristicas/extras/PaginasCliente'),
  'PaginaMiCuenta',
)
const PaginaMisSolicitudes = perezosa(
  () => import('@/caracteristicas/extras/PaginasCliente'),
  'PaginaMisSolicitudes',
)
const PaginaSeries = perezosa(() => import('@/caracteristicas/extras/PaginaSeries'), 'PaginaSeries')
const PaginaConfiguracion = perezosa(
  () => import('@/caracteristicas/configuracion/PaginaConfiguracion'),
  'PaginaConfiguracion',
)
const PaginaClientes = perezosa(
  () => import('@/caracteristicas/admin/PaginaClientes'),
  'PaginaClientes',
)
const PaginaAdministradores = perezosa(
  () => import('@/caracteristicas/admin/PaginaAdministradores'),
  'PaginaAdministradores',
)
const PaginaPanelAdmin = perezosa(
  () => import('@/caracteristicas/admin/PaginaPanelAdmin'),
  'PaginaPanelAdmin',
)
const PaginaIngresar = perezosa(
  () => import('@/caracteristicas/autenticacion/PaginaIngresar'),
  'PaginaIngresar',
)
const PaginaCompletarPerfil = perezosa(
  () => import('@/caracteristicas/autenticacion/PaginaCompletarPerfil'),
  'PaginaCompletarPerfil',
)
const PaginaPerfil = perezosa(() => import('@/caracteristicas/perfil/PaginaPerfil'), 'PaginaPerfil')
const LayoutAdmin = perezosa(() => import('@/componentes/layout/LayoutAdmin'), 'LayoutAdmin')
const LayoutCuenta = perezosa(() => import('@/componentes/layout/LayoutCuenta'), 'LayoutCuenta')

const clienteConsultas = new QueryClient({
  defaultOptions: { queries: { staleTime: 30_000, refetchOnWindowFocus: false } },
})

export default function App() {
  return (
    <QueryClientProvider client={clienteConsultas}>
      <BrowserRouter>
        <ProveedorSesion>
          <Suspense fallback={<Cargando className="min-h-screen" />}>
            <Routes>
              <Route element={<LayoutPublico />}>
                <Route index element={<PaginaInicio />} />
                <Route path="sedes" element={<PaginaSedes />} />
                <Route path="sedes/:id" element={<PaginaSedeDetalle />} />
                <Route path="canchas/:id" element={<PaginaCanchaDetalle />} />
                <Route path="reservar" element={<PaginaReservar />} />
                <Route path="promociones" element={<PaginaPromociones />} />
                <Route path="torneos" element={<PaginaTorneos />} />
                <Route path="torneos/partidos/:id" element={<PaginaPartido />} />
                <Route path="torneos/:id" element={<PaginaTorneoDetalle />} />
                <Route path="equipos/:id" element={<PaginaEquipoPublico />} />
                <Route path="jugadores/:id" element={<PaginaJugadorPublico />} />
                <Route path="ingresar" element={<PaginaIngresar />} />
                <Route element={<RutaProtegida rol="CLIENTE" permitirIncompleto />}>
                  <Route path="completar-perfil" element={<PaginaCompletarPerfil />} />
                </Route>
                <Route element={<RutaProtegida />}>
                  <Route path="mis-reservas/:id" element={<PaginaReservaDetalle />} />
                  <Route element={<LayoutCuenta />}>
                    <Route path="perfil" element={<PaginaPerfil />} />
                    <Route path="mis-reservas" element={<PaginaMisReservas />} />
                    <Route path="notificaciones" element={<PaginaNotificaciones />} />
                    <Route path="mis-premios" element={<PaginaMisPremios />} />
                    <Route path="mi-cuenta" element={<PaginaMiCuenta />} />
                    <Route path="favoritos" element={<PaginaFavoritos />} />
                    <Route path="lista-espera" element={<PaginaListaEspera />} />
                    <Route path="mis-torneos" element={<PaginaMisSolicitudes />} />
                  </Route>
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
                  <Route path="configuracion" element={<PaginaConfiguracion />} />
                  <Route path="series" element={<PaginaSeries />} />
                  <Route path="estadisticas" element={<PaginaEstadisticas />} />
                  <Route path="promociones" element={<PaginaPromocionesAdmin />} />
                  <Route path="premios" element={<PaginaPremiosAdmin />} />
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
          </Suspense>
        </ProveedorSesion>
      </BrowserRouter>
      <ContenedorAvisos />
    </QueryClientProvider>
  )
}
