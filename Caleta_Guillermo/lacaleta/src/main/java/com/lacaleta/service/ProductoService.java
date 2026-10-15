package com.lacaleta.service;

import com.lacaleta.model.Categoria;
import com.lacaleta.model.Producto;
import com.lacaleta.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    public Optional<Producto> buscarPorId(Integer id) {
        return productoRepository.findById(id);
    }

    public boolean guardar(Producto datos) {
        if (datos.getDisponible() == null) {
            datos.setDisponible(true);
        }
        if (datos.getStock() == null) {
            datos.setStock(10);
        }
        productoRepository.save(datos);
        return true;
    }

    public boolean eliminar(Integer id) {
        if (productoRepository.existsById(id)) {
            productoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Producto> listarConFiltros(String codigoCategoria, Boolean disponible, String buscar) {
        return productoRepository.findAll().stream()
                .filter(p -> {
                    if (codigoCategoria != null && !codigoCategoria.isBlank()) {
                        if (p.getCategoria() == null
                                || !codigoCategoria.equalsIgnoreCase(
                                        p.getCategoria().getCodigo())) {
                            return false;
                        }
                    }
                    if (disponible != null) {
                        if (!disponible.equals(p.getDisponible())) {
                            return false;
                        }
                    }
                    if (buscar != null && !buscar.isBlank()) {
                        String query = buscar.trim().toLowerCase();
                        boolean coincideNombre = p.getNombre() != null
                                && p.getNombre().toLowerCase().contains(query);
                        boolean coincideDesc = p.getDescripcion() != null
                                && p.getDescripcion().toLowerCase().contains(query);
                        return coincideNombre || coincideDesc;
                    }
                    return true;
                })
                .toList();
    }

    public List<Producto> porCategoria(String codigoCategoria) {
        return productoRepository.findAll().stream()
                .filter(p -> p.getCategoria() != null
                        && p.getCategoria().getCodigo() != null
                        && p.getCategoria().getCodigo().trim()
                                .equalsIgnoreCase(codigoCategoria.trim()))
                .toList();
    }

    public List<Producto> obtenerClasicas() {
        List<Producto> res = new java.util.ArrayList<>();
        res.addAll(porCategoria("principales"));
        if (res.isEmpty()) {
            res.addAll(porCategoria("clasicas"));
        }
        res.addAll(porCategoria("especiales"));
        return res;
    }

    public List<Producto> obtenerCombos() {
        return porCategoria("combos");
    }

    public List<Producto> obtenerAcompanamientos() {
        List<Producto> res = porCategoria("acompanamientos");
        if (res.isEmpty())
            res = porCategoria("acompañamientos");
        return res;
    }

    public List<Producto> obtenerBebidas() {
        List<Producto> res = porCategoria("bebidas");
        if (res.isEmpty())
            res = porCategoria("bebida");
        return res;
    }

    public List<Producto> obtenerPromociones() {
        List<Producto> res = porCategoria("promociones");
        if (res.isEmpty())
            res = obtenerPromos1Persona();
        return res;
    }

    public List<Producto> obtenerPromos1Persona() {
        return productoRepository.findAll().stream()
                .filter(p -> "Personal".equalsIgnoreCase(p.getTamano()))
                .toList();
    }

    public List<Producto> obtenerPromosDuo() {
        return productoRepository.findAll().stream()
                .filter(p -> "Dúo".equalsIgnoreCase(p.getTamano()))
                .toList();
    }

    public List<Producto> obtenerPromosTrio() {
        return productoRepository.findAll().stream()
                .filter(p -> "Trío".equalsIgnoreCase(p.getTamano()))
                .toList();
    }

    public List<Producto> obtenerPromosFamiliares() {
        return productoRepository.findAll().stream()
                .filter(p -> "Familiar".equalsIgnoreCase(p.getTamano())
                        && (p.getNombre().contains("Banquete")
                        || p.getNombre().contains("Fiesta")
                        || p.getNombre().contains("Gran Cofradía")))
                .toList();
    }

    public List<Producto> obtenerFavoritas() {
        return obtenerClasicas().stream().limit(3).toList();
    }
}