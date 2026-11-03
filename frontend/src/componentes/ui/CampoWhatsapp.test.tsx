import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'

import { CampoWhatsapp } from './CampoWhatsapp'

function Envoltorio({ inicial = '' }: { inicial?: string }) {
  const [valor, setValor] = useState(inicial)
  return (
    <>
      <CampoWhatsapp value={valor} onChange={setValor} />
      <output>{valor}</output>
    </>
  )
}

describe('CampoWhatsapp', () => {
  it('antepone el indicativo de Colombia por defecto', async () => {
    render(<Envoltorio />)
    await userEvent.type(screen.getByLabelText('WhatsApp'), '300 123 4567')
    expect(screen.getByRole('status')).toHaveTextContent('+573001234567')
  })

  it('usa el país elegido', async () => {
    render(<Envoltorio />)
    await userEvent.selectOptions(screen.getByLabelText('Indicativo del país'), '58')
    await userEvent.type(screen.getByLabelText('WhatsApp'), '4125551234')
    expect(screen.getByRole('status')).toHaveTextContent('+584125551234')
  })

  it('reconoce el país si pegan el número con indicativo', async () => {
    render(<Envoltorio />)
    await userEvent.click(screen.getByLabelText('WhatsApp'))
    await userEvent.paste('+593 99 123 4567')
    expect(screen.getByLabelText('Indicativo del país')).toHaveValue('593')
    expect(screen.getByRole('status')).toHaveTextContent('+593991234567')
  })

  it('reconoce el país aunque escriban el indicativo tecla por tecla', async () => {
    render(<Envoltorio />)
    await userEvent.type(screen.getByLabelText('WhatsApp'), '+593991234567')
    expect(screen.getByLabelText('Indicativo del país')).toHaveValue('593')
    expect(screen.getByRole('status')).toHaveTextContent('+593991234567')
  })

  it('muestra un número guardado separado en país y número', () => {
    render(<Envoltorio inicial="+573001234567" />)
    expect(screen.getByLabelText('Indicativo del país')).toHaveValue('57')
    expect(screen.getByLabelText('WhatsApp')).toHaveValue('3001234567')
  })
})
