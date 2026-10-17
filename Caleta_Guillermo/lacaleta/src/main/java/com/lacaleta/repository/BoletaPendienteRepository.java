package com.lacaleta.repository;

import com.lacaleta.model.BoletaPendiente;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface BoletaPendienteRepository extends JpaRepository<BoletaPendiente, Long> {
    @Query(value = "select * from boletas_pendientes where proximo_intento <= CURRENT_TIMESTAMP order by id limit 10 for update skip locked", nativeQuery = true)
    List<BoletaPendiente> bloquearPendientes();
}
