package com.umg.sgq.repositorio;

import com.umg.sgq.entidad.Satisfaccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioSatisfaccion extends JpaRepository<Satisfaccion, Long> {
    boolean existsByComplaintCaseId(Long caseId);
}
