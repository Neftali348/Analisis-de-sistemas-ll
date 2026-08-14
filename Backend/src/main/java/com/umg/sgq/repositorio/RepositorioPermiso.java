package com.umg.sgq.repositorio;

import com.umg.sgq.entidad.Permiso;
import com.umg.sgq.enumeracion.CodigoPermiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositorioPermiso extends JpaRepository<Permiso, Long> {
    Optional<Permiso> findByCode(CodigoPermiso code);
}
