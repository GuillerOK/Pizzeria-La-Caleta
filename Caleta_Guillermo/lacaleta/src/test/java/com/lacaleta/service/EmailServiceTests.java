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
    @Test void boletaRenderizaDescuentoYRegaloConImportesPersistidos() {
        var resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setCharacterEncoding("UTF-8");
        var engine = new org.thymeleaf.spring6.SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var service = new EmailService();
        ReflectionTestUtils.setField(service, "templateEngine", engine);
        var usuario = new com.lacaleta.model.Usuario(); usuario.setNombre("Cliente");
        var pedido = new com.lacaleta.model.Pedido(); pedido.setId(1); pedido.setUsuario(usuario);
        pedido.setSubtotal(25.0); pedido.setEnvio(5.0); pedido.setDescuentoCupon(5.0);
        pedido.setTotal(25.0); pedido.setCodigoCupon("CAL5-TEST");
        pedido.setTituloCupon("Descuento de cinco"); pedido.setTipoCupon("MONTO_FIJO");
        String html = service.renderizarBoleta(pedido, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO);
        assertTrue(html.contains("Cupón Aplicado: -S/ 5.00"));
        assertTrue(html.contains("CAL5-TEST")); assertTrue(html.contains("S/ 25.00"));
        var producto = new com.lacaleta.model.Producto(); producto.setNombre("Inca Kola 1.5L");
        var detalle = new com.lacaleta.model.DetallePedido(pedido, producto, 1, 0.0);
        detalle.setCuponRegalo(true); pedido.getDetalles().add(detalle);
        pedido.setTipoCupon("REGALO"); pedido.setDescuentoCupon(0.0); pedido.setTotal(30.0);
        html = service.renderizarBoleta(pedido, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO);
        assertTrue(html.contains("(CUPÓN REGALO)")); assertTrue(html.contains("Inca Kola 1.5L"));
        assertTrue(html.contains("S/ 0.00")); assertFalse(html.contains("Cupón Aplicado: -S/"));
    }
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
