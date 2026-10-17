package com.lacaleta.controller;

import com.lacaleta.model.RecetaInsumo;
import com.lacaleta.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/admin/productos/{productoId}/receta")
public class AdminRecetaController {
    private final ProductoRepository productos;
    private final InsumoRepository insumos;
    private final RecetaInsumoRepository recetas;
    public AdminRecetaController(ProductoRepository productos, InsumoRepository insumos, RecetaInsumoRepository recetas) {
        this.productos = productos; this.insumos = insumos; this.recetas = recetas;
    }
    public record Linea(Integer insumoId, BigDecimal cantidad) { }
    @GetMapping
    @Transactional(readOnly = true)
    public List<Linea> consultar(@PathVariable Integer productoId) {
        return recetas.findByProductoIdIn(List.of(productoId)).stream()
                .map(r -> new Linea(r.getInsumo().getId(), r.getCantidad())).toList();
    }
    @PutMapping
    @Transactional
    public Map<String, String> guardar(@PathVariable Integer productoId, @RequestBody List<Linea> lineas) {
        if (lineas == null || lineas.isEmpty() || lineas.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa entre 1 y 100 insumos de la receta.");
        }
        var producto = productos.bloquearPorId(productoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto inexistente."));
        var ids = new HashSet<Integer>();
        var nuevas = new ArrayList<RecetaInsumo>();
        for (var linea : lineas) {
            if (linea == null || linea.insumoId() == null || !ids.add(linea.insumoId())
                    || linea.cantidad() == null || linea.cantidad().signum() <= 0
                    || linea.cantidad().scale() > 6 || linea.cantidad().compareTo(new BigDecimal("1000000")) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receta inválida o insumos duplicados.");
            }
            var insumo = insumos.findById(linea.insumoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insumo inexistente."));
            var receta = new RecetaInsumo(); receta.setProducto(producto); receta.setInsumo(insumo);
            receta.setCantidad(linea.cantidad()); nuevas.add(receta);
        }
        recetas.deleteAll(recetas.findByProductoIdIn(List.of(productoId)));
        recetas.flush();
        recetas.saveAll(nuevas);
        return Map.of("mensaje", "Receta actualizada.");
    }
}
