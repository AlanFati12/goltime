package co.edu.usco.goltime.auditoria.aplicacion;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.auditoria.aplicacion.ConsultaAuditoria.FiltroAuditoria;
import co.edu.usco.goltime.auditoria.aplicacion.ConsultaAuditoria.RegistroAuditado;
import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;

@Service
public class ServicioConsultaAuditoria {

    private final ConsultaAuditoria consulta;

    public ServicioConsultaAuditoria(ConsultaAuditoria consulta) {
        this.consulta = consulta;
    }

    @Transactional(readOnly = true)
    public Pagina<RegistroAuditado> buscar(FiltroAuditoria filtro, SolicitudPagina pagina) {
        return consulta.buscar(filtro, pagina);
    }

    @Transactional(readOnly = true)
    public List<String> entidades() {
        return consulta.entidades();
    }

    @Transactional(readOnly = true)
    public List<String> acciones() {
        return consulta.acciones();
    }
}
