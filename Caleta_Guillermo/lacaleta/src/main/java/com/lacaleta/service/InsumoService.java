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
        return insumoRepository.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(filtro, filtro);
    }

    public Insumo guardar(Insumo insumo) {
        return insumoRepository.save(insumo);
    }

    public String generarCodigo() {
        int siguiente = insumoRepository.findAll().stream()
                .map(Insumo::getCodigo)
                .filter(codigo -> codigo != null && codigo.matches("INS-\\d+"))
                .mapToInt(codigo -> Integer.parseInt(codigo.substring(4)))
                .max().orElse(0) + 1;
        String codigo;
        do {
            codigo = String.format("INS-%02d", siguiente++);
        } while (insumoRepository.existsByCodigoIgnoreCase(codigo));
        return codigo;
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
