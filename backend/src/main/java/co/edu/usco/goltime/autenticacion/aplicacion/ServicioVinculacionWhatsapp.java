package co.edu.usco.goltime.autenticacion.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.RegistroAuditoria;
import co.edu.usco.goltime.autenticacion.dominio.CodigoVinculacion;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.LidWhatsapp;
import co.edu.usco.goltime.compartido.dominio.NumeroTelefono;
import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.usuarios.aplicacion.RepositorioUsuarios;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/**
 * Identifica a quien le escribe al bot de WhatsApp y vincula ese WhatsApp con su cuenta (ADR 0018).
 *
 * <p>Cualquiera puede escribir cualquier número al registrarse, y WhatsApp a veces ni siquiera comparte el teléfono
 * (manda un @lid). Por eso un WhatsApp solo cuenta como de una cuenta cuando su dueño lo prueba: genera un código
 * en su perfil de la web (con sesión iniciada) y se lo envía al bot desde ese WhatsApp.
 */
@Service
public class ServicioVinculacionWhatsapp {

    static final int MAX_CODIGOS_POR_HORA = 10;
    static final int MAX_FALLOS_POR_HORA = 5;

    private final RepositorioUsuarios usuarios;
    private final RepositorioCodigosVinculacion codigos;
    private final RegistroAuditoria auditoria;
    private final Clock reloj;

    public ServicioVinculacionWhatsapp(RepositorioUsuarios usuarios, RepositorioCodigosVinculacion codigos, RegistroAuditoria auditoria,
            Clock reloj) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    public enum Situacion {
        /** El WhatsApp está vinculado a una cuenta de cliente activa: el bot actúa como ese cliente. */
        CLIENTE,
        /** Hay cuentas que escribieron ese teléfono, pero ninguna lo ha vinculado. */
        SIN_VERIFICAR,
        /** No hay cuenta con ese teléfono, o escribe desde un @lid sin vincular (no sabemos si tiene cuenta). */
        SIN_CUENTA,
        /** El WhatsApp es de un cliente desactivado: no puede usar el bot (regla 7). */
        DESACTIVADO
    }

    /**
     * Quién escribe. {@code usuarioId} y {@code nombres} solo vienen con {@link Situacion#CLIENTE}. Si WhatsApp no
     * compartió el teléfono, {@code lid} trae el identificador anónimo y {@code telefono} el de la cuenta (si la hay).
     */
    public record Identificacion(Situacion situacion, String telefono, Long usuarioId, String nombres, String lid) {
    }

    /** Código para mostrar en el perfil. */
    public record CodigoGenerado(String codigo, Instant expiraEn) {
    }

    // ---------- Quién escribe ----------

    @Transactional(readOnly = true)
    public Identificacion identificar(String numero) {
        String telefono = NumeroTelefono.de(numero).valor();
        List<Usuario> cuentas = usuarios.buscarPorTelefono(telefono).stream().filter(u -> u.rol() == Rol.CLIENTE).toList();
        Optional<Usuario> verificado = cuentas.stream().filter(Usuario::telefonoVerificado).findFirst();
        if (verificado.isPresent()) {
            Usuario u = verificado.get();
            return u.activo() ? new Identificacion(Situacion.CLIENTE, telefono, u.id(), u.nombres(), null)
                    : new Identificacion(Situacion.DESACTIVADO, telefono, null, null, null);
        }
        if (cuentas.stream().anyMatch(Usuario::activo)) {
            return new Identificacion(Situacion.SIN_VERIFICAR, telefono, null, null, null);
        }
        return new Identificacion(cuentas.isEmpty() ? Situacion.SIN_CUENTA : Situacion.DESACTIVADO, telefono, null, null, null);
    }

    /** Quien escribe desde un @lid: el cliente que lo vinculó o, si nadie, alguien de quien no sabemos si tiene cuenta. */
    @Transactional(readOnly = true)
    public Identificacion identificarPorLid(String lidTexto) {
        String lid = LidWhatsapp.de(lidTexto).valor();
        return usuarios.buscarPorWhatsappLid(lid).filter(u -> u.rol() == Rol.CLIENTE)
                .map(u -> u.activo() ? new Identificacion(Situacion.CLIENTE, u.telefono(), u.id(), u.nombres(), lid)
                        : new Identificacion(Situacion.DESACTIVADO, null, null, null, lid))
                .orElseGet(() -> new Identificacion(Situacion.SIN_CUENTA, null, null, null, lid));
    }

    /**
     * A dónde avisarle por WhatsApp a un cliente: a su teléfono si lo vinculó, o a su chat @lid. Vacío si nunca
     * demostró que el WhatsApp es suyo.
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

    // ---------- Vinculación ----------

    /** Desde el perfil: un código nuevo anula los anteriores sin usar. */
    @Transactional
    public CodigoGenerado generarCodigo(long usuarioId) {
        Usuario usuario = cliente(usuarioId);
        Instant ahora = reloj.instant();
        if (codigos.contarGeneradosDesde(usuario.id(), ahora.minus(Duration.ofHours(1))) >= MAX_CODIGOS_POR_HORA) {
            throw new ExcepcionNegocio(CodigoError.DEMASIADOS_INTENTOS, "Generaste muchos códigos seguidos. Intenta de nuevo en una hora.");
        }
        codigos.anularDeUsuario(usuario.id(), ahora);
        Instant expira = ahora.plus(CodigoVinculacion.VIGENCIA);
        for (int intento = 0; intento < 5; intento++) {
            String codigo = CodigoVinculacion.generar();
            if (codigos.guardar(usuario.id(), CodigoVinculacion.hash(codigo), expira)) {
                return new CodigoGenerado(codigo, expira);
            }
        }
        throw new IllegalStateException("No se pudo generar un código de vinculación único");
    }

    /**
     * Desde el bot: vincula el WhatsApp que envió el código (su teléfono o su @lid) con la cuenta que lo generó. Con
     * teléfono, ese pasa a ser el teléfono de la cuenta, ya verificado. Los códigos equivocados cuentan para el límite
     * de intentos de ese WhatsApp.
     */
    @Transactional(noRollbackFor = ExcepcionNegocio.class)
    public Usuario vincular(String numero, String lidTexto, String codigoTexto) {
        String telefono = numero == null ? null : NumeroTelefono.de(numero).valor();
        String lid = telefono != null || lidTexto == null ? null : LidWhatsapp.de(lidTexto).valor();
        if (telefono == null && lid == null) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Falta el número o el identificador de WhatsApp de quien escribe.");
        }
        String remitente = telefono != null ? telefono : "lid:" + lid;
        Instant ahora = reloj.instant();
        if (codigos.contarIntentosFallidosDesde(remitente, ahora.minus(Duration.ofHours(1))) >= MAX_FALLOS_POR_HORA) {
            throw new ExcepcionNegocio(CodigoError.DEMASIADOS_INTENTOS, "Probaste varios códigos equivocados. Intenta de nuevo en una hora.");
        }
        Optional<String> hash = CodigoVinculacion.normalizar(codigoTexto).map(CodigoVinculacion::hash);
        Optional<Usuario> cuenta = hash.flatMap(h -> codigos.usuarioDeVigente(h, ahora)).flatMap(usuarios::buscarPorId)
                .filter(u -> u.rol() == Rol.CLIENTE && u.activo());
        if (cuenta.isEmpty()) {
            codigos.registrarIntentoFallido(remitente);
            throw new ExcepcionNegocio(CodigoError.TOKEN_INVALIDO, "El código no es correcto o ya venció. Genera uno nuevo en tu perfil de Goltime.");
        }
        Usuario usuario = cuenta.get();
        if (telefono != null) {
            if (usuarios.existeTelefonoVerificado(telefono, usuario.id())) {
                throw new ExcepcionNegocio(CodigoError.TELEFONO_YA_REGISTRADO, "Este WhatsApp ya está vinculado a otra cuenta.");
            }
            usuario.vincularWhatsappTelefono(telefono);
        } else {
            if (usuarios.buscarPorWhatsappLid(lid).filter(otro -> !otro.id().equals(usuario.id())).isPresent()) {
                throw new ExcepcionNegocio(CodigoError.TELEFONO_YA_REGISTRADO, "Este WhatsApp ya está vinculado a otra cuenta.");
            }
            usuario.vincularWhatsappLid(lid);
        }
        Usuario guardado = usuarios.guardar(usuario);
        codigos.marcarUsado(hash.get(), ahora);
        auditoria.registrar("VINCULAR_WHATSAPP", "usuarios", guardado.id(),
                Map.of("whatsapp", telefono != null ? telefono : "lid", "canal", "WHATSAPP"), null);
        return guardado;
    }

    /** Desde el perfil: el bot deja de reconocer la cuenta (teléfono y @lid). */
    @Transactional
    public Usuario desvincular(long usuarioId) {
        Usuario usuario = cliente(usuarioId);
        usuario.desvincularWhatsapp();
        codigos.anularDeUsuario(usuario.id(), reloj.instant());
        Usuario guardado = usuarios.guardar(usuario);
        auditoria.registrar("DESVINCULAR_WHATSAPP", "usuarios", guardado.id(), Map.of(), null);
        return guardado;
    }

    private Usuario cliente(long usuarioId) {
        return usuarios.buscarPorId(usuarioId).filter(u -> u.rol() == Rol.CLIENTE && u.activo())
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.ACCESO_DENEGADO, "Solo los clientes vinculan su WhatsApp con el bot."));
    }
}
