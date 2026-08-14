package com.umg.sgq.servicio;

import com.umg.sgq.dto.DtosAdministracion.*;
import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.*;
import com.umg.sgq.repositorio.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ServicioAdministracionRoles {
    private final RepositorioRol roles;
    private final RepositorioPermiso permissions;
    private final RepositorioUsuario users;
    private final ServicioAuditoria audit;

    public ServicioAdministracionRoles(RepositorioRol roles, RepositorioPermiso permissions, RepositorioUsuario users, ServicioAuditoria audit) {
        this.roles = roles;
        this.permissions = permissions;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<VistaRol> list() {
        return roles.findAll().stream().sorted(Comparator.comparing(x -> x.getCode().name())).map(this::view).toList();
    }

    @Transactional
    public VistaRol updatePermissions(CodigoRol code, SolicitudPermisosRol r, HttpServletRequest req) {
        Rol role = roles.findByCode(code).orElseThrow();
        Set<CodigoPermiso> requested = EnumSet.noneOf(CodigoPermiso.class);
        requested.addAll(r.permissions());
        Set<CodigoPermiso> allowed = allowed(code);
        Set<CodigoPermiso> required = required(code);
        if (!allowed.containsAll(requested))
            throw new IllegalArgumentException("La matriz solicitada contiene permisos incompatibles con el rol protegido.");
        if (!requested.containsAll(required))
            throw new IllegalArgumentException("No se pueden retirar permisos mínimos obligatorios de este rol.");
        validateDependencies(requested);
        Set<String> old = role.getPermissions().stream().map(p -> p.getCode().name()).collect(Collectors.toSet());
        role.getPermissions().clear();
        for (CodigoPermiso pc : requested) role.getPermissions().add(permissions.findByCode(pc).orElseThrow());
        roles.save(role);
        revokeRoleSessions(code);
        audit.log(req, "ROLES", "ACTUALIZAR_PERMISOS", "ROL", code.name(), "Permisos actualizados en rol protegido", ResultadoAuditoria.EXITOSO, old, requested);
        return view(role);
    }

    @Transactional
    public VistaRol changeStatus(CodigoRol code, boolean active, HttpServletRequest req) {
        Rol r = roles.findByCode(code).orElseThrow();
        if (code == CodigoRol.ADMINISTRADOR && !active)
            throw new IllegalArgumentException("El rol Administrador no puede inactivarse.");
        boolean old = r.isActive();
        r.setActive(active);
        roles.save(r);
        revokeRoleSessions(code);
        audit.log(req, "ROLES", "CAMBIO_ESTADO", "ROL", code.name(), "Estado del rol modificado", ResultadoAuditoria.EXITOSO, Map.of("activo", old), Map.of("activo", active));
        return view(r);
    }

    private void revokeRoleSessions(CodigoRol code) {
        for (Usuario u : users.findAll()) {
            if (u.getRole().getCode() == code) {
                u.setCredentialVersion(u.getCredentialVersion() + 1);
                u.setLastActivityAt(null);
                users.save(u);
            }
        }
    }

    private void validateDependencies(Set<CodigoPermiso> p) {
        if (p.contains(CodigoPermiso.CASE_CLOSE) && !p.contains(CodigoPermiso.CASE_VIEW))
            throw new IllegalArgumentException("Cerrar casos requiere Consultar casos.");
        if (p.contains(CodigoPermiso.CASE_ASSIGN) && !p.contains(CodigoPermiso.CASE_VIEW))
            throw new IllegalArgumentException("Asignar casos requiere Consultar casos.");
        if (p.contains(CodigoPermiso.REPORT_EXPORT) && !p.contains(CodigoPermiso.REPORT_VIEW))
            throw new IllegalArgumentException("Exportar reportes requiere Consultar reportes.");
        if (p.contains(CodigoPermiso.AUDIT_EXPORT) && !p.contains(CodigoPermiso.AUDIT_VIEW))
            throw new IllegalArgumentException("Exportar auditoría requiere Consultar bitácora.");
    }

    private Set<CodigoPermiso> allowed(CodigoRol r) {
        switch (r) {
            case AGENTE_ATENCION:
                return EnumSet.of(CodigoPermiso.CASE_VIEW, CodigoPermiso.CASE_FOLLOWUP, CodigoPermiso.CASE_RESOLVE, CodigoPermiso.EVIDENCE_UPLOAD, CodigoPermiso.EVIDENCE_DOWNLOAD);
            case SUPERVISOR:
                return EnumSet.of(CodigoPermiso.CASE_VIEW, CodigoPermiso.CASE_UPDATE, CodigoPermiso.CASE_ASSIGN, CodigoPermiso.CASE_FOLLOWUP, CodigoPermiso.CASE_CLOSE, CodigoPermiso.CASE_REOPEN, CodigoPermiso.CASE_PRIORITY, CodigoPermiso.EVIDENCE_UPLOAD, CodigoPermiso.EVIDENCE_DOWNLOAD, CodigoPermiso.REPORT_VIEW, CodigoPermiso.REPORT_EXPORT);
            case ADMINISTRADOR:
                return EnumSet.allOf(CodigoPermiso.class);
            case AUDITOR:
                return EnumSet.of(CodigoPermiso.AUDIT_VIEW, CodigoPermiso.AUDIT_EXPORT, CodigoPermiso.REPORT_VIEW, CodigoPermiso.REPORT_EXPORT);
            default:
                throw new IllegalArgumentException("Rol no soportado: " + r);
        }
    }

    private Set<CodigoPermiso> required(CodigoRol r) {
        switch (r) {
            case AGENTE_ATENCION:
                return EnumSet.of(CodigoPermiso.CASE_VIEW, CodigoPermiso.CASE_FOLLOWUP, CodigoPermiso.CASE_RESOLVE, CodigoPermiso.EVIDENCE_UPLOAD, CodigoPermiso.EVIDENCE_DOWNLOAD);
            case SUPERVISOR:
                return EnumSet.of(CodigoPermiso.CASE_VIEW, CodigoPermiso.CASE_ASSIGN, CodigoPermiso.CASE_FOLLOWUP, CodigoPermiso.CASE_CLOSE, CodigoPermiso.CASE_REOPEN, CodigoPermiso.CASE_PRIORITY, CodigoPermiso.REPORT_VIEW);
            case ADMINISTRADOR:
                return EnumSet.of(CodigoPermiso.CASE_VIEW, CodigoPermiso.CASE_CLOSE, CodigoPermiso.CASE_REOPEN, CodigoPermiso.USER_ADMIN, CodigoPermiso.ROLE_ADMIN, CodigoPermiso.BRANCH_ADMIN, CodigoPermiso.REPORT_VIEW, CodigoPermiso.AUDIT_VIEW, CodigoPermiso.NOTIFICATION_ADMIN);
            case AUDITOR:
                return EnumSet.of(CodigoPermiso.AUDIT_VIEW, CodigoPermiso.REPORT_VIEW);
            default:
                throw new IllegalArgumentException("Rol no soportado: " + r);
        }
    }

    private VistaRol view(Rol r) {
        return new VistaRol(r.getId(), r.getCode(), r.getName(), r.getDescription(), r.isActive(), r.isProtectedRole(), r.getPermissions().stream().map(Permiso::getCode).collect(Collectors.toCollection(() -> EnumSet.noneOf(CodigoPermiso.class))));
    }
}
