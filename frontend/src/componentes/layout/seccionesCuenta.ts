import {
  Bell,
  CalendarCheck,
  Gift,
  Heart,
  Hourglass,
  LayoutGrid,
  Trophy,
  UserRound,
  type LucideIcon,
} from 'lucide-react'

/** Secciones del área personal del cliente, en el orden en que aparecen en el menú. */
export const SECCIONES_CUENTA: {
  a: string
  texto: string
  icono: LucideIcon
  descripcion: string
}[] = [
  {
    a: '/mi-cuenta',
    texto: 'Resumen',
    icono: LayoutGrid,
    descripcion: 'Todo lo de tu cuenta en un lugar.',
  },
  {
    a: '/mis-reservas',
    texto: 'Mis reservas',
    icono: CalendarCheck,
    descripcion: 'Próximas, historial y canceladas.',
  },
  {
    a: '/favoritos',
    texto: 'Favoritas',
    icono: Heart,
    descripcion: 'Vuelve a reservar tus canchas preferidas.',
  },
  {
    a: '/lista-espera',
    texto: 'Lista de espera',
    icono: Hourglass,
    descripcion: 'Horarios ocupados que esperas.',
  },
  {
    a: '/mis-premios',
    texto: 'Mis premios',
    icono: Gift,
    descripcion: 'Tu progreso de fidelidad y premios ganados.',
  },
  {
    a: '/mis-torneos',
    texto: 'Mis torneos',
    icono: Trophy,
    descripcion: 'Solicitudes de inscripción de tus equipos.',
  },
  {
    a: '/notificaciones',
    texto: 'Notificaciones',
    icono: Bell,
    descripcion: 'Avisos sobre tus reservas.',
  },
  { a: '/perfil', texto: 'Mi perfil', icono: UserRound, descripcion: 'Tus datos y contraseña.' },
]
