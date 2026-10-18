import { Link } from 'react-router-dom'

import { Tarjeta } from '@/componentes/ui/Tarjeta'

const accesos = [
  {
    a: '/admin/agenda',
    titulo: 'Agenda',
    texto: 'Calendario por día o semana con las reservas y bloqueos de cada cancha.',
  },
  {
    a: '/admin/series',
    titulo: 'Reservas recurrentes',
    texto: 'La misma reserva todas las semanas, con revisión previa de cada fecha.',
  },
  {
    a: '/admin/bloqueos',
    titulo: 'Bloqueos',
    texto: 'Cierra horarios por mantenimiento, eventos o clima, avisando a los clientes afectados.',
  },
  {
    a: '/admin/auditoria',
    titulo: 'Auditoría',
    texto: 'Quién hizo qué sobre cada dato del negocio.',
  },
  {
    a: '/admin/reservas',
    titulo: 'Reservas',
    texto: 'Aprueba o rechaza las solicitudes pendientes y revisa todas las reservas.',
  },
  {
    a: '/admin/estadisticas',
    titulo: 'Estadísticas',
    texto: 'Ingresos reales, ocupación, horas pico y clientes frecuentes, con exportación a CSV.',
  },
  {
    a: '/admin/torneos',
    titulo: 'Torneos',
    texto:
      'Crea ligas, eliminatorias y grupos, genera el fixture, programa partidos y registra resultados.',
  },
  {
    a: '/admin/promociones',
    titulo: 'Promociones',
    texto: 'Descuentos por código o automáticos, con vigencia, franjas y cupos.',
  },
  {
    a: '/admin/premios',
    titulo: 'Premios',
    texto: 'Reglas de fidelidad: cada N reservas completadas, un descuento.',
  },
  {
    a: '/admin/equipos',
    titulo: 'Equipos',
    texto: 'Equipos y jugadores reutilizables entre torneos.',
  },
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
        Accesos rápidos a la operación diaria del negocio.
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
