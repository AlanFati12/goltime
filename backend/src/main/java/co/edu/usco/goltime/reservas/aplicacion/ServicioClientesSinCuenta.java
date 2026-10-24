package co.edu.usco.goltime.reservas.aplicacion;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.usco.goltime.compartido.dominio.CodigoError;
import co.edu.usco.goltime.compartido.dominio.ExcepcionNegocio;
import co.edu.usco.goltime.compartido.dominio.LidWhatsapp;
import co.edu.usco.goltime.compartido.dominio.NumeroTelefono;
import co.edu.usco.goltime.reservas.aplicacion.RepositorioMostrador.ClienteMostrador;

/**
 * Clientes sin cuenta (los de mostrador) vistos desde otros módulos: el bot de WhatsApp los reconoce por su
 * número y los registra la primera vez que piden una reserva (ADR 0018).
 */
@Service
public class ServicioClientesSinCuenta {

    private final RepositorioMostrador mostrador;

    public ServicioClientesSinCuenta(RepositorioMostrador mostrador) {
        this.mostrador = mostrador;
    }

    @Transactional(readOnly = true)
    public Optional<ClienteMostrador> buscarPorTelefono(String telefono) {
        return mostrador.buscarPorTelefono(NumeroTelefono.de(telefono).valor());
    }

    @Transactional(readOnly = true)
    public Optional<ClienteMostrador> buscarPorLid(String lid) {
        return mostrador.buscarPorLid(LidWhatsapp.de(lid).valor());
    }

    /** Reutiliza al cliente con ese número o lo crea con el nombre dado. */
    @Transactional
    public ClienteMostrador registrar(String nombre, String telefono) {
        String limpio = nombreValido(nombre);
        String numero = NumeroTelefono.de(telefono).valor();
        mostrador.buscarOCrear(limpio, numero);
        return mostrador.buscarPorTelefono(numero).orElseThrow();
    }

    /**
     * Quien escribe desde un @lid sin cuenta queda atado a ese @lid: solo desde ese WhatsApp verá o cancelará sus
     * reservas. El teléfono es para que el administrador pueda contactarlo (no está verificado).
     */
    @Transactional
    public ClienteMostrador registrarConLid(String nombre, String telefonoContacto, String lid) {
        String limpio = nombreValido(nombre);
        if (telefonoContacto == null || telefonoContacto.isBlank()) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Necesitamos un celular de contacto para la reserva.");
        }
        String numero = NumeroTelefono.de(telefonoContacto).valor();
        String valor = LidWhatsapp.de(lid).valor();
        return mostrador.buscarPorLid(valor).orElseGet(() -> new ClienteMostrador(mostrador.crearConLid(limpio, numero, valor), limpio, numero));
    }

    /** A dónde avisarle por WhatsApp: a su chat @lid si reservó desde uno; si no, a su teléfono. */
    @Transactional(readOnly = true)
    public Optional<String> destinoWhatsapp(long clienteMostradorId, String telefono) {
        return mostrador.lidDe(clienteMostradorId).map(l -> new LidWhatsapp(l).jid()).or(() -> Optional.ofNullable(telefono));
    }

    private static String nombreValido(String nombre) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() < 3 || limpio.length() > 150) {
            throw new ExcepcionNegocio(CodigoError.DATOS_INVALIDOS, "Necesitamos tu nombre (entre 3 y 150 caracteres) para la reserva.");
        }
        return limpio;
    }
}
