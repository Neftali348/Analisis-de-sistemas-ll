package com.umg.sgq.controlador;

import com.umg.sgq.dto.DtosAdministracion.*;
import com.umg.sgq.enumeracion.EstadoRegistro;
import com.umg.sgq.servicio.ServicioAdministracionUsuarios;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("@authz.has('USER_ADMIN')")
public class ControladorAdministracionUsuarios {
    private final ServicioAdministracionUsuarios service;

    public ControladorAdministracionUsuarios(ServicioAdministracionUsuarios service) {
        this.service = service;
    }

    @GetMapping
    public List<VistaUsuario> list() {
        return service.list();
    }

    @PostMapping
    public RespuestaCreacionUsuario create(@Valid @RequestBody SolicitudUsuario r, HttpServletRequest req) {
        return service.create(r, req);
    }

    @PutMapping("/{id}")
    public VistaUsuario update(@PathVariable Long id, @Valid @RequestBody SolicitudUsuario r, HttpServletRequest req) {
        return service.update(id, r, req);
    }

    @PatchMapping("/{id}/status")
    public VistaUsuario status(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest req) {
        return service.changeStatus(id, EstadoRegistro.valueOf(body.get("status")), req);
    }

    @PostMapping("/{id}/unlock")
    public VistaUsuario unlock(@PathVariable Long id, HttpServletRequest req) {
        return service.unlock(id, req);
    }

    @PostMapping("/{id}/reset-password")
    public Map<String, String> reset(@PathVariable Long id, HttpServletRequest req) {
        return Map.of("temporaryPassword", service.resetPassword(id, req), "message", "Contraseña temporal generada. Entréguela al usuario por un canal seguro.");
    }
}
