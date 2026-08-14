package com.umg.sgq.repositorio;

import com.umg.sgq.entidad.Sucursal;
import com.umg.sgq.enumeracion.EstadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface RepositorioSucursal extends JpaRepository<Sucursal, Long> {
    boolean existsByCodeIgnoreCase(String code);

    List<Sucursal> findByStatusOrderByName(EstadoRegistro status);
}
