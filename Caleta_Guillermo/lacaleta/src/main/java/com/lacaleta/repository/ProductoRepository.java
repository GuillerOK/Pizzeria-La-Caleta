package com.lacaleta.repository;

import com.lacaleta.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    java.util.Optional<Producto> findByNombreIgnoreCase(String nombre);
    long countByDisponibleTrue();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Producto p where p.id = :id")
    java.util.Optional<Producto> bloquearPorId(@org.springframework.data.repository.query.Param("id") Integer id);
    List<Producto> findByCategoriaId(Integer categoriaId);
}
