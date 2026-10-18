import { cva } from 'class-variance-authority'

export const estilosBoton = cva(
  'inline-flex shrink-0 items-center justify-center gap-2 rounded-boton font-medium whitespace-nowrap transition-all active:scale-[0.98] disabled:pointer-events-none disabled:opacity-50 [&_svg]:size-4 [&_svg]:shrink-0',
  {
    variants: {
      variante: {
        primario: 'bg-primario text-white shadow-suave hover:bg-primario-oscuro',
        secundario: 'border border-borde bg-superficie text-texto shadow-suave hover:bg-tenue',
        peligro: 'bg-peligro text-white shadow-suave hover:bg-peligro/90',
        fantasma: 'text-texto-suave hover:bg-tenue hover:text-texto',
        suave: 'bg-primario-claro text-primario-oscuro hover:bg-primario/15',
      },
      tamano: {
        sm: 'h-8 px-3 text-sm',
        md: 'h-10 px-4 text-sm',
        lg: 'h-12 px-6 text-base',
        icono: 'size-9',
      },
    },
    defaultVariants: { variante: 'primario', tamano: 'md' },
  },
)
