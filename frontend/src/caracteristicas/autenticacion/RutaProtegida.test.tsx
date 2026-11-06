import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'

import { usuarioDePrueba } from '@/test/usuarios'

import { useSesion } from './almacenSesion'
import { RutaProtegida } from './RutaProtegida'

function montar(rol?: 'ADMIN', permitirIncompleto = false) {
  return render(
    <MemoryRouter initialEntries={['/admin']}>
      <Routes>
        <Route path="/" element={<p>Inicio</p>} />
        <Route path="/ingresar" element={<p>Ingresar</p>} />
        <Route path="/completar-perfil" element={<p>Completa tu perfil</p>} />
        <Route element={<RutaProtegida rol={rol} permitirIncompleto={permitirIncompleto} />}>
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

  it('manda a completar el perfil a un cliente al que le faltan datos de Google', () => {
    useSesion.getState().iniciar('t', usuarioDePrueba({ perfilCompleto: false }))
    montar()
    expect(screen.getByText('Completa tu perfil')).toBeInTheDocument()
  })

  it('en la propia pantalla de completar perfil sí deja pasar al cliente incompleto', () => {
    useSesion.getState().iniciar('t', usuarioDePrueba({ perfilCompleto: false }))
    montar(undefined, true)
    expect(screen.getByText('Panel')).toBeInTheDocument()
  })

  it('un administrador nunca pasa por completar el perfil', () => {
    useSesion.getState().iniciar('t', usuarioDePrueba({ rol: 'ADMIN', perfilCompleto: true }))
    montar('ADMIN')
    expect(screen.getByText('Panel')).toBeInTheDocument()
  })
})
