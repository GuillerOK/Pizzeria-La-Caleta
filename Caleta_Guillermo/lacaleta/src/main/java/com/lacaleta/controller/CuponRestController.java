package com.lacaleta.controller;

import com.lacaleta.model.Cupon;
import com.lacaleta.model.Usuario;
import com.lacaleta.repository.CuponRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/cupones")
public class CuponRestController {

    private final CuponRepository cuponRepository;

    public CuponRestController(CuponRepository cuponRepository) {
        this.cuponRepository = cuponRepository;
    }

    public record ValidarCuponRequest(String codigo, Double subtotal) {}

    @PostMapping("/validar")
    public ResponseEntity<?> validarCupon(@RequestBody ValidarCuponRequest solicitud, HttpSession session) {
        if (solicitud == null || solicitud.codigo() == null || solicitud.codigo().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("valido", false, "error", "Ingresa un código de cupón válido."));
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of("valido", false, "error", "Inicia sesión con tu cuenta para aplicar tu cupón."));
        }

        var cuponOpt = cuponRepository.findByCodigoIgnoreCase(solicitud.codigo().trim());
        if (cuponOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("valido", false, "error", "El cupón no existe en nuestro sistema."));
        }

        Cupon cupon = cuponOpt.get();

        if (!"ACTIVO".equalsIgnoreCase(cupon.getEstado())) {
            return ResponseEntity.badRequest().body(Map.of("valido", false, "error", "Este cupón ya fue utilizado o no se encuentra activo."));
        }

        if (cupon.getFechaVencimiento() != null && cupon.getFechaVencimiento().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("valido", false, "error", "Este cupón ha vencido."));
        }

        if (cupon.getUsuario() == null || !cupon.getUsuario().getId().equals(usuario.getId())) {
            return ResponseEntity.status(403).body(Map.of("valido", false, "error", "Este cupón pertenece a otra cuenta de usuario."));
        }

        double subtotal = solicitud.subtotal() != null ? solicitud.subtotal() : 0.0;
        if (!Double.isFinite(subtotal) || subtotal < 0 || cupon.getDescuento() == null
                || !Double.isFinite(cupon.getDescuento()) || cupon.getDescuento() < 0
                || ("PORCENTAJE".equalsIgnoreCase(cupon.getTipo()) && cupon.getDescuento() > 100)) {
            return ResponseEntity.badRequest().body(Map.of("valido", false, "error", "Importe o descuento inválido."));
        }
        if (cupon.getCodigo().startsWith("CAL10") && subtotal < 40.0) {
            return ResponseEntity.badRequest().body(Map.of("valido", false, "error", "Este cupón requiere un consumo mínimo de S/ 40.00."));
        }

        double descuento;
        if ("DELIVERY_GRATIS".equalsIgnoreCase(cupon.getTipo())) {
            descuento = 5.00;
        } else if ("PORCENTAJE".equalsIgnoreCase(cupon.getTipo())) {
            descuento = Math.round((subtotal * (cupon.getDescuento() / 100.0)) * 100.0) / 100.0;
        } else if ("REGALO".equalsIgnoreCase(cupon.getTipo())) {
            if (cupon.getProductoRegalo() == null) {
                return ResponseEntity.badRequest().body(Map.of("valido", false, "error", "El cupón no tiene un producto de regalo configurado."));
            }
            descuento = 0;
        } else if ("MONTO_FIJO".equalsIgnoreCase(cupon.getTipo())) {
            descuento = Math.min(subtotal, cupon.getDescuento());
        } else {
            return ResponseEntity.badRequest().body(Map.of("valido", false, "error", "Tipo de cupón inválido."));
        }

        return ResponseEntity.ok(Map.of(
                "valido", true,
                "codigo", cupon.getCodigo(),
                "titulo", cupon.getTitulo(),
                "descripcion", cupon.getDescripcion() != null ? cupon.getDescripcion() : "",
                "tipo", cupon.getTipo(),
                "valorDescuento", cupon.getDescuento(),
                "descuento", descuento,
                "mensaje", "¡Cupón aplicado correctamente!"
        ));
    }
}
