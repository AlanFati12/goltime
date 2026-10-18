package co.edu.usco.goltime.autenticacion.aplicacion;

import java.time.Clock;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.autenticacion.dominio.GeneradorTokens;
import co.edu.usco.goltime.autenticacion.dominio.TipoTokenVerificacion;
import co.edu.usco.goltime.autenticacion.dominio.TokenVerificacion;
import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo;
import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo.MensajeCorreo;
import co.edu.usco.goltime.usuarios.aplicacion.RepositorioUsuarios;
import co.edu.usco.goltime.usuarios.dominio.ClaveCambiadaEvento;
import co.edu.usco.goltime.usuarios.dominio.PoliticaClave;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/** Recuperación de contraseña por correo con token de un solo uso que vence a los 30 minutos. */
@Service
public class ServicioRecuperacionClave {

    private final RepositorioUsuarios usuarios;
    private final RepositorioTokensVerificacion tokens;
    private final PasswordEncoder codificador;
    private final EnviadorCorreo correo;
    private final ApplicationEventPublisher eventos;
    private final PropiedadesAutenticacion propiedades;
    private final Clock reloj;

    public ServicioRecuperacionClave(RepositorioUsuarios usuarios, RepositorioTokensVerificacion tokens,
            PasswordEncoder codificador, EnviadorCorreo correo, ApplicationEventPublisher eventos,
            PropiedadesAutenticacion propiedades, Clock reloj) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.codificador = codificador;
        this.correo = correo;
        this.eventos = eventos;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    /**
     * Genera el enlace y lo envía. No devuelve nada ni falla si el correo no existe, para no
     * revelar qué correos están registrados.
     */
    @Transactional
    public void solicitar(String correoSolicitado) {
        usuarios.buscarPorCorreo(correoSolicitado).filter(Usuario::activo).ifPresent(usuario -> {
            String plano = GeneradorTokens.generar();
            tokens.invalidarPendientes(usuario.id(), TipoTokenVerificacion.RESTABLECER_CLAVE, reloj.instant());
            tokens.guardar(TokenVerificacion.nuevo(usuario.id(), TipoTokenVerificacion.RESTABLECER_CLAVE,
                    GeneradorTokens.hash(plano), reloj.instant().plus(ServicioRegistro.VIGENCIA_TOKEN)));
            correo.enviar(new MensajeCorreo(usuario.correo(), "Restablece tu contraseña de Goltime",
                    "restablecer-clave", Map.of("nombre", usuario.nombres(),
                            "enlace", propiedades.urlPublica() + "/restablecer-clave?token=" + plano)));
        });
    }

    @Transactional
    public void restablecer(String tokenPlano, String claveNueva, String confirmacion) {
        if (!claveNueva.equals(confirmacion)) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La confirmación no coincide con la contraseña.");
        }
        PoliticaClave.validar(claveNueva);
        TokenVerificacion token = tokens.buscarPorHash(GeneradorTokens.hash(tokenPlano))
                .filter(t -> t.tipo() == TipoTokenVerificacion.RESTABLECER_CLAVE)
                .filter(t -> t.estaVigente(reloj.instant()))
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.TOKEN_INVALIDO,
                        "El enlace no es válido o ya venció. Solicita uno nuevo."));
        Usuario usuario = usuarios.buscarPorId(token.usuarioId()).filter(Usuario::activo)
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.TOKEN_INVALIDO, "El enlace no es válido."));
        usuario.cambiarClave(codificador.encode(claveNueva));
        usuarios.guardar(usuario);
        token.marcarUsado(reloj.instant());
        tokens.guardar(token);
        eventos.publishEvent(new ClaveCambiadaEvento(usuario.id()));
    }
}
