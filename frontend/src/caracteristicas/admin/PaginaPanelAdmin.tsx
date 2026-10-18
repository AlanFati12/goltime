import { Link } from 'react-router-dom'

import { Tarjeta } from '@/componentes/ui/Tarjeta'

const accesos = [
  {
    a: '/admin/sedes',
    titulo: 'Sedes',
    texto: 'Crea sedes, define su horario de operación y sube fotos.',
  },
  {
    a: '/admin/canchas',
    titulo: 'Canchas',
    texto: 'Modalidades, precios, tarifas por franja, dependencias entre canchas y estado.',
  },
  {
    a: '/admin/festivos',
    titulo: 'Festivos',
    texto: 'Festivos de Colombia que activan las tarifas especiales.',
  },
  {
    a: '/admin/clientes',
    titulo: 'Clientes',
    texto: 'Busca, desactiva o reactiva clientes y habilita la inscripción de equipos.',
  },
  {
    a: '/admin/administradores',
    titulo: 'Administradores',
    texto: 'Crea cuentas para otros administradores del negocio.',
  },
]

export function PaginaPanelAdmin() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold">Panel de administración</h1>
      <p className="text-texto-suave max-w-2xl text-sm">
        Aquí irán los indicadores del día, la agenda de reservas y las estadísticas a medida que se
        construyan las siguientes fases.
      </p>
      <div className="grid gap-4 sm:grid-cols-2">
        {accesos.map((a) => (
          <Link key={a.a} to={a.a}>
            <Tarjeta className="hover:border-primario h-full transition-colors">
              <h2 className="mb-1 font-semibold">{a.titulo}</h2>
              <p className="text-texto-suave text-sm">{a.texto}</p>
            </Tarjeta>
          </Link>
        ))}
      </div>
    </div>
  )
}
