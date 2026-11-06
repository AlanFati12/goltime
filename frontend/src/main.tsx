import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import App from './App'
import './estilos/index.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

/**
 * Retira la pantalla de arranque de index.html cuando la aplicación ya pintó. Se deja visible
 * un mínimo muy breve (400 ms) para que el logo alcance a dibujarse y la salida no parezca un parpadeo.
 */
const arranque = document.getElementById('arranque')
if (arranque) {
  const transcurrido = performance.now()
  window.setTimeout(
    () => {
      arranque.classList.add('salida')
      window.setTimeout(() => arranque.remove(), 350)
    },
    Math.max(0, 400 - transcurrido),
  )
}
