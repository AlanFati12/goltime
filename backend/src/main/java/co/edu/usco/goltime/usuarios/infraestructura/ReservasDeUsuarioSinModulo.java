package co.edu.usco.goltime.usuarios.infraestructura;

import org.springframework.stereotype.Component;

import co.edu.usco.goltime.usuarios.aplicacion.ReservasDeUsuario;

/**
 * Adaptador provisional: todavía no existe el módulo de reservas, así que ningún cliente tiene
 * reservas futuras. La Fase 3 lo reemplaza por la implementación real (y se elimina esta clase).
 */
@Component
class ReservasDeUsuarioSinModulo implements ReservasDeUsuario {

    @Override
    public int contarFuturasActivas(long usuarioId) {
        return 0;
    }

    @Override
    public int cancelarFuturas(long usuarioId, String motivo) {
        return 0;
    }
}
