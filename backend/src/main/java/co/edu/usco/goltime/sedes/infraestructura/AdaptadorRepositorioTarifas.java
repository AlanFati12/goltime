package co.edu.usco.goltime.sedes.infraestructura;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.sedes.aplicacion.RepositorioTarifas;
import co.edu.usco.goltime.sedes.dominio.Tarifa;

@Repository
class AdaptadorRepositorioTarifas implements RepositorioTarifas {

    private final RepositorioTarifasJpa jpa;

    AdaptadorRepositorioTarifas(RepositorioTarifasJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Tarifa> deCancha(long canchaId) {
        return jpa.findByCanchaIdOrderByMinutoInicioAscIdAsc(canchaId).stream().map(AdaptadorRepositorioTarifas::aDominio).toList();
    }

    @Override
    public java.util.Map<Long, List<Tarifa>> deCanchas(java.util.Collection<Long> canchaIds) {
        if (canchaIds.isEmpty()) {
            return java.util.Map.of();
        }
        return jpa.findByCanchaIdInOrderByMinutoInicioAscIdAsc(canchaIds).stream().map(AdaptadorRepositorioTarifas::aDominio)
                .collect(java.util.stream.Collectors.groupingBy(Tarifa::canchaId));
    }

    @Override
    public Optional<Tarifa> buscarPorId(long id) {
        return jpa.findById(id).map(AdaptadorRepositorioTarifas::aDominio);
    }

    @Override
    public Tarifa guardar(Tarifa t) {
        TarifaEntidad e = t.id() == null ? new TarifaEntidad() : jpa.findById(t.id()).orElseThrow();
        if (t.id() == null) {
            e.canchaId = t.canchaId();
        }
        e.diasSemana = aArreglo(t.diasSemana());
        e.minutoInicio = t.minutoInicio();
        e.minutoFin = t.minutoFin();
        e.aplicaFestivo = t.aplicaFestivo();
        e.precioHora = t.precioHora();
        e.vigenteDesde = t.vigenteDesde();
        e.vigenteHasta = t.vigenteHasta();
        return aDominio(jpa.saveAndFlush(e));
    }

    @Override
    public void eliminar(long id) {
        jpa.deleteById(id);
    }

    private static short[] aArreglo(Set<DayOfWeek> dias) {
        short[] arreglo = new short[dias.size()];
        int i = 0;
        for (DayOfWeek d : EnumSet.copyOf(dias.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : dias)) {
            arreglo[i++] = (short) d.getValue();
        }
        return arreglo;
    }

    private static Tarifa aDominio(TarifaEntidad e) {
        Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
        for (short d : e.diasSemana) {
            dias.add(DayOfWeek.of(d));
        }
        return new Tarifa(e.id, e.canchaId, dias, e.minutoInicio, e.minutoFin, e.aplicaFestivo, e.precioHora,
                e.vigenteDesde, e.vigenteHasta);
    }
}
