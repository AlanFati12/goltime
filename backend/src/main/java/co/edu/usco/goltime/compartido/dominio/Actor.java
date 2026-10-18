package co.edu.usco.goltime.compartido.dominio;

/** Quién realiza una acción: un usuario (id y rol) o el sistema (id nulo). */
public record Actor(Long id, Rol rol) {

    public static Actor sistema() {
        return new Actor(null, Rol.SISTEMA);
    }
}
