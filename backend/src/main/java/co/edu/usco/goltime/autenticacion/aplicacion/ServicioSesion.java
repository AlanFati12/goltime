package co.edu.usco.goltime.autenticacion.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.autenticacion.dominio.GeneradorTokens;
import co.edu.usco.goltime.autenticacion.dominio.TokenRefresco;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.usuarios.aplicacion.RepositorioUsuarios;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/**
 * Ingreso con Google, renovación con rotación de refresh tokens y cierre de sesión. No hay contraseñas: la identidad
 * la afirma Google (ver {@link VerificadorGoogle}) y la sesión sigue siendo la propia de Goltime.
 */
@Service
public class ServicioSesion {

    private final RepositorioUsuarios usuarios;
    private final RepositorioTokensRefresco tokens;
    private final EmisorTokenAcceso emisor;
    private final VerificadorGoogle google;
    private final PropiedadesAutenticacion propiedades;
    private final Clock reloj;

    public ServicioSesion(RepositorioUsuarios usuarios, RepositorioTokensRefresco tokens, EmisorTokenAcceso emisor,
            VerificadorGoogle google, PropiedadesAutenticacion propiedades, Clock reloj) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.emisor = emisor;
        this.google = google;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    /**
     * Entra con el token de identidad de Google. La cuenta se busca por el identificador de Google; si es la primera
     * vez, por el correo (un administrador autorizado de antemano, o una cuenta anterior) y se vincula; y si el correo
     * es nuevo se crea un cliente. Nadie se vuelve administrador entrando: ese rol solo lo da otro administrador.
     */
    @Transactional
    public ResultadoSesion ingresarConGoogle(String credencial, boolean recordar, String userAgent, String ip) {
        IdentidadGoogle identidad = google.verificar(credencial);
        if (!identidad.correoVerificado()) {
            throw new ExcepcionNegocio(CodigoError.CORREO_NO_VERIFICADO,
                    "Google no confirma que ese correo sea tuyo. Entra con otra cuenta de Google.");
        }
        Instant ahora = reloj.instant();
        Usuario usuario = usuarios.buscarPorGoogleSub(identidad.sub()).orElse(null);
        if (usuario == null) {
            usuario = usuarios.buscarPorCorreo(identidad.correo()).orElse(null);
            if (usuario != null) {
                usuario.vincularGoogle(identidad.sub());
            } else {
                usuario = Usuario.nuevoClienteGoogle(identidad.nombres(), identidad.apellidos(), identidad.correo(),
                        identidad.sub(), ahora);
            }
        }
        if (!usuario.activo()) {
            throw new ExcepcionNegocio(CodigoError.USUARIO_DESACTIVADO,
                    "Tu cuenta está desactivada. Comunícate con el administrador.");
        }
        usuario.registrarIngreso(ahora);
        usuario = usuarios.guardar(usuario);
        Emision emision = emitirRefresco(usuario, UUID.randomUUID(), recordar, userAgent, ip);
        return resultado(usuario, emision);
    }

    /**
     * Rota el refresh token: el presentado queda revocado y se emite uno nuevo de la misma familia.
     * Si el token ya había sido rotado se interpreta como reutilización y se revoca toda la familia.
     */
    @Transactional(noRollbackFor = ExcepcionNegocio.class)
    public ResultadoSesion refrescar(String refrescoPlano, String userAgent, String ip) {
        if (refrescoPlano == null || refrescoPlano.isBlank()) {
            throw sesionInvalida();
        }
        TokenRefresco actual = tokens.buscarPorHashBloqueando(GeneradorTokens.hash(refrescoPlano))
                .orElseThrow(ServicioSesion::sesionInvalida);
        if (actual.fueUsado()) {
            tokens.revocarFamilia(actual.familiaId());
            throw sesionInvalida();
        }
        Instant ahora = reloj.instant();
        if (actual.estaExpirado(ahora)) {
            throw sesionInvalida();
        }
        Usuario usuario = usuarios.buscarPorId(actual.usuarioId()).filter(Usuario::activo)
                .orElseThrow(ServicioSesion::sesionInvalida);

        Emision nueva = emitirRefresco(usuario, actual.familiaId(), actual.recordar(), userAgent, ip);
        actual.rotarA(nueva.token().id());
        tokens.guardar(actual);
        return resultado(usuario, nueva);
    }

    /** Cierra la sesión revocando toda la familia del refresh token. Es idempotente. */
    @Transactional
    public void salir(String refrescoPlano) {
        if (refrescoPlano == null || refrescoPlano.isBlank()) {
            return;
        }
        tokens.buscarPorHashBloqueando(GeneradorTokens.hash(refrescoPlano))
                .ifPresent(t -> tokens.revocarFamilia(t.familiaId()));
    }

    private Emision emitirRefresco(Usuario usuario, UUID familia, boolean recordar, String userAgent, String ip) {
        Duration duracion = recordar
                ? Duration.ofDays(propiedades.refrescoDias())
                : Duration.ofHours(propiedades.refrescoSesionHoras());
        String plano = GeneradorTokens.generar();
        TokenRefresco guardado = tokens.guardar(TokenRefresco.nuevo(usuario.id(), familia,
                GeneradorTokens.hash(plano), reloj.instant().plus(duracion), recordar,
                recortar(userAgent, 300), ip));
        return new Emision(guardado, plano, duracion);
    }

    private ResultadoSesion resultado(Usuario usuario, Emision emision) {
        return new ResultadoSesion(emisor.emitir(usuario), emision.plano(), emision.token().recordar(),
                emision.duracion(), usuario);
    }

    private record Emision(TokenRefresco token, String plano, Duration duracion) {
    }

    private static ExcepcionNegocio sesionInvalida() {
        return new ExcepcionNegocio(CodigoError.SESION_INVALIDA, "Tu sesión expiró. Ingresa de nuevo.");
    }

    private static String recortar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
