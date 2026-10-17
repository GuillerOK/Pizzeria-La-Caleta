package com.lacaleta.controller;

import com.lacaleta.model.Insumo;
import com.lacaleta.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {
    private final PedidoRepository pedidos;
    private final ProductoRepository productos;
    private final InsumoRepository insumos;
    private final UsuarioRepository usuarios;
    public AdminRestController(PedidoRepository pedidos, ProductoRepository productos,
                               InsumoRepository insumos, UsuarioRepository usuarios) {
        this.pedidos = pedidos; this.productos = productos; this.insumos = insumos; this.usuarios = usuarios;
    }
    @GetMapping("/dashboard/metricas")
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardMetrics() {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("ventasDelDia", pedidos.ventasEntre(hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay()));
        respuesta.put("pizzasCatalogo", productos.countByDisponibleTrue());
        respuesta.put("insumosCriticos", insumos.contarCriticos());
        respuesta.put("clientesRegistrados", usuarios.countByRolId(1));
        List<Double> semanas = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            LocalDate fin = hoy.plusDays(1).minusWeeks(i);
            semanas.add(pedidos.ventasEntre(fin.minusWeeks(1).atStartOfDay(), fin.atStartOfDay()));
        }
        respuesta.put("graficoIngresosSemanales", semanas);
        long[] pagos = new long[3];
        for (Object[] fila : pedidos.contarPorMetodoPago()) {
            String metodo = String.valueOf(fila[0]).toUpperCase(Locale.ROOT);
            int indice = metodo.contains("YAPE") || metodo.contains("PLIN") ? 0
                    : metodo.contains("EFECTIVO") || metodo.equals("CONTRA_ENTREGA") || metodo.equals("PAGO_EN_TIENDA") ? 1
                    : metodo.contains("TARJETA") || metodo.contains("VISA") ? 2 : -1;
            if (indice >= 0) pagos[indice] += ((Number) fila[1]).longValue();
        }
        respuesta.put("graficoPagos", pagos);
        var top = pedidos.ventasPorProducto(PageRequest.of(0, 5));
        respuesta.put("pizzasLabels", top.stream().map(f -> f[0]).toList());
        respuesta.put("pizzasData", top.stream().map(f -> f[1]).toList());
        var bajos = insumos.findTop4ByStockActualNotNullOrderByStockActualAsc();
        respuesta.put("insumosLabels", bajos.stream().map(Insumo::getNombre).toList());
        respuesta.put("insumosData", bajos.stream().map(Insumo::getStockActual).toList());
        return respuesta;
    }
}
