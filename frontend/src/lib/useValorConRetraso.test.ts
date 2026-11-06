import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useValorConRetraso } from './useValorConRetraso'

describe('useValorConRetraso', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => vi.useRealTimers())

  it('conserva el valor anterior hasta que pasa el retraso sin cambios', () => {
    const { result, rerender } = renderHook(({ v }) => useValorConRetraso(v, 300), {
      initialProps: { v: 'a' },
    })
    rerender({ v: 'ab' })
    rerender({ v: 'abc' })
    expect(result.current).toBe('a')
    act(() => void vi.advanceTimersByTime(299))
    expect(result.current).toBe('a')
    act(() => void vi.advanceTimersByTime(1))
    expect(result.current).toBe('abc')
  })

  it('cada tecla reinicia la espera', () => {
    const { result, rerender } = renderHook(({ v }) => useValorConRetraso(v, 300), {
      initialProps: { v: '' },
    })
    rerender({ v: 'x' })
    act(() => void vi.advanceTimersByTime(200))
    rerender({ v: 'xy' })
    act(() => void vi.advanceTimersByTime(200))
    expect(result.current).toBe('')
    act(() => void vi.advanceTimersByTime(100))
    expect(result.current).toBe('xy')
  })
})
