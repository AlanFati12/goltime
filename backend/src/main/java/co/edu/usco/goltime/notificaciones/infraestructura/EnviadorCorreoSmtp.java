package co.edu.usco.goltime.notificaciones.infraestructura;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import co.edu.usco.goltime.notificaciones.aplicacion.EnviadorCorreo;
import jakarta.mail.internet.MimeMessage;

/** Envía correos HTML por SMTP (Mailpit en desarrollo). Los errores se registran y no se propagan. */
@Component
class EnviadorCorreoSmtp implements EnviadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(EnviadorCorreoSmtp.class);

    private final JavaMailSender remitente;
    private final SpringTemplateEngine plantillas;
    private final String direccionRemitente;

    EnviadorCorreoSmtp(JavaMailSender remitente, SpringTemplateEngine plantillas,
            @Value("${goltime.correo.remitente}") String direccionRemitente) {
        this.remitente = remitente;
        this.plantillas = plantillas;
        this.direccionRemitente = direccionRemitente;
    }

    @Async
    @Override
    public void enviar(MensajeCorreo mensaje) {
        try {
            Context contexto = new Context();
            contexto.setVariables(mensaje.variables());
            String html = plantillas.process("correo/" + mensaje.plantilla(), contexto);

            MimeMessage mime = remitente.createMimeMessage();
            MimeMessageHelper ayudante = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            ayudante.setFrom(direccionRemitente);
            ayudante.setTo(mensaje.destinatario());
            ayudante.setSubject(mensaje.asunto());
            ayudante.setText(html, true);
            remitente.send(mime);
        } catch (Exception ex) {
            log.warn("No se pudo enviar el correo '{}' a {}: {}", mensaje.asunto(), mensaje.destinatario(),
                    ex.getMessage());
        }
    }
}
