package com.lacaleta.controller;

import com.lacaleta.model.Pedido;
import com.lacaleta.model.Insumo;
import com.lacaleta.model.Producto;
import com.lacaleta.repository.InsumoRepository;
import com.lacaleta.repository.PedidoRepository;
import com.lacaleta.repository.ProductoRepository;
import com.lacaleta.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private InsumoRepository insumoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/dashboard/metricas")
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> response = new HashMap<>();

        // 1. Ventas del día (sumatoria total)
        List<Pedido> pedidos = pedidoRepository.findAll();
        LocalDate hoy = LocalDate.now();
        double ventasDelDia = pedidos.stream()
                .filter(p -> p.getFecha() != null && p.getFecha().toLocalDate().equals(hoy))
                .mapToDouble(p -> p.getTotal() != null ? p.getTotal() : 0.0)
                .sum();
        response.put("ventasDelDia", ventasDelDia);

        // 2. Pizzas en catálogo (count de productos activos)
        long pizzasCatalogo = productoRepository.findAll().stream()
                .filter(p -> p.getDisponible() != null && p.getDisponible())
                .count();
        response.put("pizzasCatalogo", pizzasCatalogo);

        // 3. Insumos Críticos (stockActual <= stockMinimo)
        long insumosCriticos = insumoRepository.findAll().stream()
                .filter(i -> i.getStockActual() != null && i.getStockMinimo() != null && i.getStockActual() <= i.getStockMinimo())
                .count();
        response.put("insumosCriticos", insumosCriticos);

        // 4. Clientes Registrados
        long clientesRegistrados = usuarioRepository.findAll().stream()
                .filter(u -> Integer.valueOf(1).equals(u.getRolId()))
                .count();
        response.put("clientesRegistrados", clientesRegistrados);

        // 5. Gráfico de Ingresos Semanales (Últimas 4 semanas)
        double[] ingresosSemanales = new double[4];
        
        // Datos para Gráfico Circular: Métodos de Pago
        int yapePlin = 0, efectivo = 0, tarjeta = 0;

        // Datos para Gráfico Barras: Pizzas (simulado basado en detalles de pedidos o estático adaptado)
        // Como simplificación, calcularemos basados en las pizzas disponibles
        Map<String, Double> ventasPorPizzaMap = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();
        for (Pedido p : pedidos) {
            if (p.getFecha() != null) {
                long daysBetween = ChronoUnit.DAYS.between(p.getFecha().toLocalDate(), now.toLocalDate());
                if (daysBetween >= 0 && daysBetween < 28) {
                    int weekIndex = 3 - (int) (daysBetween / 7);
                    ingresosSemanales[weekIndex] += (p.getTotal() != null ? p.getTotal() : 0.0);
                }
            }
            
            if (p.getMetodoPago() != null) {
                String mp = p.getMetodoPago().toUpperCase();
                if (mp.contains("YAPE") || mp.contains("PLIN")) yapePlin++;
                else if (mp.contains("EFECTIVO")) efectivo++;
                else if (mp.contains("TARJETA") || mp.contains("VISA")) tarjeta++;
            }

            if (p.getDetalles() != null) {
                for (var d : p.getDetalles()) {
                    if (d.getProducto() != null && d.getProducto().getNombre() != null) {
                        ventasPorPizzaMap.put(d.getProducto().getNombre(), 
                            ventasPorPizzaMap.getOrDefault(d.getProducto().getNombre(), 0.0) + (d.getSubtotal() != null ? d.getSubtotal() : 0.0));
                    }
                }
            }
        }
        
        response.put("graficoIngresosSemanales", Arrays.asList(
            ingresosSemanales[0], ingresosSemanales[1], ingresosSemanales[2], ingresosSemanales[3]
        ));

        response.put("graficoPagos", Arrays.asList(yapePlin, efectivo, tarjeta));

        // Ordenar y tomar top 5 pizzas
        List<Map.Entry<String, Double>> topPizzas = ventasPorPizzaMap.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(5).toList();
        response.put("pizzasLabels", topPizzas.stream().map(Map.Entry::getKey).toList());
        response.put("pizzasData", topPizzas.stream().map(Map.Entry::getValue).toList());

        // Datos para Gráfico Horizontal: Insumos (Tomamos los 4 con menos stock)
        List<Insumo> topInsumos = insumoRepository.findAll().stream()
            .filter(i -> i.getStockActual() != null)
            .sorted((i1, i2) -> i1.getStockActual().compareTo(i2.getStockActual()))
            .limit(4).toList();
            
        response.put("insumosLabels", topInsumos.stream().map(Insumo::getNombre).toList());
        response.put("insumosData", topInsumos.stream().map(Insumo::getStockActual).toList());

        return response;
    }
}
