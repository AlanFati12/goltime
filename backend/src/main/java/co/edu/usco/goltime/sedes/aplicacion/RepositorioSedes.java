package co.edu.usco.goltime.sedes.aplicacion;

import java.util.List;
import java.util.Optional;

import co.edu.usco.goltime.sedes.dominio.Foto;
import co.edu.usco.goltime.sedes.dominio.Sede;

public interface RepositorioSedes {

    Optional<Sede> buscarPorId(long id);

    List<Sede> listar(boolean soloActivas);

    /** ¿Existe otra sede con ese nombre (sin distinguir mayúsculas)? {@code excluirId} puede ser nulo. */
    boolean existeNombre(String nombre, Long excluirId);

    /** Guarda los datos, el estado y el horario semanal. */
    Sede guardar(Sede sede);

    List<Foto> fotos(long sedeId);

    Foto agregarFoto(long sedeId, String archivo);

    Optional<Foto> quitarFoto(long sedeId, long fotoId);
}
