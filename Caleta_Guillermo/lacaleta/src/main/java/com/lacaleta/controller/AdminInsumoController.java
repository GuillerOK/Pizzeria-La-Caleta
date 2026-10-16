package com.lacaleta.controller;

import com.lacaleta.model.Insumo;
import com.lacaleta.service.InsumoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/insumos")
public class AdminInsumoController {

    @Autowired
    private InsumoService insumoService;

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(value = "id", required = false) Integer id,
            @RequestParam("nombre") String nombre,
            @RequestParam("stockActual") Double stockActual,
            @RequestParam("unidadMedida") String unidadMedida,
            @RequestParam(value = "stockMinimo", defaultValue = "5.0") Double stockMinimo,
            RedirectAttributes flash) {

        Insumo insumo = new Insumo();
        insumo.setId(id);
        insumo.setNombre(nombre.trim());
        insumo.setStockActual(stockActual);
        insumo.setUnidadMedida(unidadMedida);
        insumo.setStockMinimo(stockMinimo);

        insumoService.guardar(insumo);
        flash.addFlashAttribute("exito", "Insumo registrado/actualizado correctamente.");
        return "redirect:/admin#stock";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        if (insumoService.eliminar(id)) {
            flash.addFlashAttribute("exito", "Insumo eliminado correctamente.");
        } else {
            flash.addFlashAttribute("error", "No se pudo encontrar el insumo a eliminar.");
        }
        return "redirect:/admin#stock";
    }
}