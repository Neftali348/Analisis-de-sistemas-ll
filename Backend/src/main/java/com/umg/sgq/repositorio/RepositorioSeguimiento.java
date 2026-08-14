package com.umg.sgq.repositorio;

import com.umg.sgq.entidad.Seguimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface RepositorioSeguimiento extends JpaRepository<Seguimiento, Long> {
    List<Seguimiento> findByComplaintCaseIdOrderByCreatedAtAsc(Long caseId);

    List<Seguimiento> findByComplaintCaseIdAndVisibleToClientTrueOrderByCreatedAtAsc(Long caseId);

    long countByComplaintCaseId(Long caseId);
}
