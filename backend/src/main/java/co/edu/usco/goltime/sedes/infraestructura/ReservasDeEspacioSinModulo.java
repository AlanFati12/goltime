package co.edu.usco.goltime.sedes.infraestructura;

import org.springframework.stereotype.Component;

import co.edu.usco.goltime.sedes.aplicacion.ReservasDeEspacio;

/**
 * Adaptador provisional: todavía no existe el módulo de reservas, así que ninguna cancha ni sede
 * tiene reservas futuras. La Fase 3 lo reemplaza por la implementación real (y se elimina esta clase).
 */
@Component
class ReservasDeEspacioSinModulo implements ReservasDeEspacio {

    @Override
    public int contarFuturasActivasEnCancha(long canchaId) {
        return 0;
    }

    @Override
    public int contarFuturasActivasEnSede(long sedeId) {
        return 0;
    }

    @Override
    public int cancelarFuturasEnCancha(long canchaId, String motivo) {
        return 0;
    }

    @Override
    public int cancelarFuturasEnSede(long sedeId, String motivo) {
        return 0;
    }
}
