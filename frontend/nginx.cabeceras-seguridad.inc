# Cabeceras de seguridad. nginx descarta las de nivel superior cuando un bloque `location` define sus propios
# add_header, así que cada bloque incluye este archivo para no perderlas.
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "geolocation=(), camera=(), microphone=()" always;
# Google Identity Services (botón «Continuar con Google»): su script, su ventana emergente (frame) y sus estilos.
add_header Content-Security-Policy "default-src 'self'; script-src 'self' https://accounts.google.com/gsi/client; style-src 'self' 'unsafe-inline' https://accounts.google.com/gsi/style; font-src 'self'; img-src 'self' data: blob:; connect-src 'self' https://accounts.google.com/gsi/; frame-src https://accounts.google.com/gsi/; frame-ancestors 'none'; base-uri 'self'" always;
