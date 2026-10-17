package com.lacaleta.repository;

import com.lacaleta.model.Cupon;
import com.lacaleta.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuponRepository extends JpaRepository<Cupon, Integer> {
    List<Cupon> findByUsuarioOrderByFechaCanjeDesc(Usuario usuario);
    List<Cupon> findByUsuarioAndEstadoOrderByFechaCanjeDesc(Usuario usuario, String estado);
    Optional<Cupon> findByCodigo(String codigo);
    long countByUsuarioAndEstado(Usuario usuario, String estado);

    @Transactional
    void deleteByUsuario(Usuario usuario);
}
