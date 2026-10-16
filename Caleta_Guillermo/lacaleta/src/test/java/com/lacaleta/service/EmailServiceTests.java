package com.lacaleta.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.SocketTimeoutException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.InternetAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTests {
    @Test
    void registroEnviaCodigoPorSmtpAlDestinatarioReal() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService();
        ReflectionTestUtils.setField(service, "mailSender", sender);
        ReflectionTestUtils.setField(service, "remitente", "soporte@example.com");
        assertTrue(service.enviarCodigoVerificacion("cliente@example.com", "012345"));
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        SimpleMailMessage mensaje = captor.getValue();
        assertArrayEquals(new String[]{"cliente@example.com"}, mensaje.getTo());
        assertEquals("soporte@example.com", mensaje.getFrom());
        assertTrue(mensaje.getSubject().contains("Verifica tu cuenta"));
        assertTrue(mensaje.getText().contains("012345"));
    }

    @Test
    void smtpAusenteNoSimulaEnvioEnNingunoDeLosFlujos() {
        EmailService service = new EmailService();
        assertEquals(CorreoException.Motivo.CONFIGURACION, assertThrows(CorreoException.class,
                () -> service.enviarCodigoVerificacion("cliente@example.com", "012345")).getMotivo());
        assertThrows(CorreoException.class,
                () -> service.enviarCodigoRecuperacion("cliente@example.com", "012345"));
    }

    @Test
    void falloDelServidorSmtpNoSeReportaComoExito() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("sin conexión")).when(sender).send(any(SimpleMailMessage.class));
        EmailService service = new EmailService();
        ReflectionTestUtils.setField(service, "mailSender", sender);
        assertEquals(CorreoException.Motivo.ENVIO, assertThrows(CorreoException.class,
                () -> service.enviarCodigoVerificacion("cliente@example.com", "012345")).getMotivo());
    }

    @Test
    void distingueAutenticacionDeTimeoutSinExponerDetallesDelProveedor() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService();
        ReflectionTestUtils.setField(service, "mailSender", sender);
        doThrow(new MailAuthenticationException("detalle privado del proveedor"))
                .when(sender).send(any(SimpleMailMessage.class));
        CorreoException auth = assertThrows(CorreoException.class,
                () -> service.enviarCodigoVerificacion("cliente@example.com", "012345"));
        assertEquals(CorreoException.Motivo.AUTENTICACION, auth.getMotivo());
        assertFalse(auth.getMessage().contains("detalle privado"));
        doThrow(new MailSendException("fallo", new SocketTimeoutException("timeout")))
                .when(sender).send(any(SimpleMailMessage.class));
        assertEquals(CorreoException.Motivo.CONEXION, assertThrows(CorreoException.class,
                () -> service.enviarCodigoVerificacion("cliente@example.com", "012345")).getMotivo());
    }

    @Test
    void identificaDestinatarioRechazadoDentroDeMailSendException() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = new EmailService();
        ReflectionTestUtils.setField(service, "mailSender", sender);
        SendFailedException direccion = new SendFailedException("rechazado", null, null, null,
                new InternetAddress[]{new InternetAddress("cliente@example.com")});
        MailSendException smtp = new MailSendException("fallo", null,
                java.util.Map.of(new SimpleMailMessage(), direccion));
        doThrow(smtp).when(sender).send(any(SimpleMailMessage.class));
        assertEquals(CorreoException.Motivo.DESTINATARIO, assertThrows(CorreoException.class,
                () -> service.enviarCodigoVerificacion("cliente@example.com", "012345")).getMotivo());
    }
}
