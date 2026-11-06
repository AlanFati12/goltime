/** Lado mayor máximo de una foto subida: sobra para verla a pantalla completa y pesa una fracción. */
export const LADO_MAXIMO_FOTO = 1600
const PESO_QUE_YA_NO_SE_TOCA = 400 * 1024

/** Medidas de una imagen reducida para que su lado mayor no pase de `maximo`, conservando la proporción. */
export function dimensionesReducidas(ancho: number, alto: number, maximo = LADO_MAXIMO_FOTO) {
  const mayor = Math.max(ancho, alto)
  if (mayor <= maximo) return { ancho, alto }
  const factor = maximo / mayor
  return { ancho: Math.round(ancho * factor), alto: Math.round(alto * factor) }
}

function aBlob(lienzo: HTMLCanvasElement, tipo: string, calidad: number) {
  return new Promise<Blob | null>((resolver) => lienzo.toBlob(resolver, tipo, calidad))
}

/**
 * Prepara una foto antes de subirla: la reduce a 1600 px y la recomprime en WebP (o JPEG si el navegador no sabe
 * escribir WebP). Una foto de celular de 4 MB baja a unos 200 KB: sube más rápido y las páginas que la muestran
 * cargan mucho antes. Hacerlo en el navegador es lo seguro: él ya aplica la orientación (EXIF) de la foto, algo
 * que el servidor perdería al recomprimir. Si algo falla, o si el resultado no pesa menos, se sube el original.
 */
export async function optimizarFoto(archivo: File): Promise<File> {
  try {
    const imagen = await createImageBitmap(archivo)
    const { ancho, alto } = dimensionesReducidas(imagen.width, imagen.height)
    if (
      ancho === imagen.width &&
      alto === imagen.height &&
      archivo.size <= PESO_QUE_YA_NO_SE_TOCA
    ) {
      imagen.close()
      return archivo
    }
    const lienzo = document.createElement('canvas')
    lienzo.width = ancho
    lienzo.height = alto
    const contexto = lienzo.getContext('2d')
    if (!contexto) return archivo
    contexto.fillStyle = '#fff'
    contexto.fillRect(0, 0, ancho, alto)
    contexto.drawImage(imagen, 0, 0, ancho, alto)
    imagen.close()
    let resultado = await aBlob(lienzo, 'image/webp', 0.82)
    if (resultado?.type !== 'image/webp') resultado = await aBlob(lienzo, 'image/jpeg', 0.85)
    if (!resultado || resultado.size >= archivo.size) return archivo
    const extension = resultado.type === 'image/webp' ? 'webp' : 'jpg'
    return new File([resultado], `${archivo.name.replace(/\.[^.]+$/, '')}.${extension}`, {
      type: resultado.type,
    })
  } catch {
    return archivo
  }
}
