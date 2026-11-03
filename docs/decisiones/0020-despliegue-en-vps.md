# ADR 0020: Despliegue en un VPS con Docker Compose y Caddy

- Estado: aceptada
- Contexto: se contrata un VPS (Hostinger KVM 2, Ubuntu 24.04) para publicar Goltime en `goltime.alanpc.tech`. El `docker-compose.yml` actual es de desarrollo: expone la base de datos, la API con Swagger y Mailpit.
- Decisiones:
  1. **Archivo de Compose aparte** (`docker-compose.produccion.yml`) en lugar de un `override`: Compose no permite quitar los `ports` heredados, y así es imposible exponer por accidente la base o la API. Solo Caddy publica los puertos 80 y 443. Usa el nombre de proyecto `goltime`, distinto del de desarrollo, para no mezclar volúmenes.
  2. **Caddy como terminación HTTPS**: pide y renueva el certificado de Let's Encrypt sin configuración, agrega `Strict-Transport-Security` y habla con el Nginx del frontend. Nginx se mantiene tal cual (CSP y cabeceras) y ahora respeta `X-Forwarded-Proto` para que la API sepa que la conexión original era HTTPS.
  3. **Perfil `prod` forzado** (cookies `Secure`, Swagger deshabilitado) y variables obligatorias (`DOMINIO`, secretos, SMTP real) que hacen fallar el arranque si faltan, en vez de caer en valores de desarrollo.
  4. **Administrador inicial por variables de entorno** (`ADMIN_CORREO` y `ADMIN_CLAVE`), como en desarrollo; los siguientes los crea un administrador desde el panel.
  5. **Respaldos con `scripts/respaldo.sh`** (volcado de la base y copia de las fotos, con retención de 14 días) programado con cron. No reemplaza una copia fuera del servidor.
  6. **El dominio raíz queda libre**: la aplicación vive en el subdominio `goltime.`, y `alanpc.tech` queda disponible para un portafolio.
- Fuera de alcance: despliegue continuo (CI/CD), alta disponibilidad y el bot de WhatsApp, que tiene su propia guía y se despliega aparte.
