package com.lacaleta.demo;

import com.lacaleta.dto.CheckoutRequest;
import com.lacaleta.model.*;
import com.lacaleta.repository.*;
import com.lacaleta.service.PedidoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.util.*;
import java.math.BigDecimal;

/** Prueba sobre la rama configurada: checkout real, pero todas las compras se revierten. */
@Component
@Profile("demo-check")
public class DemoCheckRunner implements ApplicationRunner {
    private final PedidoService checkout;
    private final ProductoRepository productos;
    private final InsumoRepository insumos;
    private final RecetaInsumoRepository recetas;
    private final PedidoRepository pedidos;
    private final BoletaPendienteRepository boletas;
    private final TransactionTemplate tx;
    private final ConfigurableApplicationContext contexto;
    private final JavaMailSenderImpl smtp;
    @PersistenceContext private EntityManager em;
    @Value("${app.demo.check-smtp:false}") private boolean comprobarSmtp;
    public DemoCheckRunner(PedidoService checkout, ProductoRepository productos, InsumoRepository insumos,
                           RecetaInsumoRepository recetas, PedidoRepository pedidos, BoletaPendienteRepository boletas,
                           PlatformTransactionManager manager, ConfigurableApplicationContext contexto, JavaMailSenderImpl smtp) {
        this.checkout = checkout; this.productos = productos; this.insumos = insumos; this.recetas = recetas;
        this.pedidos = pedidos; this.boletas = boletas; this.tx = new TransactionTemplate(manager);
        this.contexto = contexto; this.smtp = smtp;
    }
    @Override public void run(ApplicationArguments args) throws Exception {
        try {
            for (String entrega : List.of("DELIVERY", "RETIRO")) verificar(entrega);
            if (comprobarSmtp) {
                smtp.testConnection();
                System.out.println("DEMO_OK: conexión y autenticación SMTP verificadas; no se enviaron correos de prueba.");
            }
            System.out.println("DEMO_OK: ambos checkouts ejecutados en PostgreSQL y revertidos sin consumir existencias ni dejar pedidos.");
        } finally { contexto.close(); }
    }
    private void verificar(String entrega) {
        long pedidosAntes = pedidos.count(), boletasAntes = boletas.count();
        var stockAntes = new TreeMap<Integer, Double>();
        insumos.findAll().forEach(i -> stockAntes.put(i.getId(), i.getStockActual()));
        var resultado = tx.execute(estado -> {
            Usuario usuario = em.createQuery("select u from Usuario u where u.verificado=true and u.rolId=1", Usuario.class)
                    .setMaxResults(1).getResultStream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Se necesita un cliente con correo verificado para la prueba."));
            var catalogo = em.createQuery("select p from Producto p where p.disponible=true and p.stock>0 order by p.id", Producto.class).getResultList();
            if (catalogo.isEmpty() || catalogo.size() > 100) throw new IllegalStateException("La prueba necesita entre 1 y 100 productos disponibles.");
            var stockProductos = new TreeMap<Integer, Integer>();
            catalogo.forEach(p -> stockProductos.put(p.getId(), p.getStock()));
            var lineas = recetas.findByProductoIdIn(stockProductos.keySet());
            if (lineas.isEmpty()) throw new IllegalStateException("No hay recetas configuradas.");
            var consumos = new TreeMap<Integer, BigDecimal>();
            lineas.forEach(r -> consumos.merge(r.getInsumo().getId(), r.getCantidad(), BigDecimal::add));
            String metodo = entrega.equals("DELIVERY") ? "CONTRA_ENTREGA" : "PAGO_EN_TIENDA";
            var solicitud = new CheckoutRequest(usuario.getEmail(), catalogo.stream().map(p -> new CheckoutRequest.Item(p.getId(), 1)).toList(),
                    metodo, entrega, "Dirección ficticia de verificación", UUID.randomUUID().toString());
            var pedido = checkout.procesarCompra(solicitud, usuario.getId());
            em.flush();
            for (var producto : catalogo) {
                if (producto.getStock() != stockProductos.get(producto.getId()) - 1)
                    throw new IllegalStateException("No se descontó un producto del catálogo.");
            }
            for (var consumo : consumos.entrySet()) {
                Insumo insumo = insumos.findById(consumo.getKey()).orElseThrow();
                double esperado = BigDecimal.valueOf(stockAntes.get(insumo.getId())).subtract(consumo.getValue()).doubleValue();
                if (Math.abs(insumo.getStockActual() - esperado) > .0000001) throw new IllegalStateException("Descuento de insumo incorrecto.");
            }
            double esperado = catalogo.stream().mapToDouble(Producto::getPrecio).sum() + (entrega.equals("DELIVERY") ? 5 : 0);
            if (Math.abs(pedido.getTotal() - esperado) > .001 || !metodo.equals(pedido.getMetodoPago()))
                throw new IllegalStateException("Total o modalidad de pago incorrectos.");
            if (boletas.count() != boletasAntes + 1) throw new IllegalStateException("No se encoló la boleta.");
            var boleta = boletas.findAll().stream().filter(b -> b.getPedidoId().equals(pedido.getId())).findFirst().orElseThrow();
            if (!boleta.getHtml().contains("La Caleta") || !boleta.getDestinatario().equals(usuario.getEmail()))
                throw new IllegalStateException("Boleta o destinatario incorrectos.");
            System.out.println("DEMO_OK: " + metodo + ", total=" + pedido.getTotal() + ", insumos=" + consumos.size() + ", productos=" + catalogo.size() + ", boleta HTML persistida en transacción.");
            estado.setRollbackOnly();
            return stockProductos;
        });
        if (resultado == null || pedidos.count() != pedidosAntes || boletas.count() != boletasAntes)
            throw new IllegalStateException("La prueba no se revirtió correctamente.");
        productos.findAllById(resultado.keySet()).forEach(p -> {
            if (!Objects.equals(resultado.get(p.getId()), p.getStock())) throw new IllegalStateException("Stock de producto no restaurado.");
        });
        insumos.findAll().forEach(i -> {
            if (!Objects.equals(stockAntes.get(i.getId()), i.getStockActual()))
                throw new IllegalStateException("La prueba modificó existencias de insumos.");
        });
    }
}
