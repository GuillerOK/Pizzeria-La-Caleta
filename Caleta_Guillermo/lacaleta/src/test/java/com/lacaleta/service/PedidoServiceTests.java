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
    BoletaOutboxService boletas = mock(BoletaOutboxService.class);
    RecetaInsumoRepository recetas = mock(RecetaInsumoRepository.class);
    InsumoRepository insumos = mock(InsumoRepository.class);
    PedidoService service = new PedidoService(productos, pedidos, usuarios, boletas, recetas, insumos);
    Producto pizza;
    Insumo harina;

    @BeforeEach void preparar() {
        Usuario usuario = new Usuario();
        usuario.setEmail("cliente@example.com");
        usuario.setVerificado(true);
        when(usuarios.bloquearPorId(1)).thenReturn(Optional.of(usuario));
        pizza = new Producto();
        pizza.setNombre("Pizza");
        pizza.setPrecio(25.10);
        pizza.setStock(5);
        pizza.setId(7);
        when(productos.bloquearPorId(7)).thenReturn(Optional.of(pizza));
        harina = new Insumo(3, "INS-03", "Harina", 2.0, "kg", 0.5);
        var linea = new RecetaInsumo();
        linea.setProducto(pizza); linea.setInsumo(harina);
        linea.setCantidad(new java.math.BigDecimal("0.25"));
        when(recetas.findByProductoIdIn(anyCollection())).thenReturn(List.of(linea));
        when(insumos.bloquearPorId(3)).thenReturn(Optional.of(harina));
    }

    CheckoutRequest solicitud(int... cantidades) {
        return new CheckoutRequest("cliente@example.com", java.util.Arrays.stream(cantidades)
                .mapToObj(c -> new CheckoutRequest.Item(7, c)).toList(),
                "EFECTIVO", "DELIVERY", "Av. Lima 123", "a1234567-1234-1234-1234-123456789abc");
    }

    @Test void sumaDuplicadosYCalculaPreciosDesdeLaBaseDeDatos() {
        Pedido resultado = service.procesarCompra(solicitud(1, 2), 1);
        assertEquals(2, pizza.getStock());
        assertEquals(1.25, harina.getStockActual());
        assertEquals(80.30, resultado.getTotal());
        assertEquals("CONTRA_ENTREGA", resultado.getMetodoPago());
        assertEquals(1, resultado.getDetalles().size());
        assertEquals(3, resultado.getDetalles().get(0).getCantidad());
        assertSame(resultado, resultado.getDetalles().get(0).getPedido());
        var orden = inOrder(pedidos, boletas);
        orden.verify(pedidos).saveAndFlush(resultado);
        orden.verify(boletas).encolar(eq(resultado), any(), any());
        verifyNoInteractions(email);
    }

    @Test void stockInsuficienteNoGuardaNiEnviaCorreo() {
        assertThrows(ResponseStatusException.class, () -> service.procesarCompra(solicitud(3, 3), 1));
        assertEquals(5, pizza.getStock());
        verify(pedidos, never()).saveAndFlush(any());
        verifyNoInteractions(boletas);
    }

    @Test void rechazaCantidadesNegativasAntesDeBloquearStock() {
        assertThrows(ResponseStatusException.class, () -> service.procesarCompra(solicitud(-1), 1));
        verifyNoInteractions(productos, boletas);
    }

    @Test void noPermiteCambiarElDestinatarioDeLaSesion() {
        var solicitud = new CheckoutRequest("otro@example.com", List.of(new CheckoutRequest.Item(7, 1)),
                "EFECTIVO", "DELIVERY", "Av. Lima 123", "a1234567-1234-1234-1234-123456789abc");
        assertThrows(ResponseStatusException.class, () -> service.procesarCompra(solicitud, 1));
        verifyNoInteractions(productos, pedidos, email);
    }

    @Test void propagaFalloDePreparacionDeBoletaParaRevertirLaTransaccion() {
        doThrow(new CorreoException(CorreoException.Motivo.CONEXION, null))
                .when(boletas).encolar(any(), any(), any());
        assertThrows(CorreoException.class, () -> service.procesarCompra(solicitud(1), 1));
    }

    @Test void insumoInsuficienteNoGuardaPedidoNiBoleta() {
        harina.setStockActual(0.1);
        assertThrows(ResponseStatusException.class, () -> service.procesarCompra(solicitud(1), 1));
        verify(pedidos, never()).saveAndFlush(any());
        verifyNoInteractions(boletas);
    }

    @Test void recetaAusenteImpideVenderSinDescontarInsumos() {
        when(recetas.findByProductoIdIn(anyCollection())).thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> service.procesarCompra(solicitud(1), 1));
        verify(pedidos, never()).saveAndFlush(any());
    }

    @Test void retiroNoCobraDelivery() {
        var original = solicitud(1);
        var retiro = new CheckoutRequest(original.email(), original.items(), "EFECTIVO", "RETIRO", null, original.claveCompra());
        var pedido = service.procesarCompra(retiro, 1);
        assertEquals(25.10, pedido.getTotal());
        assertEquals("PAGO_EN_TIENDA", pedido.getMetodoPago());
    }

    @Test void demoRechazaPagosDePasarelasNoImplementadas() {
        var base = solicitud(1);
        var tarjeta = new CheckoutRequest(base.email(), base.items(), "TARJETA", "DELIVERY", base.direccionEntrega(), base.claveCompra());
        assertThrows(ResponseStatusException.class, () -> service.procesarCompra(tarjeta, 1));
        verifyNoInteractions(productos, boletas);
    }

    @Test void pagoEnTiendaNoPuedeUsarseConDelivery() {
        var base = solicitud(1);
        var incompatible = new CheckoutRequest(base.email(), base.items(), "PAGO_EN_TIENDA", "DELIVERY", base.direccionEntrega(), base.claveCompra());
        assertThrows(ResponseStatusException.class, () -> service.procesarCompra(incompatible, 1));
        verifyNoInteractions(productos, boletas);
    }

    @Test void reintentoDevuelveMismoPedidoSinDescontarOtraVez() {
        Pedido previo = new Pedido(); previo.setId(123);
        when(pedidos.findByUsuarioIdAndClaveCompra(1, solicitud(1).claveCompra())).thenReturn(Optional.of(previo));
        assertSame(previo, service.procesarCompra(solicitud(1), 1));
        verifyNoInteractions(productos, recetas, insumos, boletas);
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
