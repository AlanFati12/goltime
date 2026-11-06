# Ingreso con Google: qué crear en Google y cómo configurarlo

Goltime solo permite entrar con Google (ADR 0023). Para que funcione hace falta un **ID de cliente de OAuth**, que se crea gratis en Google Cloud. No hay secreto que guardar: el ID de cliente es público y es lo único que necesita la aplicación (`GOOGLE_CLIENT_ID`).

## 1. Crear el proyecto y la pantalla de consentimiento

1. Entra a <https://console.cloud.google.com> con la cuenta de Google que será la dueña del proyecto (la tuya, no es la de ningún cliente).
2. Arriba, en el selector de proyectos, **Proyecto nuevo** → nombre `Goltime` → Crear.
3. Menú **APIs y servicios → Pantalla de consentimiento de OAuth** (en versiones nuevas: **Google Auth Platform → Branding / Marca**).
4. Tipo de usuario: **Externo**.
5. Completa:
   - **Nombre de la aplicación**: `Goltime` (es lo que ve la persona al elegir su cuenta).
   - **Correo de asistencia** y **correo de contacto del desarrollador**: el tuyo.
   - **Dominio de la aplicación** (opcional pero recomendado): página principal `https://goltime.alanpc.tech`, y los enlaces a la política de privacidad y a los términos (ver «Pendientes» abajo).
   - **Dominios autorizados**: `alanpc.tech`.
6. **Permisos (scopes)**: agrega solo los básicos `openid`, `.../auth/userinfo.email` y `.../auth/userinfo.profile`. Son «no sensibles»: no hace falta ninguna verificación de Google.
7. **Estado de publicación**: pulsa **Publicar aplicación** (pasar de «Pruebas» a «En producción»). Si se deja en Pruebas, solo pueden entrar las cuentas que agregues a mano como «usuarios de prueba» (máximo 100) y el permiso caduca a los 7 días.

## 2. Crear el ID de cliente

1. **APIs y servicios → Credenciales → Crear credenciales → ID de cliente de OAuth**.
2. Tipo de aplicación: **Aplicación web**. Nombre: `Goltime web`.
3. **Orígenes de JavaScript autorizados** (aquí va solo el origen: esquema + dominio, sin ruta ni barra final):
   - `https://goltime.alanpc.tech`
   - `http://localhost:8081` (probar la app local en Docker)
   - `http://localhost:5173` (probar con el servidor de desarrollo de Vite)
4. **URIs de redireccionamiento autorizados**: déjalo vacío. El botón de Google funciona con una ventana emergente y no usa redirecciones.
5. Crear. Copia el **ID de cliente** (termina en `.apps.googleusercontent.com`). El «secreto» que muestra Google **no se usa**.

Los cambios de orígenes pueden tardar unos minutos (a veces hasta una hora) en aplicarse.

## 3. Ponerlo en la aplicación

- **Local**: en `.env`, `GOOGLE_CLIENT_ID=<el ID>` y, si quieres seguir entrando sin Google mientras desarrollas, `GOOGLE_MODO_PRUEBA=true` (con ambos, aparece el botón de Google y además el ingreso de prueba). Luego `docker compose up -d --build`.
- **Servidor**: en el `.env` del VPS, `GOOGLE_CLIENT_ID=<el ID>` y `ADMIN_CORREO=<correo de la cuenta de Google del administrador>`. En producción `GOOGLE_MODO_PRUEBA` no existe (y si se activara, la API se niega a arrancar).

## 4. Cómo funciona para quien entra

1. Pulsa «Continuar con Google» y elige su cuenta.
2. Google entrega a la página un token firmado; Goltime lo verifica en el servidor (firma, que sea para esta aplicación, que no haya vencido y que Google confirme el correo).
3. Si es la primera vez se crea su cuenta de cliente y ve **«Completa tu perfil»**: apellidos (si Google no los dio), WhatsApp (obligatorio) y documento (opcional). Sin eso no puede reservar.
4. El administrador se autoriza **por correo**: el primero con `ADMIN_CORREO`, los demás desde el panel (Sistema → Administradores). Al entrar con la cuenta de Google de ese correo quedan vinculados como administradores.

## Pendientes recomendados antes de abrirlo al público

- **Política de privacidad y términos de uso**: Google pide los enlaces en la pantalla de consentimiento, y la ley colombiana de datos personales (Ley 1581 de 2012) exige informar el tratamiento. La pantalla de ingreso dice «al continuar aceptas los términos…» pero todavía no hay páginas a las que enlazar. Hay que redactarlas (idealmente revisadas por quien corresponda de la universidad o un abogado).
- **Cuenta de Google propietaria del proyecto**: si esa cuenta se pierde, se pierde el control del ID de cliente. Conviene agregar un segundo propietario en IAM.
- **Quien pierda su cuenta de Google pierde el acceso a Goltime**: no hay contraseña de respaldo. El administrador puede ayudar con las reservas, pero no restablecer nada.
