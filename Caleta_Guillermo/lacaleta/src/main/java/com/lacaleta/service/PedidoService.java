package com.lacaleta.service;

import com.lacaleta.dto.CheckoutRequest;
import com.lacaleta.model.*;
import com.lacaleta.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Service
public class PedidoService {
    public static final BigDecimal COSTO_ENVIO = new BigDecimal("5.00");
    private final ProductoRepository productos;
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final BoletaOutboxService boletas;
    private final RecetaInsumoRepository recetas;
    private final InsumoRepository insumos;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CuponRepository cupones;

    public PedidoService(ProductoRepository productos, PedidoRepository pedidos,
                         UsuarioRepository usuarios, BoletaOutboxService boletas,
                         RecetaInsumoRepository recetas, InsumoRepository insumos) {
        this.productos = productos;
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.boletas = boletas;
        this.recetas = recetas;
        this.insumos = insumos;
    }
    @org.springframework.beans.factory.annotation.Autowired
    public PedidoService(ProductoRepository productos, PedidoRepository pedidos,
                         UsuarioRepository usuarios, BoletaOutboxService boletas,
                         RecetaInsumoRepository recetas, InsumoRepository insumos,
                         CuponRepository cupones) {
        this(productos, pedidos, usuarios, boletas, recetas, insumos);
        this.cupones = cupones;
    }

    @Transactional(rollbackFor = Exception.class)
    public Pedido procesarCompra(CheckoutRequest solicitud, Integer usuarioId) {
        Usuario usuario = usuarios.bloquearPorId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Inicia sesión nuevamente."));
        if (!Boolean.TRUE.equals(usuario.getVerificado())) {
            throw new ResponseStatusException(FORBIDDEN, "Verifica tu correo antes de comprar.");
        }
        if (solicitud == null || solicitud.email() == null
                || !usuario.getEmail().equalsIgnoreCase(solicitud.email())) {
            throw new ResponseStatusException(BAD_REQUEST, "El correo no corresponde a la sesión.");
        }
        if (solicitud.items() == null || solicitud.items().isEmpty() || solicitud.items().size() > 100) {
            throw new ResponseStatusException(BAD_REQUEST, "El carrito debe contener entre 1 y 100 items.");
        }
        if (solicitud.claveCompra() == null || !solicitud.claveCompra().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new ResponseStatusException(BAD_REQUEST, "Falta una clave de compra válida.");
        }
        var existente = pedidos.findByUsuarioIdAndClaveCompra(usuarioId, solicitud.claveCompra());
        if (existente.isPresent()) return existente.get();
        String metodo = solicitud.metodoPago();
        if (metodo == null || !java.util.Set.of("EFECTIVO", "CONTRA_ENTREGA", "PAGO_EN_TIENDA").contains(metodo)) {
            throw new ResponseStatusException(BAD_REQUEST, "Método de pago inválido.");
        }
        if (!"DELIVERY".equals(solicitud.tipoEntrega()) && !"RETIRO".equals(solicitud.tipoEntrega())) {
            throw new ResponseStatusException(BAD_REQUEST, "Selecciona delivery o retiro.");
        }
        boolean delivery = "DELIVERY".equals(solicitud.tipoEntrega());
        if ((delivery && "PAGO_EN_TIENDA".equals(metodo)) || (!delivery && "CONTRA_ENTREGA".equals(metodo))) {
            throw new ResponseStatusException(BAD_REQUEST, "El pago debe corresponder al tipo de entrega.");
        }
        metodo = delivery ? "CONTRA_ENTREGA" : "PAGO_EN_TIENDA";
        if (delivery && (solicitud.direccionEntrega() == null || solicitud.direccionEntrega().isBlank()
                || solicitud.direccionEntrega().length() > 255)) {
            throw new ResponseStatusException(BAD_REQUEST, "Ingresa una dirección de entrega válida.");
        }
        // Agregar duplicados y bloquear siempre en orden para evitar interbloqueos.
        var cantidades = new TreeMap<Integer, Integer>();
        for (var item : solicitud.items()) {
            if (item == null || item.productoId() == null || item.productoId() <= 0
                    || item.cantidad() == null || item.cantidad() <= 0 || item.cantidad() > 1000) {
                throw new ResponseStatusException(BAD_REQUEST, "Producto o cantidad inválidos.");
            }
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setClaveCompra(solicitud.claveCompra());
        pedido.setMetodoPago(metodo);
        pedido.setDireccionEntrega(delivery ? solicitud.direccionEntrega().trim() : "RETIRO EN TIENDA");
        var productosComprados = new TreeMap<Integer, Producto>();
        pedido.setFecha(LocalDateTime.now(ZoneId.of("America/Lima")));
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var entrada : cantidades.entrySet()) {
            Producto producto = productos.bloquearPorId(entrada.getKey())
                    .orElseThrow(() -> new ResponseStatusException(CONFLICT, "Un producto ya no existe."));
            productosComprados.put(entrada.getKey(), producto);
            int cantidad = entrada.getValue();
            if (cantidad > 1000) throw new ResponseStatusException(BAD_REQUEST, "Máximo 1000 unidades por producto.");
            if (!Boolean.TRUE.equals(producto.getDisponible()) || producto.getStock() == null
                    || producto.getStock() < cantidad) {
                throw new ResponseStatusException(CONFLICT, "Stock insuficiente para " + producto.getNombre());
            }
            if (producto.getPrecio() == null || !Double.isFinite(producto.getPrecio()) || producto.getPrecio() < 0) {
                throw new ResponseStatusException(CONFLICT, "El producto no tiene un precio válido.");
            }
            BigDecimal precio = BigDecimal.valueOf(producto.getPrecio()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal importe = precio.multiply(BigDecimal.valueOf(cantidad));
            producto.setStock(producto.getStock() - cantidad);
            DetallePedido detalle = new DetallePedido(pedido, producto, cantidad, precio.doubleValue());
            detalle.setSubtotal(importe.doubleValue());
            pedido.getDetalles().add(detalle);
            subtotal = subtotal.add(importe);
        }
        var consumos = new TreeMap<Integer, BigDecimal>();
        var conReceta = new java.util.HashSet<Integer>();
        for (var linea : recetas.findByProductoIdIn(cantidades.keySet())) {
            if (linea.getCantidad() == null || linea.getCantidad().signum() <= 0) {
                throw new ResponseStatusException(CONFLICT, "Receta con cantidades inválidas.");
            }
            conReceta.add(linea.getProducto().getId());
            consumos.merge(linea.getInsumo().getId(), linea.getCantidad()
                    .multiply(BigDecimal.valueOf(cantidades.get(linea.getProducto().getId()))), BigDecimal::add);
        }
        for (var producto : productosComprados.values()) {
            String categoria = producto.getCategoria() == null ? "" : producto.getCategoria().getCodigo();
            if (!java.util.Set.of("bebidas", "bebida").contains(categoria == null ? "" : categoria.toLowerCase(java.util.Locale.ROOT))
                    && !conReceta.contains(producto.getId())) {
                throw new ResponseStatusException(CONFLICT, "Configura la receta de insumos para " + producto.getNombre());
            }
        }
        for (var consumo : consumos.entrySet()) {
            Insumo insumo = insumos.bloquearPorId(consumo.getKey())
                    .orElseThrow(() -> new ResponseStatusException(CONFLICT, "Un insumo ya no existe."));
            Double stock = insumo.getStockActual();
            if (stock == null || !Double.isFinite(stock)
                    || BigDecimal.valueOf(stock).compareTo(consumo.getValue()) < 0) {
                throw new ResponseStatusException(CONFLICT, "Insumo insuficiente: " + insumo.getNombre());
            }
            insumo.setStockActual(BigDecimal.valueOf(stock).subtract(consumo.getValue()).doubleValue());
        }
        BigDecimal envio = delivery ? COSTO_ENVIO : BigDecimal.ZERO;
        BigDecimal descuentoCupon = BigDecimal.ZERO;
        if (cupones != null && solicitud.codigoCupon() != null && !solicitud.codigoCupon().isBlank()) {
            Cupon cupon = cupones.findByCodigoIgnoreCase(solicitud.codigoCupon().trim())
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "El cupón no existe en la base de datos."));
            if (!"ACTIVO".equalsIgnoreCase(cupon.getEstado())) {
                throw new ResponseStatusException(BAD_REQUEST, "El cupón ya ha sido utilizado o no se encuentra activo.");
            }
            if (cupon.getFechaVencimiento() != null && cupon.getFechaVencimiento().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(BAD_REQUEST, "El cupón ha expirado.");
            }
            if (cupon.getUsuario() != null && !cupon.getUsuario().getId().equals(usuario.getId())) {
                throw new ResponseStatusException(FORBIDDEN, "El cupón no pertenece a tu cuenta.");
            }
            if (cupon.getCodigo().startsWith("CAL10") && subtotal.compareTo(new BigDecimal("40.00")) < 0) {
                throw new ResponseStatusException(BAD_REQUEST, "Este cupón requiere un consumo mínimo de S/ 40.00.");
            }
            if ("DELIVERY_GRATIS".equalsIgnoreCase(cupon.getTipo())) {
                descuentoCupon = envio;
            } else if ("PORCENTAJE".equalsIgnoreCase(cupon.getTipo())) {
                descuentoCupon = subtotal.multiply(BigDecimal.valueOf(cupon.getDescuento()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                descuentoCupon = BigDecimal.valueOf(cupon.getDescuento()).min(subtotal);
            }
            cupon.setEstado("USADO");
            cupones.save(cupon);
        }
        BigDecimal totalFinal = subtotal.add(envio).subtract(descuentoCupon).max(BigDecimal.ZERO);
        pedido.setTotal(totalFinal.setScale(2, RoundingMode.HALF_UP).doubleValue());
        pedidos.saveAndFlush(pedido); // Incluye detalles por CascadeType.ALL y stock por dirty checking.

        try {
            double nuevoTotalGastado = pedidos.totalPorUsuario(usuario.getId());
            int puntosGastados = 0;
            if (cupones != null) {
                var canjeados = cupones.findByUsuarioOrderByFechaCanjeDesc(usuario);
                puntosGastados = canjeados.stream()
                        .mapToInt(c -> c.getPuntosRequeridos() != null ? c.getPuntosRequeridos() : 0)
                        .sum();
            }
            int saldoPuntos = Math.max(0, 50 + (int) (nuevoTotalGastado / 5.0) - puntosGastados);
            usuario.setPuntos(saldoPuntos);
            usuarios.save(usuario);
        } catch (Exception ignored) {}
        boletas.encolar(pedido, subtotal, envio);
        return pedido;
    }
}
