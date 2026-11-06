package co.edu.usco.goltime.sedes.infraestructura;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.sedes.aplicacion.RepositorioSedes;
import co.edu.usco.goltime.sedes.dominio.Foto;
import co.edu.usco.goltime.sedes.dominio.HorarioDia;
import co.edu.usco.goltime.sedes.dominio.HorarioSemanal;
import co.edu.usco.goltime.sedes.dominio.Sede;
import co.edu.usco.goltime.sedes.dominio.Sede.DatosSede;

@Repository
class AdaptadorRepositorioSedes implements RepositorioSedes {

    private final RepositorioSedesJpa jpa;
    private final RepositorioFotosSedeJpa fotos;

    AdaptadorRepositorioSedes(RepositorioSedesJpa jpa, RepositorioFotosSedeJpa fotos) {
        this.jpa = jpa;
        this.fotos = fotos;
    }

    @Override
    public Optional<Sede> buscarPorId(long id) {
        return jpa.findById(id).map(AdaptadorRepositorioSedes::aDominio);
    }

    @Override
    public List<Sede> listar(boolean soloActivas) {
        return (soloActivas ? jpa.findByActivaTrueOrderByNombreAsc() : jpa.findAllByOrderByNombreAsc())
                .stream().map(AdaptadorRepositorioSedes::aDominio).toList();
    }

    @Override
    public boolean existeNombre(String nombre, Long excluirId) {
        return jpa.existeNombre(nombre.trim(), excluirId);
    }

    @Override
    public Sede guardar(Sede sede) {
        SedeEntidad e = sede.id() == null ? new SedeEntidad() : jpa.findById(sede.id()).orElseThrow();
        e.nombre = sede.nombre();
        e.direccion = sede.direccion();
        e.ciudad = sede.ciudad();
        e.telefono = sede.telefono();
        e.whatsapp = sede.whatsapp();
        e.descripcion = sede.descripcion();
        e.servicios = new ArrayList<>(sede.servicios());
        e.latitud = sede.latitud();
        e.longitud = sede.longitud();
        e.activa = sede.activa();
        e.horario.clear();
        sede.horario().dias().forEach(d -> e.horario.add(aEmbebido(d)));
        return aDominio(jpa.saveAndFlush(e));
    }

    @Override
    public List<Foto> fotos(long sedeId) {
        return fotos.findBySedeIdOrderByOrdenAscIdAsc(sedeId).stream().map(f -> new Foto(f.id, f.archivo, f.orden)).toList();
    }

    @Override
    public java.util.Map<Long, List<Foto>> fotosDe(java.util.Collection<Long> sedeIds) {
        if (sedeIds.isEmpty()) {
            return java.util.Map.of();
        }
        return fotos.findBySedeIdInOrderByOrdenAscIdAsc(sedeIds).stream().collect(java.util.stream.Collectors.groupingBy(
                f -> f.sedeId, java.util.stream.Collectors.mapping(f -> new Foto(f.id, f.archivo, f.orden), java.util.stream.Collectors.toList())));
    }

    @Override
    public List<Sede> buscarPorIds(java.util.Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : jpa.findAllById(ids).stream().map(AdaptadorRepositorioSedes::aDominio).toList();
    }

    @Override
    public Foto agregarFoto(long sedeId, String archivo) {
        FotoSedeEntidad f = new FotoSedeEntidad();
        f.sedeId = sedeId;
        f.archivo = archivo;
        f.orden = fotos.findBySedeIdOrderByOrdenAscIdAsc(sedeId).size();
        FotoSedeEntidad guardada = fotos.saveAndFlush(f);
        return new Foto(guardada.id, guardada.archivo, guardada.orden);
    }

    @Override
    public Optional<Foto> quitarFoto(long sedeId, long fotoId) {
        return fotos.findByIdAndSedeId(fotoId, sedeId).map(f -> {
            fotos.delete(f);
            return new Foto(f.id, f.archivo, f.orden);
        });
    }

    static Sede aDominio(SedeEntidad e) {
        return Sede.reconstruir(e.id,
                new DatosSede(e.nombre, e.direccion, e.ciudad, e.telefono, e.whatsapp, e.descripcion, e.servicios, e.latitud, e.longitud),
                e.activa, aHorario(e.horario));
    }

    static HorarioSemanal aHorario(List<HorarioDiaEmbebido> filas) {
        return new HorarioSemanal(filas.stream().map(f -> new HorarioDia(f.diaSemana, f.cerrado,
                f.minutoApertura == null ? 0 : f.minutoApertura, f.minutoCierre == null ? 0 : f.minutoCierre)).toList());
    }

    static HorarioDiaEmbebido aEmbebido(HorarioDia d) {
        return d.cerrado() ? new HorarioDiaEmbebido(d.diaSemana(), true, null, null)
                : new HorarioDiaEmbebido(d.diaSemana(), false, d.minutoApertura(), d.minutoCierre());
    }
}
