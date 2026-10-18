package co.edu.usco.goltime.sedes.infraestructura;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.sedes.aplicacion.RepositorioCanchas;
import co.edu.usco.goltime.sedes.dominio.Cancha;
import co.edu.usco.goltime.sedes.dominio.Cancha.DatosCancha;
import co.edu.usco.goltime.sedes.dominio.Foto;

@Repository
class AdaptadorRepositorioCanchas implements RepositorioCanchas {

    private final RepositorioCanchasJpa jpa;
    private final RepositorioFotosCanchaJpa fotos;

    AdaptadorRepositorioCanchas(RepositorioCanchasJpa jpa, RepositorioFotosCanchaJpa fotos) {
        this.jpa = jpa;
        this.fotos = fotos;
    }

    @Override
    public Optional<Cancha> buscarPorId(long id) {
        return jpa.findById(id).map(AdaptadorRepositorioCanchas::aDominio);
    }

    @Override
    public List<Cancha> buscarPorIds(Collection<Long> ids) {
        return jpa.findAllById(ids).stream().map(AdaptadorRepositorioCanchas::aDominio).toList();
    }

    @Override
    public List<Cancha> listar(Long sedeId) {
        return (sedeId == null ? jpa.findAllByOrderBySedeIdAscOrdenAscNombreAsc() : jpa.findBySedeIdOrderByOrdenAscNombreAsc(sedeId))
                .stream().map(AdaptadorRepositorioCanchas::aDominio).toList();
    }

    @Override
    public boolean existeNombre(long sedeId, String nombre, Long excluirId) {
        return jpa.existeNombre(sedeId, nombre.trim(), excluirId);
    }

    @Override
    public Cancha guardar(Cancha c) {
        CanchaEntidad e;
        if (c.id() == null) {
            e = new CanchaEntidad();
            e.sedeId = c.sedeId();
        } else {
            e = jpa.findById(c.id()).orElseThrow();
        }
        e.nombre = c.nombre();
        e.modalidad = c.modalidad();
        e.jugadoresPorEquipo = c.jugadoresPorEquipo();
        e.capacidadTotal = c.capacidadTotal();
        e.superficie = c.superficie();
        e.largoM = c.largoM();
        e.anchoM = c.anchoM();
        e.techada = c.techada();
        e.iluminacion = c.iluminacion();
        e.precioBaseHora = c.precioBaseHora();
        e.duracionBloqueMin = c.duracionBloqueMin();
        e.duracionMinMin = c.duracionMinMin();
        e.duracionMaxMin = c.duracionMaxMin();
        e.estado = c.estado();
        e.descripcion = c.descripcion();
        e.orden = c.orden();
        e.horarioPropio.clear();
        if (c.horarioPropio() != null) {
            c.horarioPropio().dias().forEach(d -> e.horarioPropio.add(AdaptadorRepositorioSedes.aEmbebido(d)));
        }
        e.componentes.clear();
        e.componentes.addAll(c.componentes());
        return aDominio(jpa.saveAndFlush(e));
    }

    @Override
    public List<Cancha> compuestasQueUsan(long canchaId) {
        return jpa.compuestasQueUsan(canchaId).stream().map(AdaptadorRepositorioCanchas::aDominio).toList();
    }

    @Override
    public List<Foto> fotos(long canchaId) {
        return fotos.findByCanchaIdOrderByOrdenAscIdAsc(canchaId).stream().map(f -> new Foto(f.id, f.archivo, f.orden)).toList();
    }

    @Override
    public Foto agregarFoto(long canchaId, String archivo) {
        FotoCanchaEntidad f = new FotoCanchaEntidad();
        f.canchaId = canchaId;
        f.archivo = archivo;
        f.orden = fotos.findByCanchaIdOrderByOrdenAscIdAsc(canchaId).size();
        FotoCanchaEntidad guardada = fotos.saveAndFlush(f);
        return new Foto(guardada.id, guardada.archivo, guardada.orden);
    }

    @Override
    public Optional<Foto> quitarFoto(long canchaId, long fotoId) {
        return fotos.findByIdAndCanchaId(fotoId, canchaId).map(f -> {
            fotos.delete(f);
            return new Foto(f.id, f.archivo, f.orden);
        });
    }

    private static Cancha aDominio(CanchaEntidad e) {
        DatosCancha datos = new DatosCancha(e.nombre, e.modalidad, e.jugadoresPorEquipo, e.capacidadTotal, e.superficie,
                e.largoM, e.anchoM, e.techada, e.iluminacion, e.precioBaseHora, e.duracionBloqueMin, e.duracionMinMin,
                e.duracionMaxMin, e.descripcion, e.orden);
        return Cancha.reconstruir(e.id, e.sedeId, datos, e.estado,
                e.horarioPropio.isEmpty() ? null : AdaptadorRepositorioSedes.aHorario(e.horarioPropio), new HashSet<>(e.componentes));
    }
}
