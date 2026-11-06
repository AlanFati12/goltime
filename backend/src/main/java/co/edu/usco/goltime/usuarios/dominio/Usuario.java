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
    private String googleSub;
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

    /**
     * Alta de un cliente la primera vez que entra con Google. Google ya confirmó el correo; el teléfono (WhatsApp)
     * queda vacío hasta que la persona lo agregue en su perfil. Entrar aceptando el aviso de términos de la pantalla
     * de ingreso cuenta como aceptación.
     */
    public static Usuario nuevoClienteGoogle(String nombres, String apellidos, String correo, String googleSub,
            Instant aceptoTerminosEn) {
        Usuario usuario = nuevo(Rol.CLIENTE, nombres, apellidos, correo, null, null);
        usuario.googleSub = googleSub;
        // La columna es obligatoria: sin apellido (Google no siempre lo da) queda vacío hasta que la persona lo complete.
        usuario.apellidos = apellidos == null ? "" : apellidos.trim();
        usuario.correoVerificado = true;
        usuario.aceptoTerminosEn = aceptoTerminosEn;
        return usuario;
    }

    /** Alta de un administrador (seed inicial o creado por otro administrador): queda autorizado y se vincula con su
     * cuenta de Google la primera vez que entra con ese correo. */
    public static Usuario nuevoAdministrador(String nombres, String apellidos, String correo, String telefono) {
        Usuario usuario = nuevo(Rol.ADMIN, nombres, apellidos, correo, telefono, null);
        usuario.correoVerificado = true;
        return usuario;
    }

    private static Usuario nuevo(Rol rol, String nombres, String apellidos, String correo, String telefono,
            String documento) {
        Usuario usuario = new Usuario(rol);
        usuario.nombres = limpiar(nombres);
        usuario.apellidos = limpiar(apellidos);
        usuario.correo = normalizarCorreo(correo);
        usuario.telefono = normalizarTelefono(rol, telefono);
        usuario.documento = limpiar(documento);
        usuario.activo = true;
        return usuario;
    }

    /** Reconstruye un usuario existente desde el almacenamiento (sin validar reglas de alta). */
    public static Usuario reconstruir(Long id, String nombres, String apellidos, String correo, String telefono,
            boolean telefonoVerificado, String whatsappLid, String documento, String googleSub, Rol rol, boolean activo, boolean correoVerificado,
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
        u.googleSub = googleSub;
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

    /** El WhatsApp es opcional (con Google no se pide al entrar): sin él el bot solo reconoce al cliente si lo vincula con el código. */
    private static String normalizarTelefono(Rol rol, String telefono) {
        return NumeroTelefono.normalizarOpcional(telefono);
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
        // Con Google puede no haber apellido: la columna es obligatoria, así que queda vacío.
        this.apellidos = apellidos == null ? "" : apellidos.trim();
        String nuevoTelefono = normalizarTelefono(rol, telefono);
        if (nuevoTelefono == null ? this.telefono != null : !nuevoTelefono.equals(this.telefono)) {
            // Un número nuevo hay que volver a probarlo desde WhatsApp.
            this.telefonoVerificado = false;
        }
        this.telefono = nuevoTelefono;
        this.documento = limpiar(documento);
    }

    /** Prefijo del identificador que genera el ingreso de prueba (solo desarrollo); el de Google de verdad es numérico. */
    static final String PREFIJO_SUB_DE_PRUEBA = "prueba-";

    /**
     * Une la cuenta con su cuenta de Google. Google ya confirmó que el correo es de quien entra. Una cuenta creada con el
     * ingreso de prueba (solo desarrollo) se puede pasar a una cuenta real de Google sin perderla; una ya vinculada a
     * otra cuenta de Google real, no.
     */
    public void vincularGoogle(String sub) {
        boolean eraDePrueba = this.googleSub != null && this.googleSub.startsWith(PREFIJO_SUB_DE_PRUEBA);
        if (this.googleSub != null && !this.googleSub.equals(sub) && !eraDePrueba) {
            throw new ExcepcionNegocio(CodigoError.CREDENCIALES_INVALIDAS, "Ese correo ya está vinculado a otra cuenta de Google.");
        }
        this.googleSub = sub;
        this.correoVerificado = true;
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

    /**
     * ¿Ya dio los datos que Google no entrega y Goltime necesita? Un cliente necesita nombre, apellido y WhatsApp (con
     * él el administrador lo contacta y el bot lo reconoce). El documento es opcional. El administrador no necesita nada.
     */
    public boolean perfilCompleto() {
        if (rol != Rol.CLIENTE) {
            return true;
        }
        return nombres != null && !nombres.isBlank() && apellidos != null && !apellidos.isBlank() && telefono != null;
    }

    public void registrarIngreso(Instant ahora) {
        this.ultimoLogin = ahora;
    }

    public void verificarCorreo() {
        this.correoVerificado = true;
    }

    /**
     * Quien escribe desde este WhatsApp envió el código que generó la cuenta en la web: ese teléfono pasa a ser el de
     * la cuenta, ya verificado.
     */
    public void vincularWhatsappTelefono(String telefonoE164) {
        if (rol != Rol.CLIENTE) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Solo los clientes usan el bot de WhatsApp.");
        }
        this.telefono = NumeroTelefono.de(telefonoE164).valor();
        this.telefonoVerificado = true;
    }

    /** El bot deja de reconocer la cuenta por cualquier vía. */
    public void desvincularWhatsapp() {
        this.telefonoVerificado = false;
        this.whatsappLid = null;
    }

    /**
     * Quien escribe desde ese @lid (WhatsApp no compartió su teléfono) envió el código que generó la cuenta en la web.
     * A diferencia del teléfono, no cambia si la persona edita su perfil.
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
    public String googleSub() { return googleSub; }
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
