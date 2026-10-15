package com.lacaleta.service;

import com.lacaleta.model.Insumo;
import com.lacaleta.repository.InsumoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InsumoService {

    @Autowired
    private InsumoRepository insumoRepository;

    public List<Insumo> listar() {
        return insumoRepository.findAll();
    }

    public List<Insumo> listarConFiltro(String buscar) {
        if (buscar == null || buscar.isBlank()) {
            return listar();
        }
        String filtro = buscar.toLowerCase().trim();
        return insumoRepository.findAll().stream()
                .filter(i -> i.getNombre().toLowerCase().contains(filtro)
                        || i.getCodigo().toLowerCase().contains(filtro))
                .collect(Collectors.toList());
    }

    public void guardar(Insumo insumo) {
        insumoRepository.save(insumo);
    }

    public Optional<Insumo> buscarPorId(Integer id) {
        return insumoRepository.findById(id);
    }

    public boolean eliminar(Integer id) {
        if (insumoRepository.existsById(id)) {
            insumoRepository.deleteById(id);
            return true;
        }
        return false;
    }
}