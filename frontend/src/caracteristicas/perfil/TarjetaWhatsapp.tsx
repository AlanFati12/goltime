import { useMutation, useQuery } from '@tanstack/react-query'
import { Copy, MessageCircle } from 'lucide-react'
import { useEffect, useState } from 'react'

import { useSesion, type Usuario } from '@/caracteristicas/autenticacion/almacenSesion'
import { Alerta } from '@/componentes/ui/Alerta'
import { avisar } from '@/componentes/ui/almacenAvisos'
import { Boton } from '@/componentes/ui/Boton'
import { Insignia } from '@/componentes/ui/Insignia'
import { Tarjeta, TituloTarjeta } from '@/componentes/ui/Tarjeta'
import { api, mensajeDeError } from '@/lib/api'
import { formatearTelefono } from '@/lib/telefono'

interface CodigoVinculacion {
  codigo: string
  expiraEn: string
  numeroBot?: string
  enlace?: string
}

/** Segundos que faltan para que venza el código (se recalcula cada segundo). */
function useSegundosRestantes(expiraEn: string | undefined) {
  const [ahora, setAhora] = useState(() => Date.now())
  useEffect(() => {
    if (!expiraEn) return
    const intervalo = setInterval(() => setAhora(Date.now()), 1000)
    return () => clearInterval(intervalo)
  }, [expiraEn])
  return expiraEn ? Math.max(0, Math.round((new Date(expiraEn).getTime() - ahora) / 1000)) : 0
}

/**
 * Vincular el WhatsApp con el bot (ADR 0018): el cliente genera aquí un código de un solo uso y se lo envía al bot
 * desde el WhatsApp que quiere vincular. Así se prueba que es dueño de la cuenta y del WhatsApp, sin correos.
 */
export function TarjetaWhatsapp({ usuario }: { usuario: Usuario }) {
  const actualizarUsuario = useSesion((s) => s.actualizarUsuario)
  const [codigo, setCodigo] = useState<CodigoVinculacion | null>(null)
  const [confirmando, setConfirmando] = useState(false)
  const segundos = useSegundosRestantes(codigo?.expiraEn)
  const vencido = codigo !== null && segundos === 0

  const generar = useMutation({
    mutationFn: async () => (await api.post<CodigoVinculacion>('/me/whatsapp/codigo')).data,
    onSuccess: setCodigo,
    onError: (e) => avisar.error(mensajeDeError(e)),
  })
  const desvincular = useMutation({
    mutationFn: async () => (await api.delete<Usuario>('/me/whatsapp')).data,
    onSuccess: (u) => {
      actualizarUsuario(u)
      setConfirmando(false)
      avisar.exito('Desvinculaste tu WhatsApp del bot.')
    },
    onError: (e) => avisar.error(mensajeDeError(e)),
  })

  // Mientras el código está a la vista, se revisa si el bot ya lo recibió.
  useQuery({
    queryKey: ['perfil', 'whatsapp'],
    queryFn: async () => {
      const u = (await api.get<Usuario>('/me')).data
      if (u.whatsappVinculado) {
        actualizarUsuario(u)
        setCodigo(null)
        avisar.exito('¡Listo! El bot de WhatsApp ya te reconoce.')
      }
      return u
    },
    enabled: codigo !== null && !vencido,
    refetchInterval: 4000,
    gcTime: 0,
  })

  const copiar = async () => {
    if (!codigo) return
    try {
      await navigator.clipboard.writeText(codigo.codigo)
      avisar.exito('Código copiado.')
    } catch {
      avisar.error('No se pudo copiar; escríbelo a mano.')
    }
  }

  if (usuario.whatsappVinculado) {
    return (
      <Tarjeta>
        <TituloTarjeta
          titulo="WhatsApp"
          descripcion="El bot de WhatsApp de Goltime te reconoce: puedes reservar y ver tus reservas desde allí."
        />
        <div className="flex flex-wrap items-center gap-3">
          <Insignia tono="verde">Vinculado</Insignia>
          {confirmando ? (
            <>
              <span className="text-texto-suave text-sm">¿Desvincular tu WhatsApp?</span>
              <Boton
                variante="peligro"
                tamano="sm"
                disabled={desvincular.isPending}
                onClick={() => desvincular.mutate()}
              >
                Sí, desvincular
              </Boton>
              <Boton variante="fantasma" tamano="sm" onClick={() => setConfirmando(false)}>
                Cancelar
              </Boton>
            </>
          ) : (
            <Boton variante="secundario" tamano="sm" onClick={() => setConfirmando(true)}>
              Desvincular
            </Boton>
          )}
        </div>
      </Tarjeta>
    )
  }

  const minutos = Math.floor(segundos / 60)
  const resto = String(segundos % 60).padStart(2, '0')

  return (
    <Tarjeta>
      <TituloTarjeta
        titulo="WhatsApp"
        descripcion="Vincula tu WhatsApp para reservar y ver tus reservas desde el bot de Goltime."
      />
      {codigo === null || vencido ? (
        <div className="flex flex-col items-start gap-3">
          {vencido && <Alerta tipo="aviso">El código venció. Genera uno nuevo.</Alerta>}
          <Boton onClick={() => generar.mutate()} disabled={generar.isPending}>
            <MessageCircle aria-hidden />
            {generar.isPending ? 'Generando…' : 'Vincular WhatsApp'}
          </Boton>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          <p className="text-texto-suave text-sm">
            Envíale este código al bot
            {codigo.numeroBot ? (
              <>
                {' '}
                (<strong className="text-texto">{formatearTelefono(codigo.numeroBot)}</strong>)
              </>
            ) : null}{' '}
            desde el WhatsApp que quieres vincular. Vence en{' '}
            <strong className="text-texto tabular-nums">
              {minutos}:{resto}
            </strong>
            .
          </p>
          <p
            className="bg-tenue text-texto rounded-tarjeta py-4 text-center font-mono text-3xl font-bold tracking-[0.4em]"
            aria-label={`Código ${codigo.codigo.split('').join(' ')}`}
          >
            {codigo.codigo}
          </p>
          <div className="flex flex-wrap gap-2">
            {codigo.enlace && (
              <a
                href={codigo.enlace}
                target="_blank"
                rel="noreferrer"
                className="bg-volt text-noche hover:bg-volt-vivo inline-flex h-10 items-center gap-2 rounded-full px-5 text-sm font-semibold"
              >
                <MessageCircle className="size-4" aria-hidden />
                Abrir WhatsApp
              </a>
            )}
            <Boton variante="secundario" onClick={copiar}>
              <Copy aria-hidden />
              Copiar código
            </Boton>
          </div>
          <p className="text-texto-suave text-xs">
            Esta página se actualiza sola cuando el bot reciba el código.
          </p>
        </div>
      )}
    </Tarjeta>
  )
}
