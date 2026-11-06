import { describe, expect, it } from 'vitest'

import { dimensionesReducidas, optimizarFoto } from './imagenes'

describe('dimensionesReducidas', () => {
  it('no toca lo que ya cabe', () => {
    expect(dimensionesReducidas(1200, 800)).toEqual({ ancho: 1200, alto: 800 })
  })

  it('reduce el lado mayor a 1600 conservando la proporción (horizontal y vertical)', () => {
    expect(dimensionesReducidas(4000, 3000)).toEqual({ ancho: 1600, alto: 1200 })
    expect(dimensionesReducidas(3000, 4000)).toEqual({ ancho: 1200, alto: 1600 })
  })
})

describe('optimizarFoto', () => {
  it('si el navegador no puede procesar la imagen, devuelve el archivo original', async () => {
    const original = new File([new Uint8Array(10)], 'foto.jpg', { type: 'image/jpeg' })
    expect(await optimizarFoto(original)).toBe(original)
  })
})
