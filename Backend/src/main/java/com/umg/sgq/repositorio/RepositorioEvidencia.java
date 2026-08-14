package com.umg.sgq.repositorio;

import com.umg.sgq.entidad.Evidencia;
import com.umg.sgq.enumeracion.EstadoEvidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface RepositorioEvidencia extends JpaRepository<Evidencia, Long> {
    List<Evidencia> findByComplaintCaseIdOrderByUploadedAtAsc(Long caseId);

    long countByComplaintCaseIdAndFollowUpIsNullAndStatus(Long caseId, EstadoEvidencia status);

    long countByFollowUpIdAndStatus(Long followUpId, EstadoEvidencia status);

    boolean existsByComplaintCaseIdAndSha256AndStatus(Long caseId, String sha256, EstadoEvidencia status);
}
