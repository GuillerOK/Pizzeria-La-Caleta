package com.lacaleta.service;

import com.lacaleta.dto.CheckoutRequest;
import com.lacaleta.model.*;
import com.lacaleta.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PedidoServiceTests {
    ProductoRepository productos = mock(ProductoRepository.class);
    PedidoRepository pedidos = mock(PedidoRepository.class);
    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    EmailService email = mock(EmailService.class);
    PedidoService service = new PedidoService(productos, pedidos, usuarios, email);
    Producto pizza;

    @BeforeEach void preparar() {
        Usuario usuario = new Usuario();
        usuario.setEmail("cliente@example.com");
        when(usuarios.findById(1)).thenReturn(Optional.of(usuario));
        pizza = new Producto();
        pizza.setNombre("Pizza");
        pizza.setPrecio(25.10);
        pizza.setStock(5);
        when(productos.bloquearPorId(7)).thenReturn(Optional.of(pizza));
    }

    CheckoutRequest solicitud(int... cantidades) {
        return new CheckoutRequest("cliente@example.com", java.util.Arrays.stream(cantidades)
                .mapToObj(c -> new CheckoutRequest.Item(7, c)).toList());
    }

    @Test void sumaDuplicadosYCalculaPreciosDesdeLaBaseDeDatos() {
        Pedido resultado = service.procesarCompra(solicitud(1, 2), 1);
        assertEquals(2, pizza.getStock());
        assertEquals(80.30, resultado.getTotal());
        assertEquals(1, resultado.getDetalles().size());
        assertEquals(3, resultado.getDetalles().get(0).getCantidad());
        assertSame(resultado, resultado.getDetalles().get(0).getPedido());
        var orden = inOrder(pedidos, email);
        orden.verify(pedidos).saveAndFlush(resultado);
        orden.verify(email).enviarBoleta(eq(resultado), any(), any());
    }

    @Test void stockInsuficienteNoGuardaNiEnviaCorreo() {
        assertThrows(ResponseStatusException.class, () -> service.procesarCompra(solicitud(3, 3), 1));
        assertEquals(5, pizza.getStock());
        verifyNoInteractions(pedidos, email);
    }

    @Test void rechazaCantidadesNegativasAntesDeBloquearStock() {
        assertThrows(ResponseStatusException.class, () -> service.procesarCompra(solicitud(-1), 1));
        verifyNoInteractions(productos, pedidos, email);
    }

    @Test void noPermiteCambiarElDestinatarioDeLaSesion() {
        var solicitud = new CheckoutRequest("otro@example.com", List.of(new CheckoutRequest.Item(7, 1)));
        assertThrows(ResponseStatusException.class, () -> service.procesarCompra(solicitud, 1));
        verifyNoInteractions(productos, pedidos, email);
    }

    @Test void propagaFalloDeCorreoParaQueSpringReviertaLaTransaccion() {
        doThrow(new CorreoException(CorreoException.Motivo.CONEXION, null))
                .when(email).enviarBoleta(any(), any(), any());
        assertThrows(CorreoException.class, () -> service.procesarCompra(solicitud(1), 1));
    }

    @Test void boletaRenderizaHtmlEscapadoConImportesYDestinatario() throws Exception {
        var sender = mock(org.springframework.mail.javamail.JavaMailSender.class);
        var mensaje = new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new java.util.Properties()));
        when(sender.createMimeMessage()).thenReturn(mensaje);
        var resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");
        var engine = new org.thymeleaf.spring6.SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var correo = new EmailService();
        org.springframework.test.util.ReflectionTestUtils.setField(correo, "mailSender", sender);
        org.springframework.test.util.ReflectionTestUtils.setField(correo, "templateEngine", engine);
        org.springframework.test.util.ReflectionTestUtils.setField(correo, "remitente", "tienda@example.com");
        Pedido pedido = service.procesarCompra(solicitud(2), 1);
        pedido.setId(42);
        pedido.getUsuario().setNombre("<script>cliente</script>");
        correo.enviarBoleta(pedido, new java.math.BigDecimal("50.20"), PedidoService.COSTO_ENVIO);
        mensaje.saveChanges();
        assertTrue(mensaje.isMimeType("text/html"));
        String html = (String) mensaje.getContent();
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("55.20"));
        assertEquals("cliente@example.com", mensaje.getAllRecipients()[0].toString());
        verify(sender).send(mensaje);
    }
}
