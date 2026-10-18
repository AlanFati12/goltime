package co.edu.usco.goltime.autenticacion.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.autenticacion.dominio.GeneradorTokens;
import co.edu.usco.goltime.autenticacion.dominio.TokenRefresco;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.usuarios.aplicacion.RepositorioUsuarios;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/** Ingreso, renovación con rotación de refresh tokens y cierre de sesión. */
@Service
public class ServicioSesion {

    private static final String MENSAJE_CREDENCIALES = "Correo o contraseña incorrectos.";

    private final RepositorioUsuarios usuarios;
    private final RepositorioTokensRefresco tokens;
    private final EmisorTokenAcceso emisor;
    private final ControlIntentos intentos;
    private final PasswordEncoder codificador;
    private final PropiedadesAutenticacion propiedades;
    private final Clock reloj;
    /** Hash válido de una clave cualquiera: se compara cuando el correo no existe para igualar tiempos. */
    private final String hashFalso;

    public ServicioSesion(RepositorioUsuarios usuarios, RepositorioTokensRefresco tokens, EmisorTokenAcceso emisor,
            ControlIntentos intentos, PasswordEncoder codificador, PropiedadesAutenticacion propiedades, Clock reloj) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.emisor = emisor;
        this.intentos = intentos;
        this.codificador = codificador;
        this.propiedades = propiedades;
        this.reloj = reloj;
        this.hashFalso = codificador.encode(GeneradorTokens.generar());
    }

    @Transactional
    public ResultadoSesion ingresar(String correo, String clave, boolean recordar, String userAgent, String ip) {
        String claveIntentos = Usuario.normalizarCorreo(correo) + "|" + ip;
        if (intentos.estaBloqueado(claveIntentos)) {
            throw new ExcepcionNegocio(CodigoError.DEMASIADOS_INTENTOS,
                    "Demasiados intentos fallidos. Espera unos minutos e intenta de nuevo.");
        }
        Usuario usuario = usuarios.buscarPorCorreo(correo).orElse(null);
        String hash = usuario != null ? usuario.claveHash() : hashFalso;
        boolean coincide = codificador.matches(clave, hash);
        if (usuario == null || !coincide) {
            intentos.registrarFallo(claveIntentos);
            throw new ExcepcionNegocio(CodigoError.CREDENCIALES_INVALIDAS, MENSAJE_CREDENCIALES);
        }
        // Solo con credenciales correctas se revela el estado de la cuenta.
        if (!usuario.activo()) {
            throw new ExcepcionNegocio(CodigoError.USUARIO_DESACTIVADO,
                    "Tu cuenta está desactivada. Comunícate con el administrador.");
        }
        if (propiedades.verificacionCorreoObligatoria() && !usuario.correoVerificado()) {
            throw new ExcepcionNegocio(CodigoError.CORREO_NO_VERIFICADO,
                    "Debes verificar tu correo antes de ingresar. Revisa tu bandeja de entrada.");
        }
        intentos.limpiar(claveIntentos);
        Instant ahora = reloj.instant();
        usuario.registrarIngreso(ahora);
        usuarios.guardar(usuario);
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
