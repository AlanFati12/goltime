import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import path from 'node:path'
import type { Plugin } from 'vite'
import { defineConfig } from 'vitest/config'

/**
 * Precarga las dos fuentes con las que se pinta el primer texto (cuerpo y títulos). Vite no lo hace solo
 * para las fuentes que llegan desde CSS, y así el navegador las pide a la vez que el CSS en vez de después.
 */
function precargarFuentes(): Plugin {
  const criticas = [/barlow-latin-400-normal.*\.woff2$/, /barlow-condensed-latin-800-normal.*\.woff2$/]
  return {
    name: 'goltime-precargar-fuentes',
    apply: 'build',
    transformIndexHtml(_html, ctx) {
      const archivos = Object.keys(ctx.bundle ?? {}).filter((f) => criticas.some((r) => r.test(f)))
      return archivos.map((f) => ({
        tag: 'link',
        attrs: { rel: 'preload', as: 'font', type: 'font/woff2', href: '/' + f, crossorigin: '' },
        injectTo: 'head' as const,
      }))
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss(), precargarFuentes()],
  resolve: {
    alias: { '@': path.resolve(import.meta.dirname, 'src') },
  },
  server: {
    port: 5173,
    // En desarrollo el front y la API comparten origen mediante este proxy (igual que Nginx en producción).
    proxy: {
      '/api': { target: process.env.VITE_API_PROXY ?? 'http://localhost:8080', changeOrigin: false },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
})
