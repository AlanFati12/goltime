package co.edu.usco.goltime.autenticacion.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

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
import co.edu.usco.goltime.usuarios.dominio.PoliticaClave;
import co.edu.usco.goltime.usuarios.dominio.Usuario;

/** Registro público de clientes y verificación del correo. */
@Service
public class ServicioRegistro {

    static final Duration VIGENCIA_TOKEN = Duration.ofMinutes(30);

    private final RepositorioUsuarios usuarios;
    private final RepositorioTokensVerificacion tokens;
    private final PasswordEncoder codificador;
    private final EnviadorCorreo correo;
    private final PropiedadesAutenticacion propiedades;
    private final Clock reloj;

    public ServicioRegistro(RepositorioUsuarios usuarios, RepositorioTokensVerificacion tokens,
            PasswordEncoder codificador, EnviadorCorreo correo, PropiedadesAutenticacion propiedades, Clock reloj) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.codificador = codificador;
        this.correo = correo;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Transactional
    public Usuario registrar(DatosRegistro datos) {
        if (!datos.clave().equals(datos.confirmacionClave())) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "La confirmación no coincide con la contraseña.");
        }
        PoliticaClave.validar(datos.clave());
        if (usuarios.existePorCorreo(datos.correo())) {
            throw new ExcepcionNegocio(CodigoError.CORREO_YA_REGISTRADO, "Ya existe una cuenta con ese correo.");
        }
        Usuario cliente = Usuario.nuevoCliente(datos.nombres(), datos.apellidos(), datos.correo(), datos.telefono(),
                datos.documento(), codificador.encode(datos.clave()), datos.aceptaTerminos() ? reloj.instant() : null);
        Usuario guardado = usuarios.guardar(cliente);
        enviarVerificacion(guardado);
        return guardado;
    }

    /** Marca el correo como verificado con un token de un solo uso. */
    @Transactional
    public void verificarCorreo(String tokenPlano) {
        TokenVerificacion token = tokens.buscarPorHash(GeneradorTokens.hash(tokenPlano))
                .filter(t -> t.tipo() == TipoTokenVerificacion.VERIFICAR_CORREO)
                .filter(t -> t.estaVigente(reloj.instant()))
                .orElseThrow(() -> new ExcepcionNegocio(CodigoError.TOKEN_INVALIDO,
                        "El enlace no es válido o ya venció."));
        Usuario usuario = usuarios.buscarPorId(token.usuarioId()).orElseThrow();
        usuario.verificarCorreo();
        usuarios.guardar(usuario);
        token.marcarUsado(reloj.instant());
        tokens.guardar(token);
    }

    private void enviarVerificacion(Usuario usuario) {
        String plano = GeneradorTokens.generar();
        tokens.invalidarPendientes(usuario.id(), TipoTokenVerificacion.VERIFICAR_CORREO, reloj.instant());
        tokens.guardar(TokenVerificacion.nuevo(usuario.id(), TipoTokenVerificacion.VERIFICAR_CORREO,
                GeneradorTokens.hash(plano), reloj.instant().plus(VIGENCIA_TOKEN)));
        correo.enviar(new MensajeCorreo(usuario.correo(), "Verifica tu correo en Goltime", "verificar-correo",
                Map.of("nombre", usuario.nombres(),
                        "enlace", propiedades.urlPublica() + "/verificar-correo?token=" + plano)));
    }

    /** Datos del formulario de registro. */
    public record DatosRegistro(String nombres, String apellidos, String correo, String telefono, String documento,
            String clave, String confirmacionClave, boolean aceptaTerminos) {
    }
}
