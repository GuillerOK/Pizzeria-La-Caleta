package com.lacaleta.service;

import com.lacaleta.model.Cupon;
import com.lacaleta.model.Usuario;
import com.lacaleta.repository.CuponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CuponService {

    private final CuponRepository cuponRepository;
    @Autowired private com.lacaleta.repository.UsuarioRepository usuarios;
    @Autowired private com.lacaleta.repository.ProductoRepository productos;

    public static class CatalogoOpcion {
        private final String key;
        private final String titulo;
        private final String descripcion;
        private final String tipo;
        private final double descuento;
        private final int puntosRequeridos;
        private final String icono;
        private final String badge;

        public CatalogoOpcion(String key, String titulo, String descripcion, String tipo, double descuento, int puntosRequeridos, String icono, String badge) {
            this.key = key;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.tipo = tipo;
            this.descuento = descuento;
            this.puntosRequeridos = puntosRequeridos;
            this.icono = icono;
            this.badge = badge;
        }

        public String getKey() { return key; }
        public String getTitulo() { return titulo; }
        public String getDescripcion() { return descripcion; }
        public String getTipo() { return tipo; }
        public double getDescuento() { return descuento; }
        public int getPuntosRequeridos() { return puntosRequeridos; }
        public String getIcono() { return icono; }
        public String getBadge() { return badge; }
    }

    private static final List<CatalogoOpcion> CATALOGO = List.of(
        new CatalogoOpcion("DSCTO_5", "Cupón S/ 5.00 de Descuento", "Descuento de S/ 5.00 directo en el total de tu orden.", "MONTO_FIJO", 5.0, 20, "bi bi-cash-coin", "S/ 5 OFF"),
        new CatalogoOpcion("DELIVERY", "Cupón Delivery 100% Gratis", "Ahorra el costo de envío a tu domicilio en Lima.", "DELIVERY_GRATIS", 5.0, 35, "bi bi-bicycle", "ENVÍO GRATIS"),
        new CatalogoOpcion("GASEOSA", "Gaseosa 1.5L de Regalo", "Acompaña tus pizzas favoritas con una gaseosa fría de 1.5L.", "REGALO", 10.0, 45, "bi bi-cup-straw", "REGALO"),
        new CatalogoOpcion("DSCTO_15", "Cupón 15% de Descuento", "15% de descuento en toda la carta de Pizzas Clásicas y Promociones.", "PORCENTAJE", 15.0, 50, "bi bi-percent", "15% OFF"),
        new CatalogoOpcion("DSCTO_10", "Cupón S/ 10.00 de Descuento", "Ahorra S/ 10.00 en pedidos superiores a S/ 40.00.", "MONTO_FIJO", 10.0, 60, "bi bi-wallet2", "S/ 10 OFF"),
        new CatalogoOpcion("PIZZA_GRATIS", "Pizza Personal Clásica Gratis", "¡Canjea una pizza personal clásica tradicional totalmente gratis!", "REGALO", 18.0, 100, "bi bi-pie-chart-fill", "PIZZA GRATIS")
    );

    @Autowired
    public CuponService(CuponRepository cuponRepository) {
        this.cuponRepository = cuponRepository;
    }

    public List<CatalogoOpcion> obtenerCatalogo() {
        return CATALOGO;
    }

    public List<Cupon> listarPorUsuario(Usuario usuario) {
        if (usuario == null) return Collections.emptyList();
        return cuponRepository.findByUsuarioOrderByFechaCanjeDesc(usuario);
    }

    public List<Cupon> listarActivos(Usuario usuario) {
        if (usuario == null) return Collections.emptyList();
        return cuponRepository.findByUsuarioAndEstadoOrderByFechaCanjeDesc(usuario, "ACTIVO");
    }

    /** El saldo persistido es la única fuente de verdad; nunca resta canjes al leer. */
    @Transactional(readOnly = true)
    public int calcularPuntosDisponibles(Usuario usuario, double totalGastado) {
        return usuario == null || usuario.getPuntos() == null ? 0 : usuario.getPuntos();
    }

    /**
     * Canjea un cupón del catálogo si el usuario tiene suficientes puntos.
     */
    @Transactional(rollbackFor = Exception.class)
    public Cupon canjearCupon(Usuario usuario, String keyOpcion, double totalGastado) {
        if (usuario == null || keyOpcion == null) return null;
        // Serializa canjes y checkout y lee el saldo persistido bajo bloqueo.
        usuario = usuarios.bloquearPorId(usuario.getId()).orElse(null);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getVerificado())) return null;

        CatalogoOpcion opcion = CATALOGO.stream()
                .filter(o -> o.getKey().equalsIgnoreCase(keyOpcion.trim()))
                .findFirst()
                .orElse(null);

        if (opcion == null) {
            return null;
        }

        int puntosDisponibles = calcularPuntosDisponibles(usuario, totalGastado);
        if (puntosDisponibles < opcion.getPuntosRequeridos()) {
            return null; // Puntos insuficientes
        }

        // Generar código único atractivo
        String prefijo = switch (opcion.getKey()) {
            case "DSCTO_5" -> "CAL5";
            case "DELIVERY" -> "FREE";
            case "GASEOSA" -> "BEB";
            case "DSCTO_15" -> "OFF15";
            case "DSCTO_10" -> "CAL10";
            case "PIZZA_GRATIS" -> "PIZZA";
            default -> "CALETA";
        };
        String codigoGenerado;
        // Ocho caracteres legibles; conserva prefijos y códigos históricos sin cambios.
        var random = new java.security.SecureRandom();
        String alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        do {
            var sufijo = new StringBuilder(8);
            for (int i = 0; i < 8; i++) sufijo.append(alfabeto.charAt(random.nextInt(alfabeto.length())));
            codigoGenerado = prefijo + "-" + sufijo;
        } while (cuponRepository.findByCodigoIgnoreCase(codigoGenerado).isPresent());

        Cupon cupon = new Cupon(
                codigoGenerado,
                opcion.getTitulo(),
                opcion.getDescripcion(),
                opcion.getTipo(),
                opcion.getDescuento(),
                opcion.getPuntosRequeridos(),
                usuario,
                LocalDateTime.now().plusDays(30)
        );

        if ("REGALO".equals(opcion.getTipo())) {
            String nombre = "GASEOSA".equals(opcion.getKey()) ? "Inca Kola 1.5L" : "La Cómplice Personal";
            var regalo = productos.findByNombreIgnoreCase(nombre).orElse(null);
            if (regalo == null || !Boolean.TRUE.equals(regalo.getDisponible())
                    || regalo.getStock() == null || regalo.getStock() < 1) return null;
            cupon.setProductoRegalo(regalo);
        }
        Cupon cuponGuardado = cuponRepository.save(cupon);

        // Actualizar y persistir saldo de puntos en Neon DB (tabla usuarios)
        int saldoPuntos = Math.max(0, puntosDisponibles - opcion.getPuntosRequeridos());
        usuario.setPuntos(saldoPuntos);
        usuarios.save(usuario);

        return cuponGuardado;
    }

    @Transactional
    public void eliminarPorUsuario(Usuario usuario) {
        if (usuario != null) {
            cuponRepository.deleteByUsuario(usuario);
        }
    }
}
