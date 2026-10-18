package co.edu.usco.goltime.sedes.dominio;

import java.math.BigDecimal;
import java.util.List;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;

/** Sede del negocio: agrupa canchas y define el horario de operación por defecto. */
public class Sede {

    private Long id;
    private String nombre;
    private String direccion;
    private String ciudad;
    private String telefono;
    private String whatsapp;
    private String descripcion;
    private List<String> servicios;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private boolean activa;
    private HorarioSemanal horario;

    private Sede() {
    }

    public static Sede nueva(DatosSede datos, HorarioSemanal horario) {
        Sede sede = new Sede();
        sede.activa = true;
        sede.horario = horario;
        sede.actualizar(datos);
        return sede;
    }

    public static Sede reconstruir(Long id, DatosSede datos, boolean activa, HorarioSemanal horario) {
        Sede sede = nueva(datos, horario);
        sede.id = id;
        sede.activa = activa;
        return sede;
    }

    public void actualizar(DatosSede d) {
        this.nombre = obligatorio(d.nombre(), "El nombre de la sede es obligatorio.");
        this.direccion = obligatorio(d.direccion(), "La dirección es obligatoria.");
        this.ciudad = obligatorio(d.ciudad(), "La ciudad es obligatoria.");
        this.telefono = vacioANulo(d.telefono());
        this.whatsapp = vacioANulo(d.whatsapp());
        this.descripcion = vacioANulo(d.descripcion());
        this.servicios = d.servicios() == null ? List.of()
                : d.servicios().stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        if (d.latitud() != null && (d.latitud().abs().compareTo(BigDecimal.valueOf(90)) > 0)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La latitud debe estar entre -90 y 90.");
        }
        if (d.longitud() != null && (d.longitud().abs().compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La longitud debe estar entre -180 y 180.");
        }
        this.latitud = d.latitud();
        this.longitud = d.longitud();
    }

    public void definirHorario(HorarioSemanal nuevo) {
        this.horario = nuevo;
    }

    public void cambiarEstado(boolean activa) {
        this.activa = activa;
    }

    static String obligatorio(String texto, String mensaje) {
        if (texto == null || texto.isBlank()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, mensaje);
        }
        return texto.trim();
    }

    static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    public Long id() { return id; }
    public String nombre() { return nombre; }
    public String direccion() { return direccion; }
    public String ciudad() { return ciudad; }
    public String telefono() { return telefono; }
    public String whatsapp() { return whatsapp; }
    public String descripcion() { return descripcion; }
    public List<String> servicios() { return servicios; }
    public BigDecimal latitud() { return latitud; }
    public BigDecimal longitud() { return longitud; }
    public boolean activa() { return activa; }
    public HorarioSemanal horario() { return horario; }

    /** Datos editables de una sede. */
    public record DatosSede(String nombre, String direccion, String ciudad, String telefono, String whatsapp,
            String descripcion, List<String> servicios, BigDecimal latitud, BigDecimal longitud) {
    }
}
