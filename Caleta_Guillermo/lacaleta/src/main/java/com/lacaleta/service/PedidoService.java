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
    private final EmailService email;

    public PedidoService(ProductoRepository productos, PedidoRepository pedidos,
                         UsuarioRepository usuarios, EmailService email) {
        this.productos = productos;
        this.pedidos = pedidos;
        this.usuarios = usuarios;
        this.email = email;
    }

    @Transactional(rollbackFor = Exception.class)
    public Pedido procesarCompra(CheckoutRequest solicitud, Integer usuarioId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Inicia sesión nuevamente."));
        if (solicitud == null || solicitud.email() == null
                || !usuario.getEmail().equalsIgnoreCase(solicitud.email())) {
            throw new ResponseStatusException(BAD_REQUEST, "El correo no corresponde a la sesión.");
        }
        if (solicitud.items() == null || solicitud.items().isEmpty() || solicitud.items().size() > 100) {
            throw new ResponseStatusException(BAD_REQUEST, "El carrito debe contener entre 1 y 100 items.");
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
        pedido.setFecha(LocalDateTime.now(ZoneId.of("America/Lima")));
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var entrada : cantidades.entrySet()) {
            Producto producto = productos.bloquearPorId(entrada.getKey())
                    .orElseThrow(() -> new ResponseStatusException(CONFLICT, "Un producto ya no existe."));
            int cantidad = entrada.getValue();
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
        pedido.setTotal(subtotal.add(COSTO_ENVIO).doubleValue());
        pedidos.saveAndFlush(pedido); // Incluye detalles por CascadeType.ALL y stock por dirty checking.
        email.enviarBoleta(pedido, subtotal, COSTO_ENVIO);
        return pedido;
    }
}
