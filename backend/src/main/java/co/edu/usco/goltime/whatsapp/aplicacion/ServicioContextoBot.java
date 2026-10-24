package co.edu.usco.goltime.whatsapp.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.usco.goltime.autenticacion.aplicacion.ServicioVerificacionTelefono.Situacion;
import co.edu.usco.goltime.compartido.dominio.FormatoTexto;
import co.edu.usco.goltime.reservas.aplicacion.ConfiguracionReservas;
import co.edu.usco.goltime.reservas.dominio.PoliticaReservas;
import co.edu.usco.goltime.sedes.aplicacion.ServicioSedes;
import co.edu.usco.goltime.whatsapp.aplicacion.ServicioBotWhatsapp.Remitente;

/**
 * Lo que el bot necesita saber al recibir cada mensaje: quién escribe, qué día y hora es en Bogotá, las sedes y
 * las políticas vigentes (las mismas que configura el administrador para la web).
 */
@Service
public class ServicioContextoBot {

    private final ServicioBotWhatsapp bot;
    private final ServicioSedes sedes;
    private final ConfiguracionReservas reservas;
    private final ConfiguracionBot configuracion;
    private final Clock reloj;

    public ServicioContextoBot(ServicioBotWhatsapp bot, ServicioSedes sedes, ConfiguracionReservas reservas, ConfiguracionBot configuracion,
            Clock reloj) {
        this.bot = bot;
        this.sedes = sedes;
        this.reservas = reservas;
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    public record Sede(long id, String nombre, String direccion, String ciudad) {
    }

    public record Politicas(int horasMinimasCancelacion, int maxReprogramaciones, int antelacionMinimaHoras, int antelacionMaximaDias,
            int horasExpiracionAprobacion) {
    }

    /**
     * @param situacion CLIENTE (cuenta con el número vinculado), SIN_VERIFICAR (hay cuenta pero falta vincular),
     *                  SIN_CUENTA o DESACTIVADO
     */
    public record Contexto(boolean botActivo, Situacion situacion, String nombre, boolean puedeReservarSinCuenta, String fecha, String dia,
            String hora, List<Sede> sedes, Politicas politicas, String contacto) {
    }

    public Contexto de(String numero) {
        Remitente remitente = bot.identificar(numero);
        Instant ahora = reloj.instant();
        LocalDate hoy = ServicioConsultasBot.hoy(ahora);
        PoliticaReservas p = reservas.politica();
        List<Sede> lista = sedes.listarPublicas().stream()
                .map(v -> new Sede(v.sede().id(), v.sede().nombre(), v.sede().direccion(), v.sede().ciudad())).toList();
        return new Contexto(configuracion.activo(), remitente.situacion(), remitente.nombre(), remitente.conReservasSinCuenta(),
                hoy.toString(), ServicioConsultasBot.dia(hoy), FormatoTexto.hora(ahora), lista,
                new Politicas(p.horasMinCancelacion(), p.maxReprogramaciones(), p.antelacionMinimaHoras(), p.antelacionMaximaDias(),
                        p.horasExpiracionAprobacion()),
                configuracion.contactoNegocio());
    }
}
