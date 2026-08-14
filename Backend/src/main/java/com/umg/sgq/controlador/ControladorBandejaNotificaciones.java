package com.umg.sgq.controlador;

import com.umg.sgq.dto.DtosNotificaciones.VistaNotificacion;
import com.umg.sgq.entidad.Notificacion;
import com.umg.sgq.seguridad.ServicioUsuarioActual;
import com.umg.sgq.servicio.ServicioNotificaciones;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
public class ControladorBandejaNotificaciones {
    private final ServicioNotificaciones service;
    private final ServicioUsuarioActual current;

    public ControladorBandejaNotificaciones(ServicioNotificaciones service, ServicioUsuarioActual current) {
        this.service = service;
        this.current = current;
    }

    @GetMapping("/inbox")
    public List<VistaNotificacion> inbox() {
        return service.inbox(current.require().getId()).stream().map(this::view).toList();
    }

    private VistaNotificacion view(Notificacion n) {
        return new VistaNotificacion(n.getId(), n.getComplaintCase() == null ? null : n.getComplaintCase().getCode(), n.getEvent(), n.getChannel(), n.getStatus(), n.getRecipient(), n.getSubject(), n.getAttempts(), n.getLastError(), n.getCreatedAt(), n.getSentAt());
    }
}
