package co.edu.usco.goltime.usuarios.aplicacion;

/** Filtros del listado de clientes; los nulos no filtran. */
public record FiltroClientes(String busqueda, Boolean activo) {
}
