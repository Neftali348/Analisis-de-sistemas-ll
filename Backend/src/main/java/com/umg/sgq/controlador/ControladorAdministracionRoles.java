package com.umg.sgq.controlador;

import com.umg.sgq.dto.DtosAdministracion.*;
import com.umg.sgq.enumeracion.CodigoRol;
import com.umg.sgq.servicio.ServicioAdministracionRoles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("@authz.has('ROLE_ADMIN')")
public class ControladorAdministracionRoles {
    private final ServicioAdministracionRoles service;

    public ControladorAdministracionRoles(ServicioAdministracionRoles service) {
        this.service = service;
    }

    @GetMapping
    public List<VistaRol> list() {
        return service.list();
    }

    @PutMapping("/{code}/permissions")
    public VistaRol permissions(@PathVariable CodigoRol code, @Valid @RequestBody SolicitudPermisosRol r, HttpServletRequest req) {
        return service.updatePermissions(code, r, req);
    }

    @PatchMapping("/{code}/status")
    public VistaRol status(@PathVariable CodigoRol code, @RequestBody Map<String, Boolean> body, HttpServletRequest req) {
        return service.changeStatus(code, Boolean.TRUE.equals(body.get("active")), req);
    }
}
