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
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Cupon c where lower(c.codigo) = lower(:codigo)")
    Optional<Cupon> bloquearPorCodigo(@org.springframework.data.repository.query.Param("codigo") String codigo);
    List<Cupon> findByUsuarioOrderByFechaCanjeDesc(Usuario usuario);
    List<Cupon> findByUsuarioAndEstadoOrderByFechaCanjeDesc(Usuario usuario, String estado);
    Optional<Cupon> findByCodigo(String codigo);

    @org.springframework.data.jpa.repository.Query("select c from Cupon c where lower(trim(c.codigo)) = lower(trim(:codigo))")
    Optional<Cupon> findByCodigoIgnoreCase(@org.springframework.data.repository.query.Param("codigo") String codigo);
    long countByUsuarioAndEstado(Usuario usuario, String estado);

    @Transactional
    void deleteByUsuario(Usuario usuario);
}
