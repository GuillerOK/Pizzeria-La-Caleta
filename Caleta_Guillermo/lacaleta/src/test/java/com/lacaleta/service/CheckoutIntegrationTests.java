package com.lacaleta.service;

import com.lacaleta.dto.CheckoutRequest;
import com.lacaleta.model.*;
import com.lacaleta.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class CheckoutIntegrationTests {
    @Autowired PedidoService checkout;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProductoRepository productos;
    @Autowired InsumoRepository insumos;
    @Autowired RecetaInsumoRepository recetas;
    @Autowired PedidoRepository pedidos;
    @Autowired BoletaPendienteRepository boletas;
    @Autowired BoletaOutboxService outbox;
    @Autowired UsuarioService cuentas;
    @Autowired CuponService canjes;
    @Autowired CuponRepository cupones;
    @Autowired VerificacionCuentaService verificacion;
    @Autowired VerificacionCuentaRepository verificaciones;
    @Autowired PlatformTransactionManager manager;
    @Autowired EntityManagerFactory emf;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager em;
    @MockitoBean EmailService correo;
    Usuario usuario;
    Producto pizza;
    Insumo harina;

    @BeforeEach void preparar() {
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            boletas.deleteAllInBatch(); cupones.deleteAllInBatch(); verificaciones.deleteAllInBatch();
            em.createQuery("delete from DetallePedido").executeUpdate();
            recetas.deleteAllInBatch(); pedidos.deleteAllInBatch(); productos.deleteAllInBatch();
            insumos.deleteAllInBatch(); usuarios.deleteAllInBatch(); em.clear();
            usuario = new Usuario(null, "Cliente", "cliente@example.com", "hash", "912345678", null, 1);
            usuario.setVerificado(true); usuario = usuarios.saveAndFlush(usuario);
            pizza = new Producto(); pizza.setNombre("Pizza"); pizza.setPrecio(25.0); pizza.setStock(10);
            pizza = productos.saveAndFlush(pizza);
            harina = insumos.saveAndFlush(new Insumo(null, "HARINA", "Harina", 2.0, "kg", 0.1));
            var receta = new RecetaInsumo(); receta.setProducto(pizza); receta.setInsumo(harina);
            receta.setCantidad(new BigDecimal("0.25")); recetas.saveAndFlush(receta);
        });
        when(correo.renderizarBoleta(any(), any(), any())).thenReturn("<html>Boleta</html>");
    }
    CheckoutRequest solicitud(int cantidad) {
        return new CheckoutRequest(usuario.getEmail(), List.of(new CheckoutRequest.Item(pizza.getId(), cantidad)),
                "EFECTIVO", "DELIVERY", "Av. Lima 123", UUID.randomUUID().toString());
    }
    @Test void confirmaPedidoStockYBoletaEnLaMismaTransaccion() {
        Pedido pedido = checkout.procesarCompra(solicitud(2), usuario.getId());
        assertEquals(55.0, pedido.getTotal());
        assertEquals(8, productos.findById(pizza.getId()).orElseThrow().getStock());
        assertEquals(1.5, insumos.findById(harina.getId()).orElseThrow().getStockActual());
        assertEquals(1, pedidos.count()); assertEquals(1, boletas.count());
        verify(correo, never()).enviarHtml(any(), any(), any());
        outbox.enviarPendientes();
        assertEquals(0, boletas.count());
        verify(correo).enviarHtml(usuario.getEmail(), pedido.getId(), "<html>Boleta</html>");
    }
    @Test void falloTardioReviertePedidoDetallesStockEInsumos() {
        when(correo.renderizarBoleta(any(), any(), any())).thenThrow(new IllegalStateException("Plantilla inválida"));
        assertThrows(IllegalStateException.class, () -> checkout.procesarCompra(solicitud(2), usuario.getId()));
        assertEquals(10, productos.findById(pizza.getId()).orElseThrow().getStock());
        assertEquals(2.0, insumos.findById(harina.getId()).orElseThrow().getStockActual());
        assertEquals(0, pedidos.count()); assertEquals(0, boletas.count());
    }
    @Test void stockDeInsumoInsuficienteRevierteTambienProductos() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> checkout.procesarCompra(solicitud(9), usuario.getId()));
        assertEquals(10, productos.findById(pizza.getId()).orElseThrow().getStock());
        assertEquals(0, pedidos.count());
    }
    @Test void falloSmtpConservaPedidoYDejaBoletaParaReintento() {
        checkout.procesarCompra(solicitud(1), usuario.getId());
        doThrow(new CorreoException(CorreoException.Motivo.CONEXION, null)).when(correo).enviarHtml(any(), any(), any());
        outbox.enviarPendientes();
        assertEquals(1, pedidos.count());
        assertEquals(1, boletas.count());
        assertEquals(1, boletas.findAll().get(0).getIntentos());
    }
    @Test void mismaClaveNoDuplicaPedidoNiStock() {
        var solicitud = solicitud(2);
        Integer id = checkout.procesarCompra(solicitud, usuario.getId()).getId();
        assertEquals(id, checkout.procesarCompra(solicitud, usuario.getId()).getId());
        assertEquals(1, pedidos.count());
        assertEquals(8, productos.findById(pizza.getId()).orElseThrow().getStock());
    }
    @Test void eliminarCuentaConservaPedidosAnonimosYEliminaCupones() {
        var pedido = checkout.procesarCompra(solicitud(1), usuario.getId());
        assertNotNull(canjes.canjearCupon(usuario, "DSCTO_5", 0));
        cuentas.eliminarCuenta(usuario.getId());
        assertFalse(usuarios.existsById(usuario.getId()));
        assertEquals(0, cupones.count());
        assertNull(pedidos.findById(pedido.getId()).orElseThrow().getUsuario());
        assertEquals(1, boletas.count());
    }
    @Test void canjesRecalculanSaldoAunqueClienteEnvieConsumoFalso() {
        assertNotNull(canjes.canjearCupon(usuario, "DSCTO_15", 1_000_000));
        assertNull(canjes.canjearCupon(usuario, "DSCTO_15", 1_000_000));
        assertEquals(1, cupones.count());
    }
    @Test void reenvioDeOtpExigeEsperarUnMinuto() {
        usuario.setVerificado(false); usuarios.saveAndFlush(usuario);
        when(correo.enviarCodigoVerificacion(anyString(), anyString())).thenReturn(true);
        assertTrue(verificacion.enviarCodigo(usuario.getEmail()));
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> verificacion.enviarCodigo(usuario.getEmail()));
        assertEquals(429, error.getStatusCode().value());
        verify(correo, times(1)).enviarCodigoVerificacion(anyString(), anyString());
    }
    @Test void dosClientesNoPuedenComprarLaUltimaUnidad() throws Exception {
        var otro = new Usuario(null, "Otro", "otro@example.com", "hash", "912345678", null, 1);
        otro.setVerificado(true);
        Integer otroId = usuarios.saveAndFlush(otro).getId();
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            var ultimo = productos.findById(pizza.getId()).orElseThrow(); ultimo.setStock(1);
        });
        var inicio = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var uno = executor.submit(() -> {
                inicio.await();
                try { checkout.procesarCompra(solicitud(1), usuario.getId()); return true; }
                catch (org.springframework.web.server.ResponseStatusException error) { return false; }
            });
            var dos = executor.submit(() -> {
                inicio.await();
                var request = new CheckoutRequest("otro@example.com", solicitud(1).items(), "EFECTIVO", "RETIRO", null, UUID.randomUUID().toString());
                try { checkout.procesarCompra(request, otroId); return true; }
                catch (org.springframework.web.server.ResponseStatusException error) { return false; }
            });
            inicio.countDown();
            assertNotEquals(uno.get(10, java.util.concurrent.TimeUnit.SECONDS), dos.get(10, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1, pedidos.count());
            assertEquals(0, productos.findById(pizza.getId()).orElseThrow().getStock());
            assertEquals(1.75, insumos.findById(harina.getId()).orElseThrow().getStockActual());
        } finally { executor.shutdownNow(); }
    }
    @Test void historialCargaRelacionesSinConsultasAdicionales() {
        for (int i = 0; i < 4; i++) checkout.procesarCompra(solicitud(1), usuario.getId());
        var stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.clear();
        var historial = pedidos.findByUsuarioIdOrderByFechaDesc(usuario.getId());
        long consultas = stats.getPrepareStatementCount();
        for (var pedido : historial) {
            assertEquals(usuario.getId(), pedido.getUsuario().getId());
            assertEquals("Pizza", pedido.getDetalles().get(0).getProducto().getNombre());
        }
        assertEquals(1, consultas);
        assertEquals(consultas, stats.getPrepareStatementCount());
        assertEquals(120.0, pedidos.totalPorUsuario(usuario.getId()));
        assertFalse(pedidos.ventasPorProducto(org.springframework.data.domain.PageRequest.of(0, 5)).isEmpty());
        assertEquals(4L, ((Number) pedidos.contarPorMetodoPago().get(0)[1]).longValue());
        stats.clear();
        var pagina = pedidos.findByUsuarioId(usuario.getId(), org.springframework.data.domain.PageRequest.of(0, 2));
        assertEquals(2, pagina.getContent().size()); assertEquals(4, pagina.getTotalElements());
        assertTrue(stats.getPrepareStatementCount() <= 2);
    }
}
