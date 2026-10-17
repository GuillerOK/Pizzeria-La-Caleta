package com.lacaleta.controller;

import com.lacaleta.model.Producto;
import com.lacaleta.service.CategoriaService;
import com.lacaleta.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/admin/pizzas")
public class AdminProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaService categoriaService;

    // POST: /admin/pizzas/guardar (Crear nueva pizza)
    @PostMapping("/guardar")
    public String guardar(
            @RequestParam("nombre") String nombre,
            @RequestParam("precio") Double precio,
            @RequestParam(value = "stock", defaultValue = "10") Integer stock,
            @RequestParam(value = "imagenUrl", required = false) String imagenUrl,
            @RequestParam(value = "imagenArchivo", required = false) MultipartFile imagenArchivo,
            @RequestParam(value = "codigoCategoria", defaultValue = "clasicas") String codigoCategoria,
            @RequestParam(value = "tamano", defaultValue = "Familiar") String tamano,
            RedirectAttributes flash) {

        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100) {
            flash.addFlashAttribute("error", "El nombre de la pizza es obligatorio.");
            return "redirect:/admin#productos";
        }
        if (precio == null || !Double.isFinite(precio) || precio <= 0 || stock == null || stock < 0
                || !com.lacaleta.service.ImagenProductoStorage.urlValida(imagenUrl)) {
            flash.addFlashAttribute("error", "El precio debe ser mayor a 0.");
            return "redirect:/admin#productos";
        }

        var catOpt = categoriaService.buscarPorCodigo(codigoCategoria);

        Producto pizza = new Producto();
        pizza.setNombre(nombre.trim());
        pizza.setPrecio(precio);
        pizza.setStock(stock);
        pizza.setDescripcion("Pizza artesanal horneada a la leña.");
        pizza.setTamano(tamano);
        pizza.setDisponible(true);
        pizza.setCategoria(catOpt.orElse(null));

        String rutaImagenFinal = "/images/producto_americana.jpg";

        if (imagenArchivo != null && !imagenArchivo.isEmpty()) {
            try {
                rutaImagenFinal = com.lacaleta.service.ImagenProductoStorage.guardar(imagenArchivo);
            } catch (IOException e) {
                e.printStackTrace();
                flash.addFlashAttribute("error", "Error al procesar la subida de la imagen.");
                return "redirect:/admin#productos";
            }
        } else if (imagenUrl != null && !imagenUrl.isBlank()) {
            rutaImagenFinal = imagenUrl.trim();
        }

        pizza.setImagenUrl(rutaImagenFinal);

        productoService.guardar(pizza);
        flash.addFlashAttribute("exito", "Pizza registrada correctamente.");
        return "redirect:/admin#productos";
    }

    // POST: /admin/pizzas/editar (Modificar pizza desde el modal de edición)
    @PostMapping("/editar")
    public String editar(
            @RequestParam("id") Integer id,
            @RequestParam("nombre") String nombre,
            @RequestParam("precio") Double precio,
            @RequestParam(value = "stock", defaultValue = "10") Integer stock,
            @RequestParam(value = "imagenUrl", required = false) String imagenUrl,
            @RequestParam(value = "imagenArchivo", required = false) MultipartFile imagenArchivo,
            @RequestParam(value = "codigoCategoria", required = false) String codigoCategoria,
            @RequestParam(value = "tamano", required = false) String tamano,
            @RequestParam(value = "disponible", defaultValue = "true") Boolean disponible,
            RedirectAttributes flash) {

        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100
                || precio == null || !Double.isFinite(precio) || precio <= 0 || stock == null || stock < 0
                || !com.lacaleta.service.ImagenProductoStorage.urlValida(imagenUrl)) {
            flash.addFlashAttribute("error", "Nombre, precio o stock inválidos.");
            return "redirect:/admin#productos";
        }
        var existente = productoService.buscarPorId(id);
        if (existente.isEmpty()) {
            flash.addFlashAttribute("error", "No se encontró la pizza a editar.");
            return "redirect:/admin#productos";
        }

        Producto pizza = existente.get();
        pizza.setNombre(nombre.trim());
        pizza.setPrecio(precio);
        pizza.setStock(stock);
        pizza.setDisponible(disponible);
        if (tamano != null && !tamano.isBlank()) {
            pizza.setTamano(tamano);
        }
        if (codigoCategoria != null && !codigoCategoria.isBlank()) {
            var catOpt = categoriaService.buscarPorCodigo(codigoCategoria);
            pizza.setCategoria(catOpt.orElse(pizza.getCategoria()));
        }

        // Si sube un archivo nuevo, lo procesamos
        if (imagenArchivo != null && !imagenArchivo.isEmpty()) {
            try {
                pizza.setImagenUrl(com.lacaleta.service.ImagenProductoStorage.guardar(imagenArchivo));
            } catch (IOException e) {
                e.printStackTrace();
                flash.addFlashAttribute("error", "Error al procesar la nueva imagen.");
                return "redirect:/admin#productos";
            }
        } else if (imagenUrl != null && !imagenUrl.isBlank()) {
            // Solo se actualiza si pasaron una URL de imagen válida
            pizza.setImagenUrl(imagenUrl.trim());
        }

        productoService.guardar(pizza);
        flash.addFlashAttribute("exito", "Pizza actualizada correctamente.");
        return "redirect:/admin#productos";
    }

    // POST: eliminar desde el formulario de confirmación.
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes flash) {
        if (productoService.eliminar(id)) {
            flash.addFlashAttribute("exito", "Pizza eliminada correctamente.");
        } else {
            flash.addFlashAttribute("error", "No se pudo encontrar la pizza a eliminar.");
        }
        return "redirect:/admin#productos";
    }
}
