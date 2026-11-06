package co.edu.usco.goltime.sedes.aplicacion;

import java.util.List;
import java.util.Optional;

import co.edu.usco.goltime.sedes.dominio.Cancha;
import co.edu.usco.goltime.sedes.dominio.Foto;

public interface RepositorioCanchas {

    Optional<Cancha> buscarPorId(long id);

    List<Cancha> buscarPorIds(java.util.Collection<Long> ids);

    /** Canchas de una sede (o de todas si {@code sedeId} es nulo), ordenadas por sede, orden y nombre. */
    List<Cancha> listar(Long sedeId);

    boolean existeNombre(long sedeId, String nombre, Long excluirId);

    /** Guarda los datos, estado, horario propio y composición. */
    Cancha guardar(Cancha cancha);

    /** Canchas compuestas que usan a esta como componente. */
    List<Cancha> compuestasQueUsan(long canchaId);

    List<Foto> fotos(long canchaId);

    /** Fotos de varias canchas en una sola consulta (clave: id de cancha; sin entrada si no tiene fotos). */
    java.util.Map<Long, List<Foto>> fotosDe(java.util.Collection<Long> canchaIds);

    Foto agregarFoto(long canchaId, String archivo);

    Optional<Foto> quitarFoto(long canchaId, long fotoId);
}
