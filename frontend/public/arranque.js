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
