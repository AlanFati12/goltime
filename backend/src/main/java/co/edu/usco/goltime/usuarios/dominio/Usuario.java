package co.edu.usco.goltime.usuarios.dominio;

import java.time.Instant;
import java.util.Locale;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.NumeroTelefono;
import co.edu.usco.goltime.compartido.dominio.Rol;

/**
 * Usuario del sistema (administrador o cliente). Clase de dominio pura: concentra las reglas de
 * activación, desactivación y cambio de credenciales. Los cambios que deben invalidar sesiones
 * incrementan {@code versionToken}.
 */
public class Usuario {

    private Long id;
    private String nombres;
    private String apellidos;
    private String correo;
    private String telefono;
    private boolean telefonoVerificado;
    private String whatsappLid;
    private String documento;
    private String claveHash;
    private final Rol rol;
    private boolean activo;
    private boolean correoVerificado;
    private String motivoDesactivacion;
    private Long desactivadoPor;
    private Instant fechaDesactivacion;
    private int versionToken;
    private Instant ultimoLogin;
    private Instant aceptoTerminosEn;
    private boolean puedeInscribirEquipos;
    private Instant creadoEn;

    private Usuario(Rol rol) {
        this.rol = rol;
    }

    /** Alta de un cliente desde el registro público. */
    public static Usuario nuevoCliente(String nombres, String apellidos, String correo, String telefono,
            String documento, String claveHash, Instant aceptoTerminosEn) {
        if (aceptoTerminosEn == null) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS,
                    "Debes aceptar los términos y el tratamiento de datos personales.");
        }
        Usuario usuario = nuevo(Rol.CLIENTE, nombres, apellidos, correo, telefono, documento, claveHash);
        usuario.aceptoTerminosEn = aceptoTerminosEn;
        return usuario;
    }

    /** Alta de un administrador (seed inicial o creado por otro administrador). */
    public static Usuario nuevoAdministrador(String nombres, String apellidos, String correo, String telefono,
            String claveHash) {
        Usuario usuario = nuevo(Rol.ADMIN, nombres, apellidos, correo, telefono, null, claveHash);
        usuario.correoVerificado = true;
        return usuario;
    }

    private static Usuario nuevo(Rol rol, String nombres, String apellidos, String correo, String telefono,
            String documento, String claveHash) {
        Usuario usuario = new Usuario(rol);
        usuario.nombres = limpiar(nombres);
        usuario.apellidos = limpiar(apellidos);
        usuario.correo = normalizarCorreo(correo);
        usuario.telefono = normalizarTelefono(rol, telefono);
        usuario.documento = limpiar(documento);
        usuario.claveHash = claveHash;
        usuario.activo = true;
        return usuario;
    }

    /** Reconstruye un usuario existente desde el almacenamiento (sin validar reglas de alta). */
    public static Usuario reconstruir(Long id, String nombres, String apellidos, String correo, String telefono,
            boolean telefonoVerificado, String whatsappLid, String documento, String claveHash, Rol rol, boolean activo, boolean correoVerificado,
            String motivoDesactivacion, Long desactivadoPor, Instant fechaDesactivacion, int versionToken,
            Instant ultimoLogin, Instant aceptoTerminosEn, boolean puedeInscribirEquipos, Instant creadoEn) {
        Usuario u = new Usuario(rol);
        u.id = id;
        u.nombres = nombres;
        u.apellidos = apellidos;
        u.correo = correo;
        u.telefono = telefono;
        u.telefonoVerificado = telefonoVerificado;
        u.whatsappLid = whatsappLid;
        u.documento = documento;
        u.claveHash = claveHash;
        u.activo = activo;
        u.correoVerificado = correoVerificado;
        u.motivoDesactivacion = motivoDesactivacion;
        u.desactivadoPor = desactivadoPor;
        u.fechaDesactivacion = fechaDesactivacion;
        u.versionToken = versionToken;
        u.ultimoLogin = ultimoLogin;
        u.aceptoTerminosEn = aceptoTerminosEn;
        u.puedeInscribirEquipos = puedeInscribirEquipos;
        u.creadoEn = creadoEn;
        return u;
    }

    public static String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }

    /** El cliente siempre tiene WhatsApp (lo usa el bot para reconocerlo); para el administrador es opcional. */
    private static String normalizarTelefono(Rol rol, String telefono) {
        return rol == Rol.CLIENTE ? NumeroTelefono.de(telefono).valor() : NumeroTelefono.normalizarOpcional(telefono);
    }

    private static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    // --- Comportamiento ---

    public void actualizarPerfil(String nombres, String apellidos, String telefono, String documento) {
        this.nombres = limpiar(nombres);
        this.apellidos = limpiar(apellidos);
        String nuevoTelefono = normalizarTelefono(rol, telefono);
        if (nuevoTelefono == null ? this.telefono != null : !nuevoTelefono.equals(this.telefono)) {
            // Un número nuevo hay que volver a probarlo desde WhatsApp.
            this.telefonoVerificado = false;
        }
        this.telefono = nuevoTelefono;
        this.documento = limpiar(documento);
    }

    /** Cambia la contraseña e invalida todos los tokens de acceso emitidos antes. */
    public void cambiarClave(String nuevoHash) {
        this.claveHash = nuevoHash;
        this.versionToken++;
    }

    /** Solo se desactivan clientes; el motivo es obligatorio. Invalida los tokens vigentes. */
    public void desactivar(String motivo, Long actorId, Instant ahora) {
        if (rol != Rol.CLIENTE) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Solo se pueden desactivar clientes.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El motivo de la desactivación es obligatorio.");
        }
        if (!activo) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El cliente ya está desactivado.");
        }
        this.activo = false;
        this.motivoDesactivacion = motivo.trim();
        this.desactivadoPor = actorId;
        this.fechaDesactivacion = ahora;
        this.versionToken++;
    }

    public void activar() {
        if (activo) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "El cliente ya está activo.");
        }
        this.activo = true;
        this.motivoDesactivacion = null;
        this.desactivadoPor = null;
        this.fechaDesactivacion = null;
    }

    public void registrarIngreso(Instant ahora) {
        this.ultimoLogin = ahora;
    }

    public void verificarCorreo() {
        this.correoVerificado = true;
    }

    /** Quien escribe desde este número probó con el código enviado al correo que también es dueño de la cuenta. */
    public void verificarTelefono() {
        if (telefono == null) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La cuenta no tiene número de WhatsApp.");
        }
        this.telefonoVerificado = true;
    }

    /**
     * Quien escribe desde ese @lid (WhatsApp no compartió su teléfono) probó con el código del correo que es dueño
     * de la cuenta. A diferencia del teléfono, no cambia si la persona edita su perfil.
     */
    public void vincularWhatsappLid(String lid) {
        if (rol != Rol.CLIENTE) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Solo los clientes usan el bot de WhatsApp.");
        }
        this.whatsappLid = lid;
    }

    public void habilitarInscripcionEquipos(boolean habilitado) {
        if (rol != Rol.CLIENTE) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Solo los clientes pueden inscribir equipos.");
        }
        this.puedeInscribirEquipos = habilitado;
    }

    // --- Lectura ---

    public Long id() { return id; }
    public String nombres() { return nombres; }
    public String apellidos() { return apellidos; }
    public String correo() { return correo; }
    public String telefono() { return telefono; }
    public boolean telefonoVerificado() { return telefonoVerificado; }
    public String whatsappLid() { return whatsappLid; }
    /** El bot reconoce la cuenta, por su teléfono verificado o por su @lid. */
    public boolean whatsappVinculado() { return telefonoVerificado || whatsappLid != null; }
    public String documento() { return documento; }
    public String claveHash() { return claveHash; }
    public Rol rol() { return rol; }
    public boolean activo() { return activo; }
    public boolean correoVerificado() { return correoVerificado; }
    public String motivoDesactivacion() { return motivoDesactivacion; }
    public Long desactivadoPor() { return desactivadoPor; }
    public Instant fechaDesactivacion() { return fechaDesactivacion; }
    public int versionToken() { return versionToken; }
    public Instant ultimoLogin() { return ultimoLogin; }
    public Instant aceptoTerminosEn() { return aceptoTerminosEn; }
    public boolean puedeInscribirEquipos() { return puedeInscribirEquipos; }
    public Instant creadoEn() { return creadoEn; }
}
