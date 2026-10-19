import { useEffect, useRef } from 'react'

/**
 * Observa el elemento y le pone la clase `visible` la primera vez que entra en pantalla.
 * El estilo de entrada vive en index.css (.revelar, .revelar-imagen) y respeta la preferencia
 * de movimiento reducido.
 */
export function useRevelar<T extends HTMLElement>() {
  const ref = useRef<T>(null)
  useEffect(() => {
    const el = ref.current
    if (!el) return
    if (typeof IntersectionObserver === 'undefined') {
      el.classList.add('visible')
      return
    }
    const observador = new IntersectionObserver(
      ([entrada]) => {
        if (entrada.isIntersecting) {
          el.classList.add('visible')
          observador.disconnect()
        }
      },
      { rootMargin: '0px 0px -12% 0px', threshold: 0.08 },
    )
    observador.observe(el)
    return () => observador.disconnect()
  }, [])
  return ref
}
