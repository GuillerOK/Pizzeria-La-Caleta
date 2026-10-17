package com.lacaleta.controller;

import com.lacaleta.model.Insumo;
import com.lacaleta.service.InsumoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/insumos")
public class AdminInsumoController {

    private final InsumoService insumoService;

    public AdminInsumoController(InsumoService insumoService) {
        this.insumoService = insumoService;
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(value = "id", required = false) Integer id,
            @RequestParam("nombre") String nombre,
            @RequestParam("stockActual") Double stockActual,
            @RequestParam("unidadMedida") String unidadMedida,
            @RequestParam(value = "stockMinimo", defaultValue = "5.0") Double stockMinimo,
            RedirectAttributes flash) {

        String unidadNormalizada = unidadMedida == null ? "" : unidadMedida.trim().toLowerCase(java.util.Locale.ROOT);
        if (nombre == null || nombre.isBlank() || !java.util.Set.of("kg", "g", "litros", "unidades").contains(unidadNormalizada)
                || stockActual == null || !Double.isFinite(stockActual) || stockActual < 0
                || stockMinimo == null || !Double.isFinite(stockMinimo) || stockMinimo < 0) {
            flash.addFlashAttribute("error", "Completa los campos correctamente. La unidad debe ser kg, g, litros o unidades y los stocks no pueden ser negativos.");
            return "redirect:/admin#stock";
        }

        try {
            Insumo insumo;
            if (id != null) {
                insumo = insumoService.buscarPorId(id).orElse(null);
                if (insumo == null) {
                    flash.addFlashAttribute("error", "No se encontró el insumo que intentas modificar.");
                    return "redirect:/admin#stock";
                }
            } else {
                insumo = new Insumo();
                insumo.setCodigo(insumoService.generarCodigo());
            }

            insumo.setNombre(nombre.trim());
            insumo.setStockActual(stockActual);
            insumo.setUnidadMedida(unidadNormalizada);
            insumo.setStockMinimo(stockMinimo);
            insumoService.guardar(insumo);

            flash.addFlashAttribute("exito", id == null
                    ? "Insumo registrado correctamente en la base de datos."
                    : "Insumo actualizado correctamente en la base de datos.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo guardar el insumo. Verifica los datos y la conexión con la base de datos.");
        }
        return "redirect:/admin#stock";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            if (insumoService.eliminar(id)) {
                flash.addFlashAttribute("exito", "Insumo eliminado correctamente.");
            } else {
                flash.addFlashAttribute("error", "No se encontró el insumo que deseas eliminar.");
            }
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo eliminar el insumo. Puede estar relacionado con otros registros.");
        }
        return "redirect:/admin#stock";
    }
}
