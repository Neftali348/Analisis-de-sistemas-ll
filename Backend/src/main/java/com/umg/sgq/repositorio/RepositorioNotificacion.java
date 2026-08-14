package com.umg.sgq.repositorio;

import com.umg.sgq.entidad.Notificacion;
import com.umg.sgq.enumeracion.EstadoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface RepositorioNotificacion extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findTop200ByOrderByCreatedAtDesc();

    List<Notificacion> findTop100ByStatusOrderByCreatedAtAsc(EstadoNotificacion status);

    List<Notificacion> findByRecipientUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, EstadoNotificacion status);
}
