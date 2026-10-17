package com.lacaleta.repository;

import com.lacaleta.model.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Integer> {
    boolean existsByCodigoIgnoreCase(String codigo);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from Insumo i where i.id = :id")
    java.util.Optional<Insumo> bloquearPorId(@org.springframework.data.repository.query.Param("id") Integer id);

    @org.springframework.data.jpa.repository.Query("select count(i) from Insumo i where i.stockActual <= i.stockMinimo")
    long contarCriticos();
    java.util.List<Insumo> findTop4ByStockActualNotNullOrderByStockActualAsc();
    java.util.List<Insumo> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(String nombre, String codigo);
}
