package com.umg.sgq.repositorio;

import com.umg.sgq.entidad.SecuenciaCaso;
import com.umg.sgq.enumeracion.TipoCaso;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RepositorioSecuenciaCaso extends JpaRepository<SecuenciaCaso, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SecuenciaCaso s where s.type=:type and s.year=:year")
    Optional<SecuenciaCaso> findForUpdate(@Param("type") TipoCaso type, @Param("year") int year);
}
