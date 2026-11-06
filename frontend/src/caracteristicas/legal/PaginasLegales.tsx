import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

/** Correo al que se dirigen las solicitudes sobre datos personales. */
const CORREO_CONTACTO = 'alanpimienta554@gmail.com'
const ACTUALIZADO = '6 de noviembre de 2026'

function Documento({
  titulo,
  introduccion,
  children,
}: {
  titulo: string
  introduccion: string
  children: ReactNode
}) {
  return (
    <article className="mx-auto max-w-3xl">
      <p className="text-texto-suave text-sm font-semibold">Actualizado el {ACTUALIZADO}</p>
      <h1 className="mt-2 text-4xl font-extrabold uppercase sm:text-5xl">{titulo}</h1>
      <p className="text-texto-suave mt-5 text-lg">{introduccion}</p>
      <div className="mt-10 flex flex-col gap-9">{children}</div>
      <p className="border-borde text-texto-suave mt-12 border-t pt-6 text-sm">
        Consulta también{' '}
        <Link to="/privacidad" className="text-texto font-semibold underline">
          la política de privacidad
        </Link>{' '}
        y{' '}
        <Link to="/terminos" className="text-texto font-semibold underline">
          los términos de uso
        </Link>
        .
      </p>
    </article>
  )
}

function Seccion({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <section>
      <h2 className="text-2xl font-bold">{titulo}</h2>
      <div className="text-texto-suave mt-3 flex flex-col gap-3 leading-relaxed [&_li]:ml-5 [&_li]:list-disc [&_ul]:flex [&_ul]:flex-col [&_ul]:gap-1.5">
        {children}
      </div>
    </section>
  )
}

export function PaginaPrivacidad() {
  return (
    <Documento
      titulo="Política de privacidad"
      introduccion="Goltime es una aplicación para reservar canchas sintéticas en Neiva, desarrollada como proyecto académico de la Universidad Surcolombiana. Aquí explicamos qué datos tuyos tratamos, para qué y cómo ejercer tus derechos, conforme a la Ley 1581 de 2012 y el Decreto 1377 de 2013 de Colombia."
    >
      <Seccion titulo="Qué datos recogemos">
        <ul>
          <li>
            <strong>De tu cuenta de Google:</strong> tu nombre, tu correo electrónico (confirmado
            por Google) y un identificador interno de Google. No recibimos tu contraseña de Google
            ni accedemos a tu Gmail, contactos, calendario ni a ningún otro servicio de tu cuenta.
          </li>
          <li>
            <strong>Los que tú nos das:</strong> número de WhatsApp o teléfono y, si quieres, tu
            documento de identidad.
          </li>
          <li>
            <strong>De tu uso:</strong> tus reservas, equipos y torneos en los que participas,
            premios y notificaciones.
          </li>
          <li>
            <strong>Técnicos:</strong> la dirección IP y el navegador desde los que inicias sesión,
            para proteger tu cuenta y registrar los cambios sensibles.
          </li>
        </ul>
      </Seccion>

      <Seccion titulo="Para qué los usamos">
        <ul>
          <li>Identificarte y mantener tu sesión iniciada.</li>
          <li>Gestionar tus reservas, avisarte de su estado y contactarte si hace falta.</li>
          <li>Organizar torneos y calcular los premios por tus partidos reservados.</li>
          <li>Atenderte por WhatsApp si usas el asistente de reservas.</li>
          <li>Proteger el servicio contra fraudes y accesos indebidos.</li>
        </ul>
        <p>No vendemos ni cedemos tus datos, ni los usamos para publicidad de terceros.</p>
      </Seccion>

      <Seccion titulo="Con quién se comparten">
        <p>Solo con los proveedores sin los que el servicio no funciona:</p>
        <ul>
          <li>
            <strong>Google</strong>, para iniciar sesión.
          </li>
          <li>
            <strong>Hostinger</strong>, donde está alojado el servidor.
          </li>
          <li>
            <strong>WhatsApp (Meta) y OpenAI</strong>, únicamente si le escribes al asistente por
            WhatsApp: el texto de tus mensajes se procesa con un modelo de lenguaje para
            responderte. Evita enviarle datos sensibles.
          </li>
        </ul>
        <p>
          Algunos de estos proveedores están fuera de Colombia, por lo que tus datos pueden
          transferirse al exterior para prestar el servicio.
        </p>
      </Seccion>

      <Seccion titulo="Cookies">
        <p>
          Usamos una sola cookie, necesaria y de sesión (<em>HttpOnly</em>), para mantenerte dentro
          de la aplicación. No usamos cookies de publicidad ni de seguimiento.
        </p>
      </Seccion>

      <Seccion titulo="Cuánto tiempo los guardamos">
        <p>
          Mientras tu cuenta esté activa y el tiempo necesario para cumplir obligaciones legales o
          resolver reclamaciones sobre reservas y pagos. Si pides eliminar tus datos, los borramos o
          anonimizamos, salvo lo que debamos conservar por ley.
        </p>
      </Seccion>

      <Seccion titulo="Tus derechos">
        <p>Como titular puedes, en cualquier momento:</p>
        <ul>
          <li>Conocer, actualizar y rectificar tus datos (también desde «Mi perfil»).</li>
          <li>Pedir prueba de la autorización que nos diste.</li>
          <li>Saber cómo usamos tus datos.</li>
          <li>Revocar tu autorización y pedir que suprimamos tus datos.</li>
          <li>
            Presentar una queja ante la Superintendencia de Industria y Comercio (SIC) si crees que
            se vulneraron tus derechos.
          </li>
        </ul>
        <p>
          Escríbenos a{' '}
          <a href={`mailto:${CORREO_CONTACTO}`} className="text-texto font-semibold underline">
            {CORREO_CONTACTO}
          </a>{' '}
          desde el correo de tu cuenta. Respondemos en un máximo de 10 días hábiles para consultas y
          15 para reclamos.
        </p>
      </Seccion>

      <Seccion titulo="Seguridad">
        <p>
          La conexión va cifrada (HTTPS), no guardamos contraseñas porque el acceso es solo con
          Google y limitamos quién puede ver los datos de los clientes. Ningún sistema es infalible:
          si ocurriera un incidente que te afecte, te lo informaremos.
        </p>
      </Seccion>

      <Seccion titulo="Cambios a esta política">
        <p>
          Si la modificamos de forma importante, lo avisaremos en la aplicación. La fecha de arriba
          indica la última versión.
        </p>
      </Seccion>
    </Documento>
  )
}

export function PaginaTerminos() {
  return (
    <Documento
      titulo="Términos de uso"
      introduccion="Al crear una cuenta o usar Goltime aceptas estas condiciones. Son cortas a propósito; léelas antes de reservar."
    >
      <Seccion titulo="Qué es Goltime">
        <p>
          Una plataforma para consultar canchas sintéticas en Neiva, solicitar reservas, inscribirte
          en torneos y acumular premios. Es un proyecto académico de la Universidad Surcolombiana
          (INNOVASOFT IX) y se ofrece tal como está.
        </p>
      </Seccion>

      <Seccion titulo="Tu cuenta">
        <ul>
          <li>Se crea y se accede únicamente con tu cuenta de Google.</li>
          <li>Debes dar datos verdaderos y mantenerlos al día, en especial tu WhatsApp.</li>
          <li>Eres responsable de lo que se haga desde tu cuenta; no la compartas.</li>
          <li>
            Podemos suspender cuentas que se usen para abusar del servicio o perjudicar a otros.
          </li>
        </ul>
      </Seccion>

      <Seccion titulo="Reservas">
        <ul>
          <li>
            Al reservar envías una solicitud. Queda <strong>pendiente</strong> y retiene el horario
            hasta que la administración la confirme o venza el plazo indicado.
          </li>
          <li>El precio lo calcula el sistema y se te muestra antes de solicitar.</li>
          <li>
            Puedes cancelar o reprogramar según la política vigente de la sede; verás las
            condiciones antes de confirmar el cambio.
          </li>
          <li>
            Si no te presentas o incumples repetidamente, la administración puede limitar nuevas
            reservas.
          </li>
          <li>El pago de la cancha se acuerda directamente con la sede.</li>
        </ul>
      </Seccion>

      <Seccion titulo="Torneos y premios">
        <p>
          Cada torneo tiene su reglamento. Los premios por reservas solo cuentan sobre partidos
          completados y las condiciones pueden cambiar con aviso previo.
        </p>
      </Seccion>

      <Seccion titulo="Uso aceptable">
        <p>
          No intentes acceder a datos de otras personas, saturar el servicio ni usarlo para fines
          ilícitos. Si usas el asistente de WhatsApp, hazlo con respeto y solo para temas de
          Goltime.
        </p>
      </Seccion>

      <Seccion titulo="Responsabilidad">
        <p>
          Hacemos lo posible por que el servicio esté disponible y los datos sean correctos, pero no
          garantizamos que no haya interrupciones ni errores. Las respuestas del asistente de
          WhatsApp pueden equivocarse: la reserva solo es válida cuando aparece en tu cuenta.
        </p>
      </Seccion>

      <Seccion titulo="Tus datos">
        <p>
          El tratamiento de tus datos personales se rige por la{' '}
          <Link to="/privacidad" className="text-texto font-semibold underline">
            política de privacidad
          </Link>
          .
        </p>
      </Seccion>

      <Seccion titulo="Cambios y contacto">
        <p>
          Podemos actualizar estos términos y lo avisaremos en la aplicación. Estas condiciones se
          rigen por las leyes de Colombia. Dudas:{' '}
          <a href={`mailto:${CORREO_CONTACTO}`} className="text-texto font-semibold underline">
            {CORREO_CONTACTO}
          </a>
          .
        </p>
      </Seccion>
    </Documento>
  )
}
