package co.edu.usco.goltime.compartido.aplicacion;

import java.util.Optional;

import co.edu.usco.goltime.compartido.dominio.Actor;

/** Puerto: quién está haciendo la petición actual y desde qué IP. */
public interface ContextoActual {

    Optional<Actor> actor();

    Optional<String> ip();
}
