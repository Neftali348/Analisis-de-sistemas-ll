package com.umg.sgq.repositorio;

import com.umg.sgq.entidad.Caso;
import com.umg.sgq.enumeracion.EstadoCaso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RepositorioCaso
        extends JpaRepository<Caso, Long>,
        JpaSpecificationExecutor<Caso> {

    Optional<Caso> findByCode(String code);

    long countByResponsibleIdAndSlaBreachedTrueAndStatusNotIn(
            Long userId,
            Collection<EstadoCaso> statuses
    );


    long countByResponsibleIdAndStatusNotIn(
            Long userId,
            Collection<EstadoCaso> statuses
    );

    /*
     * Consultas específicas para Dashboard.
     *
     * Evitamos la consulta anterior con parámetros NULL y LOWER(),
     * porque PostgreSQL podía interpretar el parámetro como BYTEA.
     */

    List<Caso> findAllByOrderByCreatedAtDesc();

    List<Caso> findByBranchIdOrderByCreatedAtDesc(
            Long branchId
    );

    List<Caso> findByResponsibleIdOrderByCreatedAtDesc(
            Long responsibleId
    );

    List<Caso> findByBranchIdAndResponsibleIdOrderByCreatedAtDesc(
            Long branchId,
            Long responsibleId
    );

    List<Caso> findByFirstResponseAtIsNullAndStatusNotIn(
            Collection<EstadoCaso> statuses
    );

    @Query("""
            select c
            from Caso c
            where c.createdAt between :from and :to
            """)
    List<Caso> findCreatedBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}