package com.lacaleta.service;

import com.lacaleta.model.*;
import com.lacaleta.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReporteIntegrationTests {
    @Autowired PedidoRepository pedidos;
    @Autowired ProductoRepository productos;
    @Autowired ReporteService reportes;

    Producto producto(String nombre) {
        Producto producto = new Producto();
        producto.setNombre(nombre); producto.setPrecio(20.0); producto.setStock(10);
        return productos.saveAndFlush(producto);
    }

    void pedido(String fecha, String estado, double total, String pago, Producto producto) {
        Pedido pedido = new Pedido();
        pedido.setFecha(LocalDateTime.parse(fecha)); pedido.setEstado(estado);
        pedido.setTotal(total); pedido.setMetodoPago(pago);
        pedido.getDetalles().add(new DetallePedido(pedido, producto, 2, 20.0));
        pedidos.saveAndFlush(pedido);
    }

    @Test void todosLosBloquesRespetanFechasEstadosYUltimoDiaCompleto() {
        Producto pizza = producto("Mismo nombre");
        Producto otraPizza = producto("Mismo nombre");
        pedido("2024-06-10T00:00:00", "ENTREGADO", 45, "YAPE", pizza);
        pedido("2024-06-11T23:59:59", "ENTREGADO", 55, "EFECTIVO", otraPizza);
        pedido("2024-06-11T12:00:00", "PENDIENTE", 999, "TARJETA", pizza);
        pedido("2024-06-11T12:00:00", "CANCELADO", 888, "TARJETA", pizza);
        pedido("2024-06-12T00:00:00", "ENTREGADO", 777, "TARJETA", pizza);
        pedido("2024-06-09T23:59:59", "ENTREGADO", 50, "YAPE", pizza);
        var reporte = reportes.generar("personalizado", LocalDate.of(2024, 6, 10), LocalDate.of(2024, 6, 11));
        assertEquals(4, reporte.resumen().pedidos());
        assertEquals(2, reporte.resumen().entregados());
        assertEquals(1, reporte.resumen().cancelados());
        assertEquals(100, reporte.resumen().ventas());
        assertEquals(50, reporte.resumen().ticketPromedio());
        assertEquals(100.0, reporte.variacionVentas());
        assertEquals(List.of(45.0, 55.0), reporte.tendencia().stream().map(ReporteService.Punto::ventas).toList());
        assertEquals(100, reporte.pagos().stream().mapToDouble(ReporteService.Pago::importe).sum());
        assertEquals(2, reporte.pagos().stream().mapToLong(ReporteService.Pago::pedidos).sum());
        assertEquals(2, reporte.productos().size(), "Productos distintos con el mismo nombre no deben mezclarse");
        assertEquals(4, reporte.productos().stream().mapToLong(ReporteService.Producto::unidades).sum());
        assertEquals(80, reporte.productos().stream().mapToDouble(ReporteService.Producto::importe).sum());
    }

    @Test void tendenciaMensualConservaTotalYMesesSinVentas() {
        Producto pizza = producto("Prueba mensual");
        pedido("2023-02-28T23:59:59", "ENTREGADO", 20, null, pizza);
        pedido("2023-03-01T00:00:00", "ENTREGADO", 30, "YAPE", pizza);
        var reporte = reportes.generar("personalizado", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));
        assertEquals("mes", reporte.agrupacion());
        assertEquals(12, reporte.tendencia().size());
        assertEquals(20, reporte.tendencia().get(1).ventas());
        assertEquals(30, reporte.tendencia().get(2).ventas());
        assertEquals(reporte.resumen().ventas(), reporte.tendencia().stream().mapToDouble(ReporteService.Punto::ventas).sum());
        assertTrue(reporte.pagos().stream().anyMatch(p -> p.metodo().equals("SIN REGISTRAR")));
    }
}
