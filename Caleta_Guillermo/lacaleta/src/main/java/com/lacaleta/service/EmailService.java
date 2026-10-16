package com.lacaleta.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.SendFailedException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final Pattern SMTP_STATUS = Pattern.compile("[45]\\d{2}[- ]\\d\\.\\d\\.\\d");

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remitente;

    @Autowired
    private org.thymeleaf.TemplateEngine templateEngine;

    public void enviarBoleta(com.lacaleta.model.Pedido pedido, java.math.BigDecimal subtotal,
                             java.math.BigDecimal envio) {
        if (mailSender == null) throw new CorreoException(CorreoException.Motivo.CONFIGURACION, null);
        try {
            var contexto = new org.thymeleaf.context.Context(Locale.forLanguageTag("es-PE"));
            contexto.setVariable("pedido", pedido);
            contexto.setVariable("fecha", pedido.getFecha().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            contexto.setVariable("items", pedido.getDetalles());
            contexto.setVariable("subtotal", subtotal);
            contexto.setVariable("envio", envio);
            String html = templateEngine.process("boleta-email", contexto);
            var mensaje = mailSender.createMimeMessage();
            var helper = new org.springframework.mail.javamail.MimeMessageHelper(mensaje, "UTF-8");
            if (remitente != null && !remitente.isBlank()) helper.setFrom(remitente);
            helper.setTo(pedido.getUsuario().getEmail());
            helper.setSubject("La Caleta - Boleta del pedido #" + pedido.getId());
            helper.setText(html, true);
            mailSender.send(mensaje);
        } catch (Exception error) {
            throw new CorreoException(identificarMotivo(error, 0), error);
        }
    }

    public boolean enviarCodigoVerificacion(String destino, String codigo) {
        return enviarMensaje(destino, "La Caleta - Verifica tu cuenta",
                "Hola,\n\nTu código para activar tu cuenta en La Caleta es: "
                    + codigo + "\n\nVence en 10 minutos y solo puede utilizarse una vez."
                    + "\n\nSi no creaste esta cuenta, ignora este mensaje.");
    }

    public boolean enviarCodigoRecuperacion(String destino, String codigo) {
        return enviarMensaje(destino, "La Caleta - Código de Recuperación de Contraseña",
                "Hola,\n\nTu código de verificación para restablecer tu contraseña en La Caleta es: "
                        + codigo + "\n\nEste código vencerá en 10 minutos.\n\nSi no solicitaste este cambio, ignora este mensaje.");
    }

    private boolean enviarMensaje(String destino, String asunto, String contenido) {
        if (mailSender == null) {
            log.error("CORREO_CONFIGURACION: no existe un JavaMailSender en la aplicación.");
            throw new CorreoException(CorreoException.Motivo.CONFIGURACION, null);
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            if (remitente != null && !remitente.isBlank()) {
                mensaje.setFrom(remitente);
            }
            mensaje.setTo(destino);
            mensaje.setSubject(asunto);
            mensaje.setText(contenido);
            mailSender.send(mensaje);
            return true;
        } catch (Exception e) {
            CorreoException.Motivo motivo = identificarMotivo(e, 0);
            Throwable detalle = e;
            if (e instanceof MailSendException envio && envio.getMessageExceptions().length > 0) {
                detalle = envio.getMessageExceptions()[0];
            }
            while (detalle.getCause() != null && detalle.getCause() != detalle) detalle = detalle.getCause();
            var status = SMTP_STATUS.matcher(String.valueOf(detalle.getMessage()));
            // No registrar la contraseña, el OTP, el cuerpo del correo ni el destinatario.
            log.error("CORREO_{}: tipo={}, causa={}, SMTP={}", motivo,
                    e.getClass().getSimpleName(), detalle.getClass().getSimpleName(),
                    status.find() ? status.group() : "sin código de respuesta");
            throw new CorreoException(motivo, e);
        }
    }

    private CorreoException.Motivo identificarMotivo(Throwable error, int profundidad) {
        if (error == null || profundidad > 12) return CorreoException.Motivo.ENVIO;
        if (error instanceof MailAuthenticationException || error instanceof AuthenticationFailedException) {
            return CorreoException.Motivo.AUTENTICACION;
        }
        if (error instanceof ConnectException || error instanceof SocketTimeoutException
                || error instanceof UnknownHostException || error instanceof SSLException) {
            return CorreoException.Motivo.CONEXION;
        }
        if (error instanceof MailParseException || (error instanceof SendFailedException fallo
                && fallo.getInvalidAddresses() != null && fallo.getInvalidAddresses().length > 0)) {
            return CorreoException.Motivo.DESTINATARIO;
        }
        String mensaje = String.valueOf(error.getMessage()).toLowerCase(Locale.ROOT);
        if (mensaje.contains("quota") || mensaje.contains("daily user sending limit")
                || mensaje.contains("rate limit")) return CorreoException.Motivo.LIMITE;
        if (error instanceof MailSendException envio) {
            for (Exception interno : envio.getMessageExceptions()) {
                CorreoException.Motivo motivo = identificarMotivo(interno, profundidad + 1);
                if (motivo != CorreoException.Motivo.ENVIO) return motivo;
            }
        }
        return identificarMotivo(error.getCause(), profundidad + 1);
    }
}
