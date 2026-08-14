package com.umg.sgq.repositorio;

import com.umg.sgq.entidad.Rol;
import com.umg.sgq.enumeracion.CodigoRol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositorioRol extends JpaRepository<Rol, Long> {
    Optional<Rol> findByCode(CodigoRol code);
}
