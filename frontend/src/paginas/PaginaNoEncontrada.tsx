import { Link } from 'react-router-dom'

export function PaginaNoEncontrada() {
  return (
    <section className="flex flex-col items-start gap-3 py-10">
      <h1 className="text-2xl font-bold sm:text-3xl">Página no encontrada</h1>
      <p className="text-texto-suave">La dirección que buscas no existe o fue movida.</p>
      <Link to="/" className="text-primario font-medium underline">
        Volver al inicio
      </Link>
    </section>
  )
}
