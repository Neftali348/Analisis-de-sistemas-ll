package com.umg.sgq.controlador;

import com.umg.sgq.dto.DtosNotificaciones.*;
import com.umg.sgq.servicio.ServicioAdministracionNotificaciones;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("@authz.has('NOTIFICATION_ADMIN')")
public class ControladorAdministracionNotificaciones {
    private final ServicioAdministracionNotificaciones service;

    public ControladorAdministracionNotificaciones(ServicioAdministracionNotificaciones service) {
        this.service = service;
    }

    @GetMapping("/templates")
    public List<VistaPlantilla> templates() {
        return service.templates();
    }

    @PutMapping("/templates/{id}")
    public VistaPlantilla update(@PathVariable Long id, @Valid @RequestBody SolicitudPlantilla r, HttpServletRequest req) {
        return service.update(id, r, req);
    }

    @GetMapping
    public List<VistaNotificacion> history() {
        return service.history();
    }

    @PostMapping("/{id}/resend")
    public VistaNotificacion resend(@PathVariable Long id, @RequestBody(required = false) SolicitudReenvio r, HttpServletRequest req) {
        return service.resend(id, r == null ? null : r.reason(), req);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, String> cancel(@PathVariable Long id, @Valid @RequestBody SolicitudCancelacion r, HttpServletRequest req) {
        service.cancel(id, r.reason(), req);
        return Map.of("message", "Notificación cancelada.");
    }
}
