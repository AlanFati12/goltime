package co.edu.usco.goltime.autenticacion.aplicacion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.autenticacion.dominio.VerificacionTelefono;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.LidWhatsapp;
import co.edu.usco.goltime.compartido.dominio.NumeroTelefono;
import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo.MensajeCorreo;
import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo;
import co.edu.usco.goltime.usuarios.aplicacion.RepositorioUsuarios;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/**
 * Identifica a quien escribe desde un número de WhatsApp y vincula ese número a su cuenta (ADR 0018).
 * Como cualquiera puede escribir cualquier número al registrarse, el número solo cuenta como de la
 * cuenta cuando quien escribe desde él demuestra, con un código enviado al correo, que también es su dueño.
 */
@Service
public class ServicioVerificacionTelefono {

    static final int MAX_CODIGOS_POR_HORA = 3;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final RepositorioUsuarios usuarios;
    private final RepositorioVerificacionesTelefono verificaciones;
    private final EnviadorCorreo correo;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;

    public ServicioVerificacionTelefono(RepositorioUsuarios usuarios, RepositorioVerificacionesTelefono verificaciones,
            EnviadorCorreo correo, RegistroAuditoria auditoria, Clock reloj) {
        this.usuarios = usuarios;
        this.verificaciones = verificaciones;
        this.correo = correo;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    public enum Situacion {
        /** El número está verificado en una cuenta de cliente activa: el bot actúa como ese cliente. */
        CLIENTE,
        /** Hay cuentas con ese número, pero ninguna lo ha verificado: el bot ofrece enviar el código. */
        SIN_VERIFICAR,
        /** Ninguna cuenta tiene ese número: el bot lo atiende como cliente sin cuenta. */
        SIN_CUENTA,
        /** El número es de un cliente desactivado: no puede usar el bot (regla 7). */
        DESACTIVADO
    }

    /**
     * Quién escribe. {@code usuarioId} y {@code nombres} solo vienen con {@link Situacion#CLIENTE}. Si WhatsApp no
     * compartió el teléfono, {@code lid} trae el identificador anónimo y {@code telefono} el de la cuenta (si la hay).
     */
    public record Identificacion(Situacion situacion, String telefono, Long usuarioId, String nombres, String lid) {

        Identificacion(Situacion situacion, String telefono, Long usuarioId, String nombres) {
            this(situacion, telefono, usuarioId, nombres, null);
        }
    }

    /** Correos (enmascarados) a los que se envió un código. */
    public record CodigoEnviado(List<String> correos) {
    }

    @Transactional(readOnly = true)
    public Identificacion identificar(String numero) {
        String telefono = NumeroTelefono.de(numero).valor();
        List<Usuario> cuentas = clientesCon(telefono);
        Optional<Usuario> verificado = cuentas.stream().filter(Usuario::telefonoVerificado).findFirst();
        if (verificado.isPresent()) {
            Usuario u = verificado.get();
            return u.activo() ? new Identificacion(Situacion.CLIENTE, telefono, u.id(), u.nombres())
                    : new Identificacion(Situacion.DESACTIVADO, telefono, null, null);
        }
        if (cuentas.stream().anyMatch(Usuario::activo)) {
            return new Identificacion(Situacion.SIN_VERIFICAR, telefono, null, null);
        }
        return cuentas.isEmpty() ? new Identificacion(Situacion.SIN_CUENTA, telefono, null, null)
                : new Identificacion(Situacion.DESACTIVADO, telefono, null, null);
    }

    /**
     * Quien escribe desde un @lid. Si lo vinculó a su cuenta es ese cliente; si no, no se sabe si tiene cuenta y se
     * atiende como sin cuenta (el bot le ofrece vincularla con el correo).
     */
    @Transactional(readOnly = true)
    public Identificacion identificarPorLid(String lidTexto) {
        String lid = LidWhatsapp.de(lidTexto).valor();
        return usuarios.buscarPorWhatsappLid(lid).filter(u -> u.rol() == Rol.CLIENTE)
                .map(u -> u.activo() ? new Identificacion(Situacion.CLIENTE, u.telefono(), u.id(), u.nombres(), lid)
                        : new Identificacion(Situacion.DESACTIVADO, null, null, null, lid))
                .orElseGet(() -> new Identificacion(Situacion.SIN_CUENTA, null, null, null, lid));
    }

    /**
     * Desde un chat sin teléfono visible: envía un código al correo de la cuenta. La respuesta es la misma exista o
     * no la cuenta, para que el bot no sirva para averiguar qué correos están registrados.
     */
    @Transactional
    public CodigoEnviado solicitarCodigoDesdeLid(String lidTexto, String correoCuenta) {
        String lid = LidWhatsapp.de(lidTexto).valor();
        if (correoCuenta == null || !correoCuenta.contains("@")) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Escribe el correo con el que te registraste en Goltime.");
        }
        Instant ahora = reloj.instant();
        Instant haceUnaHora = ahora.minus(Duration.ofHours(1));
        if (verificaciones.contarCreadasDesdeLid(lid, haceUnaHora) >= MAX_CODIGOS_POR_HORA) {
            throw new ExcepcionNegocio(CodigoError.DEMASIADOS_INTENTOS, "Ya pedimos varios códigos desde este WhatsApp. Intenta de nuevo en una hora.");
        }
        Optional<Usuario> cuenta = usuarios.buscarPorCorreo(correoCuenta.trim()).filter(u -> u.rol() == Rol.CLIENTE && u.activo());
        Optional<Usuario> yaVinculada = usuarios.buscarPorWhatsappLid(lid);
        if (yaVinculada.isPresent() && cuenta.map(c -> !c.id().equals(yaVinculada.get().id())).orElse(true)) {
            throw new ExcepcionNegocio(CodigoError.TELEFONO_YA_REGISTRADO, "Este WhatsApp ya está vinculado a otra cuenta.");
        }
        // No llenar la bandeja de alguien pidiendo códigos desde varios chats.
        if (cuenta.isPresent() && verificaciones.contarCreadasDesdeUsuario(cuenta.get().id(), haceUnaHora) < MAX_CODIGOS_POR_HORA) {
            Usuario u = cuenta.get();
            String codigo = "%06d".formatted(ALEATORIO.nextInt(1_000_000));
            verificaciones.anularPendientesDeLid(u.id(), lid, ahora);
            verificaciones.guardar(VerificacionTelefono.nuevaDesdeLid(u.id(), lid, hash(u.id(), "lid:" + lid, codigo), ahora));
            correo.enviar(new MensajeCorreo(u.correo(), "Tu código para vincular WhatsApp en Goltime", "verificar-telefono",
                    Map.of("nombre", u.nombres(), "codigo", codigo, "numero", "", "minutos", VerificacionTelefono.VIGENCIA.toMinutes())));
        }
        return new CodigoEnviado(List.of(enmascararCorreo(Usuario.normalizarCorreo(correoCuenta))));
    }

    @Transactional(noRollbackFor = ExcepcionNegocio.class)
    public Usuario confirmarCodigoDesdeLid(String lidTexto, String codigo) {
        String lid = LidWhatsapp.de(lidTexto).valor();
        Instant ahora = reloj.instant();
        String limpio = codigo == null ? "" : codigo.replaceAll("\\s", "");
        List<VerificacionTelefono> vigentes = verificaciones.pendientesDeLid(lid).stream().filter(v -> v.estaVigente(ahora)).toList();
        for (VerificacionTelefono v : vigentes) {
            if (MessageDigest.isEqual(v.codigoHash().getBytes(StandardCharsets.UTF_8),
                    hash(v.usuarioId(), "lid:" + lid, limpio).getBytes(StandardCharsets.UTF_8))) {
                Usuario usuario = usuarios.buscarPorId(v.usuarioId()).orElseThrow();
                if (!usuario.activo()) {
                    throw new ExcepcionNegocio(CodigoError.TOKEN_INVALIDO, "El código no es correcto o ya venció.");
                }
                usuario.vincularWhatsappLid(lid);
                Usuario guardado = usuarios.guardar(usuario);
                v.marcarUsada(ahora);
                verificaciones.guardar(v);
                auditoria.registrar("VINCULAR_WHATSAPP", "usuarios", guardado.id(), Map.of("whatsappLid", lid, "canal", "WHATSAPP"), null);
                return guardado;
            }
        }
        for (VerificacionTelefono v : vigentes) {
            v.registrarIntentoFallido();
            verificaciones.guardar(v);
        }
        throw new ExcepcionNegocio(CodigoError.TOKEN_INVALIDO, "El código no es correcto o ya venció.");
    }

    /**
     * Envía un código al correo de cada cuenta activa que escribió ese número sin verificarlo (casi siempre
     * una). Cada cuenta recibe un código distinto: el que se escriba de vuelta decide qué cuenta queda vinculada.
     */
    @Transactional
    public CodigoEnviado solicitarCodigo(String numero) {
        String telefono = NumeroTelefono.de(numero).valor();
        Instant ahora = reloj.instant();
        List<Usuario> cuentas = clientesCon(telefono);
        if (cuentas.stream().anyMatch(Usuario::telefonoVerificado)) {
            throw new ExcepcionNegocio(CodigoError.TELEFONO_YA_REGISTRADO, "Este número ya está vinculado a una cuenta.");
        }
        List<Usuario> candidatas = cuentas.stream().filter(Usuario::activo).toList();
        if (candidatas.isEmpty()) {
            throw new ExcepcionNegocio(CodigoError.RECURSO_NO_ENCONTRADO, "No hay ninguna cuenta activa con este número.");
        }
        if (verificaciones.contarCreadasDesde(telefono, ahora.minus(Duration.ofHours(1))) >= MAX_CODIGOS_POR_HORA) {
            throw new ExcepcionNegocio(CodigoError.DEMASIADOS_INTENTOS, "Ya pedimos varios códigos para este número. Intenta de nuevo en una hora.");
        }
        List<String> correos = new ArrayList<>();
        for (Usuario cuenta : candidatas) {
            String codigo = "%06d".formatted(ALEATORIO.nextInt(1_000_000));
            verificaciones.anularPendientes(cuenta.id(), telefono, ahora);
            verificaciones.guardar(VerificacionTelefono.nueva(cuenta.id(), telefono, hash(cuenta.id(), telefono, codigo), ahora));
            correo.enviar(new MensajeCorreo(cuenta.correo(), "Tu código para vincular WhatsApp en Goltime", "verificar-telefono",
                    Map.of("nombre", cuenta.nombres(), "codigo", codigo, "numero", NumeroTelefono.de(telefono).legible(),
                            "minutos", VerificacionTelefono.VIGENCIA.toMinutes())));
            correos.add(enmascararCorreo(cuenta.correo()));
        }
        return new CodigoEnviado(correos);
    }

    /**
     * Vincula el número a la cuenta cuyo código coincide. Un código equivocado gasta un intento de todos
     * los códigos pendientes del número, así no se puede adivinar probando contra varias cuentas.
     */
    @Transactional(noRollbackFor = ExcepcionNegocio.class)
    public Usuario confirmarCodigo(String numero, String codigo) {
        String telefono = NumeroTelefono.de(numero).valor();
        Instant ahora = reloj.instant();
        String limpio = codigo == null ? "" : codigo.replaceAll("\\s", "");
        List<VerificacionTelefono> vigentes = verificaciones.pendientesDe(telefono).stream().filter(v -> v.estaVigente(ahora)).toList();
        for (VerificacionTelefono v : vigentes) {
            if (MessageDigest.isEqual(v.codigoHash().getBytes(StandardCharsets.UTF_8),
                    hash(v.usuarioId(), telefono, limpio).getBytes(StandardCharsets.UTF_8))) {
                return vincular(v, telefono, ahora);
            }
        }
        for (VerificacionTelefono v : vigentes) {
            v.registrarIntentoFallido();
            verificaciones.guardar(v);
        }
        throw new ExcepcionNegocio(CodigoError.TOKEN_INVALIDO, "El código no es correcto o ya venció.");
    }

    private Usuario vincular(VerificacionTelefono v, String telefono, Instant ahora) {
        Usuario usuario = usuarios.buscarPorId(v.usuarioId()).orElseThrow();
        if (!usuario.activo() || !telefono.equals(usuario.telefono())) {
            // Se desactivó o cambió de número después de pedir el código.
            throw new ExcepcionNegocio(CodigoError.TOKEN_INVALIDO, "El código no es correcto o ya venció.");
        }
        if (usuarios.existeTelefonoVerificado(telefono, usuario.id())) {
            throw new ExcepcionNegocio(CodigoError.TELEFONO_YA_REGISTRADO, "Este número ya está vinculado a otra cuenta.");
        }
        usuario.verificarTelefono();
        Usuario guardado = usuarios.guardar(usuario);
        v.marcarUsada(ahora);
        verificaciones.guardar(v);
        auditoria.registrar("VERIFICAR_TELEFONO", "usuarios", guardado.id(), Map.of("telefono", telefono, "canal", "WHATSAPP"), null);
        return guardado;
    }

    /**
     * A dónde avisarle por WhatsApp a un cliente: a su teléfono si lo verificó, o a su chat @lid si se vinculó
     * con el correo. Vacío si nunca demostró que el WhatsApp es suyo.
     */
    @Transactional(readOnly = true)
    public Optional<String> destinoWhatsapp(long usuarioId) {
        return usuarios.buscarPorId(usuarioId).filter(Usuario::activo).flatMap(u -> {
            if (u.telefonoVerificado()) {
                return Optional.of(u.telefono());
            }
            return Optional.ofNullable(u.whatsappLid()).map(l -> new LidWhatsapp(l).jid());
        });
    }

    private List<Usuario> clientesCon(String telefono) {
        return usuarios.buscarPorTelefono(telefono).stream().filter(u -> u.rol() == Rol.CLIENTE).toList();
    }

    /** {@code ana.perez@gmail.com} → {@code a***@gmail.com}: basta para reconocerlo sin revelarlo. */
    static String enmascararCorreo(String correo) {
        int arroba = correo.indexOf('@');
        return arroba <= 0 ? "***" : correo.charAt(0) + "***" + correo.substring(arroba);
    }

    private static String hash(long usuarioId, String telefono, String codigo) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest((usuarioId + ":" + telefono + ":" + codigo).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
