# Despliegue de Goltime en el VPS

Guía para publicar Goltime en un VPS Ubuntu 24.04 (Hostinger KVM 2) con HTTPS. Decisiones en el [ADR 0020](decisiones/0020-despliegue-en-vps.md).

Valores de este despliegue (cámbialos si cambian):

| Dato | Valor |
|---|---|
| Dominio de la app | `goltime.alanpc.tech` |
| IP del VPS | `179.66.130.88` |
| Usuario de trabajo en el servidor | `goltime` (se crea en el paso 2) |

Arquitectura: `Internet → Caddy (80/443, HTTPS) → Nginx (web) → API → PostgreSQL`. Solo Caddy abre puertos.

## 1. Apuntar el dominio al servidor (DNS)

En hPanel: **Dominios → alanpc.tech → DNS / Servidores de nombres** y agrega un registro:

| Tipo | Nombre | Apunta a | TTL |
|---|---|---|---|
| A | `goltime` | `179.66.130.88` | 300 (o el mínimo que permita) |

Si ya existe un registro `A`, `AAAA` o `CNAME` llamado `goltime`, bórralo o edítalo: no pueden convivir. Comprueba la propagación desde tu computador (puede tardar de minutos a unas horas):

```bash
dig +short goltime.alanpc.tech
```

Debe responder `179.66.130.88`. No sigas al paso 7 hasta que lo haga: Caddy necesita que el dominio ya apunte al servidor para obtener el certificado.

## 2. Primer ingreso y usuario de trabajo

Desde tu computador (con tu llave SSH registrada en hPanel → VPS → Claves SSH):

```bash
ssh root@179.66.130.88
```

Dentro del servidor:

```bash
apt update && apt upgrade -y
adduser goltime                      # te pide una contraseña; guárdala
usermod -aG sudo goltime
mkdir -p /home/goltime/.ssh
cp ~/.ssh/authorized_keys /home/goltime/.ssh/ 2>/dev/null || true
chown -R goltime:goltime /home/goltime/.ssh
chmod 700 /home/goltime/.ssh && chmod 600 /home/goltime/.ssh/authorized_keys 2>/dev/null || true
```

Abre **otra terminal** y comprueba que entras como el usuario nuevo antes de seguir:

```bash
ssh goltime@179.66.130.88
```

## 3. Firewall

Como `goltime` (con `sudo`):

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 443/udp
sudo ufw enable
sudo ufw status
```

Nada más debe quedar abierto: ni 5432 (base de datos) ni 8080 (API).

## 4. Instalar Docker

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker goltime
exit            # cierra y vuelve a entrar para que el grupo surta efecto
```

Comprueba: `docker compose version`.

## 5. Llevar el código al servidor

El repositorio todavía no tiene remoto. Desde la carpeta del proyecto **en tu Mac**:

```bash
COPYFILE_DISABLE=1 tar czf - --exclude=node_modules --exclude=target --exclude=.env --exclude=.git --exclude=uploads --exclude='*.log' --exclude=.DS_Store --exclude=dist . \
  | ssh goltime@179.66.130.88 'mkdir -p ~/goltime && tar xzf - -C ~/goltime'
```

(Se usa `tar` por `ssh` porque el `rsync` de macOS no es compatible con el del servidor. Los avisos «Ignoring unknown extended header keyword» son inofensivos.)

(Cuando subas el proyecto a un repositorio privado, esto se reemplaza por `git clone` y `git pull`.)

## 6. Configurar el `.env` del servidor

```bash
ssh goltime@179.66.130.88
cd ~/goltime
cp .env.produccion.example .env
nano .env
```

Genera los secretos en el servidor y pégalos en el archivo:

```bash
openssl rand -base64 24     # DB_CLAVE
openssl rand -base64 48     # JWT_SECRETO
```

Qué completar:

- `DOMINIO=goltime.alanpc.tech`
- `DB_CLAVE` y `JWT_SECRETO` con los valores generados.
- `GOOGLE_CLIENT_ID`: el ID de cliente OAuth de Google (paso previo en [google-oauth.md](google-oauth.md)). Sin él la API no arranca.
- `ADMIN_CORREO`: el correo de la **cuenta de Google** del primer administrador. No hay contraseña: entra con esa cuenta.
- Correo (`SMTP_*` y `CORREO_REMITENTE`): ver el paso 9. Ya no se usa para entrar (Google confirma el correo), solo para los avisos de reservas. Mientras no lo tengas, deja valores provisionales; la app arranca igual, pero esos correos no saldrán.

Protege el archivo: `chmod 600 .env`.

## 7. Levantar todo

```bash
cd ~/goltime
docker compose -f docker-compose.produccion.yml up -d --build
```

La primera vez compila el backend y el frontend (varios minutos). Después:

```bash
docker compose -f docker-compose.produccion.yml ps          # todos "running"; api y db "healthy"
docker compose -f docker-compose.produccion.yml logs -f caddy   # busca "certificate obtained"
docker compose -f docker-compose.produccion.yml logs api | grep -i "Administrador inicial"
```

Abre `https://goltime.alanpc.tech/ingresar` y pulsa «Continuar con Google» con la cuenta de `ADMIN_CORREO`: entras al panel de administración.

## 8. Datos de demostración

No existen en producción: los clientes de demostración solo se pueden usar con el ingreso de prueba, que está prohibido fuera de desarrollo. Los datos reales los crea el administrador desde el panel.

## 9. Correo real

Mailpit no existe en producción. Opciones gratuitas: Brevo, Resend, SendGrid. Con cualquiera:

1. Crea la cuenta y verifica el dominio `alanpc.tech` (te piden agregar registros DNS: SPF, DKIM y a veces DMARC, en el mismo panel del paso 1).
2. Copia el servidor SMTP, el puerto (587), el usuario y la clave a `SMTP_HOST`, `SMTP_PUERTO`, `SMTP_USUARIO` y `SMTP_CLAVE`.
3. `CORREO_REMITENTE` debe ser una dirección del dominio verificado, por ejemplo `no-responder@alanpc.tech`.
4. Reinicia la API: `docker compose -f docker-compose.produccion.yml up -d api`.
5. Entra como cliente con otra cuenta de Google, completa el perfil, haz una reserva y comprueba que llega el correo de confirmación cuando la apruebes.

## 10. Respaldos

```bash
chmod +x ~/goltime/scripts/respaldo.sh
~/goltime/scripts/respaldo.sh            # prueba manual: crea ~/respaldos/db-*.sql.gz y fotos-*.tar.gz
crontab -e                               # agrega la línea:
# 30 3 * * * $HOME/goltime/scripts/respaldo.sh >> $HOME/respaldos/respaldo.log 2>&1
```

Guarda copias **fuera del servidor** de vez en cuando (desde tu Mac):

```bash
rsync -avz goltime@179.66.130.88:~/respaldos/ ~/respaldos-goltime/
```

Restaurar la base (⚠ reemplaza los datos actuales):

```bash
gunzip -c ~/respaldos/db-AAAAMMDD-HHMM.sql.gz | docker compose -f docker-compose.produccion.yml exec -T db sh -c 'psql -U "$POSTGRES_USER" "$POSTGRES_DB"'
```

## 11. Endurecer el acceso SSH

Solo cuando ya entres como `goltime` **con tu llave** (sin contraseña):

```bash
sudo nano /etc/ssh/sshd_config.d/99-endurecer.conf
```

```
PasswordAuthentication no
PermitRootLogin no
```

```bash
sudo systemctl reload ssh
```

Mantén una terminal abierta mientras pruebas entrar en otra, para no quedarte fuera.

## 12. Actualizar la aplicación

Desde tu Mac, repite el `tar` del paso 5 y en el servidor:

```bash
cd ~/goltime
docker compose -f docker-compose.produccion.yml up -d --build
```

Las migraciones de Flyway se aplican solas al arrancar la API.

## 13. Lista de verificación

- [ ] `https://goltime.alanpc.tech` abre con candado y `http://` redirige a `https://`.
- [ ] Se puede ingresar como administrador y como cliente.
- [ ] `curl -I https://goltime.alanpc.tech` muestra `strict-transport-security`.
- [ ] `https://goltime.alanpc.tech/swagger-ui.html` **no** muestra Swagger (ves la página normal de la app).
- [ ] Desde fuera, los puertos 5432 y 8080 no responden: `nc -zv 179.66.130.88 5432`.
- [ ] Se puede entrar con Google como administrador y como cliente (el cliente nuevo ve «Completa tu perfil»).
- [ ] Llega el correo de reserva confirmada.
- [ ] Existe al menos un respaldo en `~/respaldos`.

## Problemas frecuentes

- **Caddy no obtiene certificado**: el DNS aún no apunta al servidor, o el puerto 80 está bloqueado (firewall del servidor o de hPanel). Revisa `logs caddy`; reintenta solo cuando se corrige.
- **La API no arranca y dice "Define … en .env"**: falta una variable obligatoria del paso 6.
- **El panel carga pero el ingreso falla**: revisa que `DOMINIO` sea exactamente el que usas en el navegador (afecta CORS y la cookie).
- **No llegan los correos**: revisa `logs api`, las credenciales SMTP y que el remitente pertenezca al dominio verificado.
- **Memoria agotada al compilar**: improbable con 8 GB; si pasa, `docker compose … build api` por separado.
