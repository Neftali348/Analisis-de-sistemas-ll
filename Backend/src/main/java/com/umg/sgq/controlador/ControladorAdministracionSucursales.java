package com.umg.sgq.controlador;

import com.umg.sgq.dto.DtosAdministracion.*;
import com.umg.sgq.enumeracion.EstadoRegistro;
import com.umg.sgq.servicio.ServicioAdministracionSucursales;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/branches")
@PreAuthorize("@authz.has('BRANCH_ADMIN')")
public class ControladorAdministracionSucursales {
    private final ServicioAdministracionSucursales service;

    public ControladorAdministracionSucursales(ServicioAdministracionSucursales service) {
        this.service = service;
    }

    @GetMapping
    public List<VistaSucursal> list() {
        return service.list();
    }

    @PostMapping
    public VistaSucursal create(@Valid @RequestBody SolicitudSucursal r, HttpServletRequest req) {
        return service.create(r, req);
    }

    @PutMapping("/{id}")
    public VistaSucursal update(@PathVariable Long id, @Valid @RequestBody SolicitudSucursal r, HttpServletRequest req) {
        return service.update(id, r, req);
    }

    @PatchMapping("/{id}/status")
    public VistaSucursal status(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest req) {
        return service.changeStatus(id, EstadoRegistro.valueOf(body.get("status")), req);
    }
}
