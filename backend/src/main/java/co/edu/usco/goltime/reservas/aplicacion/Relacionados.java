package co.edu.usco.goltime.reservas.aplicacion;

/** Datos de otras tablas que se muestran junto a una reserva (cancha, sede y cliente). */
public record Relacionados(long canchaId, String canchaNombre, long sedeId, String sedeNombre, String modalidad,
        String clienteNombre, String clienteCorreo, String clienteTelefono) {
}
