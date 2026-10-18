package co.edu.usco.goltime.compartido.dominio;

/** Roles de la aplicación. Solo existen dos. */
public enum Rol {
    ADMIN,
    CLIENTE,
    /** Actor lógico para tareas automáticas (jobs); nunca se asigna a un usuario real. */
    SISTEMA
}
