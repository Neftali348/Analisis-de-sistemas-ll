package com.umg.sgq.servicio;

import com.umg.sgq.dto.DtosAdministracion.*;
import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.*;
import com.umg.sgq.repositorio.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ServicioAdministracionSucursales {
    private static final Set<EstadoCaso> ACTIVE_CASES = EnumSet.of(EstadoCaso.REGISTRADO, EstadoCaso.PENDIENTE_ASIGNACION, EstadoCaso.ASIGNADO, EstadoCaso.EN_PROCESO, EstadoCaso.EN_ESPERA_CLIENTE, EstadoCaso.RESUELTO, EstadoCaso.REABIERTO);
    private final RepositorioSucursal branches;
    private final RepositorioUsuario users;
    private final RepositorioCaso cases;
    private final ServicioAuditoria audit;

    public ServicioAdministracionSucursales(RepositorioSucursal branches, RepositorioUsuario users, RepositorioCaso cases, ServicioAuditoria audit) {
        this.branches = branches;
        this.users = users;
        this.cases = cases;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<VistaSucursal> list() {
        return branches.findAll().stream().sorted(Comparator.comparing(Sucursal::getName)).map(this::view).toList();
    }

    @Transactional
    public VistaSucursal create(SolicitudSucursal r, HttpServletRequest req) {
        validateUnique(r, null);
        Sucursal b = new Sucursal();
        apply(b, r);
        b = branches.save(b);
        audit.log(req, "SUCURSALES", "CREACION", "SUCURSAL", b.getId().toString(), "Sucursal creada: " + b.getCode(), ResultadoAuditoria.EXITOSO, null, Map.of("codigo", b.getCode(), "nombre", b.getName(), "ubicacion", b.getDepartment() + " / " + b.getMunicipality()));
        return view(b);
    }

    @Transactional
    public VistaSucursal update(Long id, SolicitudSucursal r, HttpServletRequest req) {
        Sucursal b = branches.findById(id).orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada."));
        validateUnique(r, id);
        Map<String, Object> old = Map.of("codigo", b.getCode(), "nombre", b.getName(), "estado", b.getStatus().name());
        if (r.status() == EstadoRegistro.INACTIVO && b.getStatus() != EstadoRegistro.INACTIVO) validateCanDeactivate(b);
        apply(b, r);
        b = branches.save(b);
        audit.log(req, "SUCURSALES", "ACTUALIZACION", "SUCURSAL", id.toString(), "Sucursal actualizada", ResultadoAuditoria.EXITOSO, old, Map.of("codigo", b.getCode(), "nombre", b.getName(), "estado", b.getStatus().name()));
        return view(b);
    }

    @Transactional
    public VistaSucursal changeStatus(Long id, EstadoRegistro status, HttpServletRequest req) {
        Sucursal b = branches.findById(id).orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada."));
        EstadoRegistro old = b.getStatus();
        if (status == EstadoRegistro.INACTIVO && old != EstadoRegistro.INACTIVO) validateCanDeactivate(b);
        b.setStatus(status);
        branches.save(b);
        audit.log(req, "SUCURSALES", "CAMBIO_ESTADO", "SUCURSAL", id.toString(), "Estado de sucursal modificado", ResultadoAuditoria.EXITOSO, Map.of("estado", old.name()), Map.of("estado", status.name()));
        return view(b);
    }

    private void validateCanDeactivate(Sucursal b) {
        long activeUsers = activeUsers(b);
        long activeCases = activeCases(b);
        if (activeCases > 0)
            throw new IllegalArgumentException("No puede inactivar la sucursal: existen " + activeCases + " casos activos. Reasigne los casos o finalice su gestión.");
        if (activeUsers > 0)
            throw new IllegalArgumentException("No puede inactivar la sucursal: existen " + activeUsers + " usuarios activos asignados. Reasígnelos o inactívelos primero.");
    }

    private void validateUnique(SolicitudSucursal r, Long id) {
        if (branches.existsByCodeIgnoreCase(r.code()) && branches.findAll().stream().anyMatch(x -> x.getCode().equalsIgnoreCase(r.code()) && !Objects.equals(x.getId(), id)))
            throw new IllegalArgumentException("Ya existe una sucursal registrada con el código ingresado.");
        boolean duplicate = branches.findAll().stream().anyMatch(x -> !Objects.equals(x.getId(), id) && x.getName().equalsIgnoreCase(r.name().trim()) && Objects.equals(norm(x.getDepartment()), norm(r.department())) && Objects.equals(norm(x.getMunicipality()), norm(r.municipality())));
        if (duplicate)
            throw new IllegalArgumentException("Ya existe una sucursal con el mismo nombre en la ubicación seleccionada.");
    }

    private void apply(Sucursal b, SolicitudSucursal r) {
        b.setCode(r.code().trim().toUpperCase(Locale.ROOT));
        b.setName(r.name().trim());
        b.setAddress(r.address().trim());
        b.setDepartment(r.department().trim());
        b.setMunicipality(r.municipality().trim());
        b.setLocationReference(blank(r.locationReference()));
        b.setPhone(blank(r.phone()));
        b.setEmail(blank(r.email()));
        b.setBusinessHours(blank(r.businessHours()));
        b.setObservations(blank(r.observations()));
        b.setStatus(r.status());
        b.setSupervisor(resolveSupervisor(r.supervisorId()));
    }

    private Usuario resolveSupervisor(Long id) {
        if (id == null) return null;
        Usuario u = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Supervisor no encontrado."));
        if (u.getStatus() != EstadoRegistro.ACTIVO || u.getRole().getCode() != CodigoRol.SUPERVISOR)
            throw new IllegalArgumentException("El responsable seleccionado no se encuentra activo o no posee el rol Supervisor.");
        return u;
    }

    private long activeUsers(Sucursal b) {
        return users.findAll().stream().filter(u -> u.getStatus() == EstadoRegistro.ACTIVO && u.getBranch() != null && Objects.equals(u.getBranch().getId(), b.getId())).count();
    }

    private long activeCases(Sucursal b) {
        return cases.findAll().stream().filter(c -> Objects.equals(c.getBranch().getId(), b.getId()) && ACTIVE_CASES.contains(c.getStatus())).count();
    }

    private String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private String norm(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    private VistaSucursal view(Sucursal b) {
        return new VistaSucursal(b.getId(), b.getCode(), b.getName(), b.getAddress(), b.getDepartment(), b.getMunicipality(), b.getLocationReference(), b.getPhone(), b.getEmail(), b.getBusinessHours(), b.getObservations(), b.getStatus(), b.getSupervisor() == null ? null : b.getSupervisor().getId(), b.getSupervisor() == null ? null : b.getSupervisor().getFullName(), activeUsers(b), activeCases(b), b.getCreatedAt() == null ? null : b.getCreatedAt().toString(), b.getUpdatedAt() == null ? null : b.getUpdatedAt().toString(), b.getVersion());
    }
}
