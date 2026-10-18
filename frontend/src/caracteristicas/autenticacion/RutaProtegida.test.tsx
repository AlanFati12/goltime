import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'

import { usuarioDePrueba } from '@/test/usuarios'

import { useSesion } from './almacenSesion'
import { RutaProtegida } from './RutaProtegida'

function montar(rol?: 'ADMIN') {
  return render(
    <MemoryRouter initialEntries={['/admin']}>
      <Routes>
        <Route path="/" element={<p>Inicio</p>} />
        <Route path="/ingresar" element={<p>Ingresar</p>} />
        <Route element={<RutaProtegida rol={rol} />}>
          <Route path="/admin" element={<p>Panel</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('RutaProtegida', () => {
  beforeEach(() => useSesion.getState().cerrar())

  it('manda al login si no hay sesión', () => {
    montar('ADMIN')
    expect(screen.getByText('Ingresar')).toBeInTheDocument()
  })

  it('manda al inicio si el rol no coincide', () => {
    useSesion.getState().iniciar('t', usuarioDePrueba({ rol: 'CLIENTE' }))
    montar('ADMIN')
    expect(screen.getByText('Inicio')).toBeInTheDocument()
  })

  it('deja pasar si el rol coincide', () => {
    useSesion.getState().iniciar('t', usuarioDePrueba({ rol: 'ADMIN' }))
    montar('ADMIN')
    expect(screen.getByText('Panel')).toBeInTheDocument()
  })
})
