import { cva } from 'class-variance-authority'

/**
 * Botones redondos (píldora) en todo el sitio. `volt` es la acción principal de las páginas públicas
 * (lima con texto oscuro); `primario` es la acción principal dentro de la app (`invertido`: azul
 * medianoche en modo claro y lima en modo oscuro).
 */
export const estilosBoton = cva(
  'inline-flex shrink-0 items-center justify-center gap-2 rounded-full font-semibold whitespace-nowrap select-none transition-[background-color,color,box-shadow,transform,border-color] duration-200 ease-[var(--ease-salida)] active:scale-[0.97] disabled:pointer-events-none disabled:opacity-50 [&_svg]:size-4 [&_svg]:shrink-0 [&_svg]:transition-transform [&_svg]:duration-200 hover:[&_svg:last-child:not(:first-child)]:translate-x-0.5',
  {
    variants: {
      variante: {
        primario: 'bg-invertido text-sobre-invertido hover:bg-invertido-suave',
        volt: 'bg-volt text-noche hover:bg-volt-vivo hover:shadow-volt',
        secundario: 'border-borde bg-superficie text-texto hover:border-texto-suave/40 border',
        claro:
          'border border-white/25 bg-white/5 text-white backdrop-blur-sm hover:border-white/50 hover:bg-white/10',
        peligro: 'bg-peligro text-white hover:bg-peligro/90',
        fantasma: 'text-texto-suave hover:bg-tenue hover:text-texto',
        suave: 'bg-primario-claro text-primario-oscuro hover:bg-volt-claro',
      },
      tamano: {
        sm: 'h-9 px-4 text-sm',
        md: 'h-10 px-5 text-sm',
        lg: 'h-12 px-7 text-base',
        xl: 'h-14 px-8 text-[1.05rem]',
        icono: 'size-9',
      },
    },
    defaultVariants: { variante: 'primario', tamano: 'md' },
  },
)
