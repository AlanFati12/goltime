package co.edu.usco.goltime.usuarios.infraestructura;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import co.edu.usco.goltime.compartido.dominio.Pagina;
import co.edu.usco.goltime.compartido.dominio.Rol;
import co.edu.usco.goltime.compartido.dominio.SolicitudPagina;
import co.edu.usco.goltime.usuarios.aplicacion.FiltroClientes;
import co.edu.usco.goltime.usuarios.aplicacion.RepositorioUsuarios;
import co.edu.usco.goltime.usuarios.dominio.Usuario;
import jakarta.persistence.criteria.Predicate;

/** Adaptador JPA del puerto {@link RepositorioUsuarios}: convierte entre entidad y dominio. */
@Repository
class AdaptadorRepositorioUsuarios implements RepositorioUsuarios {

    private final RepositorioUsuariosJpa jpa;

    AdaptadorRepositorioUsuarios(RepositorioUsuariosJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Usuario> buscarPorId(long id) {
        return jpa.findById(id).map(AdaptadorRepositorioUsuarios::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorGoogleSub(String sub) {
        return jpa.findByGoogleSub(sub).map(AdaptadorRepositorioUsuarios::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return jpa.buscarPorCorreo(correo.trim()).map(AdaptadorRepositorioUsuarios::aDominio);
    }

    @Override
    public boolean existePorCorreo(String correo) {
        return jpa.existePorCorreo(correo.trim());
    }

    @Override
    public List<Usuario> buscarPorTelefono(String telefono) {
        return jpa.findByTelefonoOrderByIdAsc(telefono).stream().map(AdaptadorRepositorioUsuarios::aDominio).toList();
    }

    @Override
    public Optional<Usuario> buscarPorWhatsappLid(String lid) {
        return jpa.findByWhatsappLid(lid).map(AdaptadorRepositorioUsuarios::aDominio);
    }

    @Override
    public boolean existeTelefonoVerificado(String telefono, Long excluirId) {
        return jpa.existeTelefonoVerificado(telefono, excluirId);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntidad entidad = usuario.id() == null
                ? nuevaEntidad(usuario)
                : jpa.findById(usuario.id()).orElseThrow();
        copiarAEntidad(usuario, entidad);
        return aDominio(jpa.saveAndFlush(entidad));
    }

    @Override
    public Pagina<Usuario> buscarClientes(FiltroClientes filtro, SolicitudPagina pagina) {
        Specification<UsuarioEntidad> especificacion = (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(raiz.get("rol"), Rol.CLIENTE));
            if (filtro.activo() != null) {
                condiciones.add(cb.equal(raiz.get("activo"), filtro.activo()));
            }
            if (filtro.busqueda() != null && !filtro.busqueda().isBlank()) {
                String patron = "%" + filtro.busqueda().trim().toLowerCase() + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(raiz.get("nombres")), patron),
                        cb.like(cb.lower(raiz.get("apellidos")), patron),
                        cb.like(cb.lower(raiz.get("correo")), patron),
                        cb.like(cb.lower(cb.coalesce(raiz.<String>get("telefono"), "")), patron),
                        cb.like(cb.lower(cb.coalesce(raiz.<String>get("documento"), "")), patron)));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        Page<UsuarioEntidad> resultado = jpa.findAll(especificacion,
                PageRequest.of(pagina.pagina(), pagina.tamano(), Sort.by(Sort.Direction.DESC, "creadoEn", "id")));
        return new Pagina<>(resultado.getContent().stream().map(AdaptadorRepositorioUsuarios::aDominio).toList(),
                resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages());
    }

    @Override
    public List<Usuario> listarAdministradores() {
        return jpa.findByRolOrderByNombresAsc(Rol.ADMIN).stream().map(AdaptadorRepositorioUsuarios::aDominio).toList();
    }

    private static UsuarioEntidad nuevaEntidad(Usuario usuario) {
        UsuarioEntidad entidad = new UsuarioEntidad();
        entidad.setRol(usuario.rol());
        return entidad;
    }

    private static void copiarAEntidad(Usuario u, UsuarioEntidad e) {
        e.setNombres(u.nombres());
        e.setApellidos(u.apellidos());
        e.setCorreo(u.correo());
        e.setTelefono(u.telefono());
        e.setTelefonoVerificado(u.telefonoVerificado());
        e.setWhatsappLid(u.whatsappLid());
        e.setDocumento(u.documento());
        e.setGoogleSub(u.googleSub());
        e.setActivo(u.activo());
        e.setCorreoVerificado(u.correoVerificado());
        e.setMotivoDesactivacion(u.motivoDesactivacion());
        e.setDesactivadoPor(u.desactivadoPor());
        e.setFechaDesactivacion(u.fechaDesactivacion());
        e.setVersionToken(u.versionToken());
        e.setUltimoLogin(u.ultimoLogin());
        e.setAceptoTerminosEn(u.aceptoTerminosEn());
        e.setPuedeInscribirEquipos(u.puedeInscribirEquipos());
    }

    static Usuario aDominio(UsuarioEntidad e) {
        return Usuario.reconstruir(e.getId(), e.getNombres(), e.getApellidos(), e.getCorreo(), e.getTelefono(),
                e.isTelefonoVerificado(), e.getWhatsappLid(), e.getDocumento(), e.getGoogleSub(), e.getRol(), e.isActivo(), e.isCorreoVerificado(),
                e.getMotivoDesactivacion(), e.getDesactivadoPor(), e.getFechaDesactivacion(), e.getVersionToken(),
                e.getUltimoLogin(), e.getAceptoTerminosEn(), e.isPuedeInscribirEquipos(), e.getCreadoEn());
    }
}
