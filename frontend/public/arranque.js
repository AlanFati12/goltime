// Se carga en <head> antes de pintar (archivo propio: la CSP no permite scripts en línea).
// 1) Con JS disponible, el contenido puede esperar su animación de entrada; sin JS se ve igual.
// 2) Aplica el tema elegido por la persona o, si no eligió, el del sistema, sin destello.
document.documentElement.classList.add('js')
;(function () {
  var tema
  try {
    tema = localStorage.getItem('goltime:tema')
  } catch {
    tema = null
  }
  if (tema !== 'claro' && tema !== 'oscuro') {
    tema = window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches ? 'oscuro' : 'claro'
  }
  document.documentElement.dataset.tema = tema
  if (tema === 'oscuro') {
    var meta = document.querySelector('meta[name="theme-color"]')
    if (meta) meta.setAttribute('content', '#080d18')
  }
})()

// Precarga la foto de la portada ("/"): la dibuja React, así que sin esto el navegador la descubre tarde,
// cuando ya bajó y ejecutó el JavaScript. Los nombres vienen de scripts/optimizar-imagenes.py.
if (location.pathname === '/') {
  var foto = document.createElement('link')
  foto.rel = 'preload'
  foto.as = 'image'
  foto.href = '/img/hero-cancha-nocturna-1280.webp'
  foto.setAttribute(
    'imagesrcset',
    '/img/hero-cancha-nocturna-640.webp 640w, /img/hero-cancha-nocturna-1280.webp 1280w, /img/hero-cancha-nocturna-2000.webp 2000w',
  )
  foto.setAttribute('imagesizes', '100vw')
  document.head.appendChild(foto)
}
