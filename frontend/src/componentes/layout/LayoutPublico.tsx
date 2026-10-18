import { Link, Outlet } from 'react-router-dom'

export function LayoutPublico() {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-borde bg-superficie border-b">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <Link to="/" aria-label="Goltime, ir al inicio">
            <img src="/logo.svg" alt="Goltime" className="h-9" />
          </Link>
          <nav aria-label="Principal" className="text-texto-suave flex gap-4 text-sm font-medium">
            <Link to="/" className="hover:text-primario">
              Inicio
            </Link>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <Outlet />
      </main>
      <footer className="border-borde text-texto-suave border-t py-4 text-center text-xs">
        Goltime · Universidad Surcolombiana · INNOVASOFT IX
      </footer>
    </div>
  )
}
