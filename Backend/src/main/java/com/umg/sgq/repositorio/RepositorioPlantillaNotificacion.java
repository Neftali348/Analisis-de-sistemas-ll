package com.umg.sgq.repositorio;

import com.umg.sgq.entidad.PlantillaNotificacion;
import com.umg.sgq.enumeracion.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface RepositorioPlantillaNotificacion extends JpaRepository<PlantillaNotificacion, Long> {
    Optional<PlantillaNotificacion> findByEventAndChannelAndActiveTrue(EventoNotificacion event, CanalNotificacion channel);
}
