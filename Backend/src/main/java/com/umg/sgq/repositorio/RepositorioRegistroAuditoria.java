package com.umg.sgq.repositorio;

import com.umg.sgq.entidad.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RepositorioRegistroAuditoria
        extends JpaRepository<RegistroAuditoria, Long>,
        JpaSpecificationExecutor<RegistroAuditoria> {

    Optional<RegistroAuditoria> findTopByOrderByIdDesc();
}