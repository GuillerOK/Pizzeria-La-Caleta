package com.lacaleta.controller;

import com.lacaleta.dto.CheckoutRequest;
import com.lacaleta.model.Usuario;
import com.lacaleta.service.PedidoService;
import com.lacaleta.service.CorreoException;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final PedidoService pedidos;
    public CheckoutController(PedidoService pedidos) { this.pedidos = pedidos; }

    @PostMapping
    public ResponseEntity<?> comprar(@RequestBody CheckoutRequest solicitud, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) return ResponseEntity.status(401).body(Map.of("error", "Inicia sesión para comprar."));
        var pedido = pedidos.procesarCompra(solicitud, usuario.getId());
        return ResponseEntity.ok(Map.of("pedidoId", pedido.getId(), "total", pedido.getTotal(), "boleta", "ENVIADA", "estado", pedido.getEstado()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> solicitudInvalida(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("error", error.getReason()));
    }

    @ExceptionHandler(CorreoException.class)
    public ResponseEntity<?> correoNoDisponible(CorreoException error) {
        return ResponseEntity.status(503).body(Map.of("error",
                "No se pudo preparar la boleta. La compra no se guardó; tu carrito sigue disponible."));
    }
}
