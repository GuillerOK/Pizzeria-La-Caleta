package com.lacaleta.service;

import com.lacaleta.model.Categoria;
import com.lacaleta.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    public Optional<Categoria> buscarPorId(Integer id) {
        return categoriaRepository.findById(id);
    }

    public Optional<Categoria> buscarPorCodigo(String codigo) {
        return categoriaRepository.findByCodigo(codigo);
    }
}