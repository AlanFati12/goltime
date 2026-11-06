import { useEffect, useState } from 'react'

/**
 * Devuelve `valor` con retraso: solo cambia cuando pasan `ms` milisegundos sin que `valor` cambie. Sirve para
 * buscadores: la casilla se actualiza al instante, pero la petición al servidor sale cuando la persona deja de
 * escribir, no por cada tecla.
 */
export function useValorConRetraso<T>(valor: T, ms = 350): T {
  const [retrasado, setRetrasado] = useState(valor)
  useEffect(() => {
    const temporizador = setTimeout(() => setRetrasado(valor), ms)
    return () => clearTimeout(temporizador)
  }, [valor, ms])
  return retrasado
}
