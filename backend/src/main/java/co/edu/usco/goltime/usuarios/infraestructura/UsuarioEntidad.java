package co.edu.usco.goltime.usuarios.infraestructura;

import java.time.Instant;

import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.compartido.infraestructura.persistencia.EntidadAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Mapeo JPA de la tabla {@code usuarios}. Se convierte a/desde el dominio en el adaptador. */
@Entity
@Table(name = "usuarios")
public class UsuarioEntidad extends EntidadAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombres;

    @Column(nullable = false)
    private String apellidos;

    @Column(nullable = false)
    private String correo;

    private String telefono;

    @Column(name = "telefono_verificado", nullable = false)
    private boolean telefonoVerificado;

    @Column(name = "whatsapp_lid")
    private String whatsappLid;

    private String documento;

    @Column(name = "google_sub", length = 64)
    private String googleSub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "correo_verificado", nullable = false)
    private boolean correoVerificado;

    @Column(name = "motivo_desactivacion")
    private String motivoDesactivacion;

    @Column(name = "desactivado_por")
    private Long desactivadoPor;

    @Column(name = "fecha_desactivacion")
    private Instant fechaDesactivacion;

    @Column(name = "version_token", nullable = false)
    private int versionToken;

    @Column(name = "ultimo_login")
    private Instant ultimoLogin;

    @Column(name = "acepto_terminos_en")
    private Instant aceptoTerminosEn;

    @Column(name = "puede_inscribir_equipos", nullable = false)
    private boolean puedeInscribirEquipos;

    @Version
    private long version;

    protected UsuarioEntidad() {
    }

    public Long getId() { return id; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public boolean isTelefonoVerificado() { return telefonoVerificado; }
    public void setTelefonoVerificado(boolean telefonoVerificado) { this.telefonoVerificado = telefonoVerificado; }
    public String getWhatsappLid() { return whatsappLid; }
    public void setWhatsappLid(String whatsappLid) { this.whatsappLid = whatsappLid; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public boolean isCorreoVerificado() { return correoVerificado; }
    public void setCorreoVerificado(boolean correoVerificado) { this.correoVerificado = correoVerificado; }
    public String getMotivoDesactivacion() { return motivoDesactivacion; }
    public void setMotivoDesactivacion(String motivoDesactivacion) { this.motivoDesactivacion = motivoDesactivacion; }
    public Long getDesactivadoPor() { return desactivadoPor; }
    public void setDesactivadoPor(Long desactivadoPor) { this.desactivadoPor = desactivadoPor; }
    public Instant getFechaDesactivacion() { return fechaDesactivacion; }
    public void setFechaDesactivacion(Instant fechaDesactivacion) { this.fechaDesactivacion = fechaDesactivacion; }
    public int getVersionToken() { return versionToken; }
    public void setVersionToken(int versionToken) { this.versionToken = versionToken; }
    public Instant getUltimoLogin() { return ultimoLogin; }
    public void setUltimoLogin(Instant ultimoLogin) { this.ultimoLogin = ultimoLogin; }
    public Instant getAceptoTerminosEn() { return aceptoTerminosEn; }
    public void setAceptoTerminosEn(Instant aceptoTerminosEn) { this.aceptoTerminosEn = aceptoTerminosEn; }
    public boolean isPuedeInscribirEquipos() { return puedeInscribirEquipos; }
    public void setPuedeInscribirEquipos(boolean valor) { this.puedeInscribirEquipos = valor; }
    public long getVersion() { return version; }
}
