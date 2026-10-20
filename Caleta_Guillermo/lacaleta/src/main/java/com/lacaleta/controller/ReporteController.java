package com.lacaleta.controller;

import com.lacaleta.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reportes")
public class ReporteController {
    private final ReporteService reportes;
    private final com.lacaleta.repository.InsumoRepository insumos;

    public ReporteController(ReporteService reportes, com.lacaleta.repository.InsumoRepository insumos) {
        this.reportes = reportes;
        this.insumos = insumos;
    }

    public record Stock(String nombre, double cantidad, String unidad, Double minimo, boolean critico) {}

    @GetMapping("/inventario")
    public java.util.List<Stock> inventario() {
        return insumos.findTop4ByStockActualNotNullOrderByStockActualAsc().stream()
                .map(i -> new Stock(i.getNombre(), i.getStockActual(), i.getUnidadMedida(), i.getStockMinimo(), i.isCritico())).toList();
    }

    @GetMapping
    public ReporteService.Reporte obtener(
            @RequestParam(defaultValue = "mes") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportes.generar(periodo, desde, hasta);
    }
}
