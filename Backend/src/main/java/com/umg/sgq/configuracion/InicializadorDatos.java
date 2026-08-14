package com.umg.sgq.configuracion;

import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.*;
import com.umg.sgq.repositorio.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Configuration
public class InicializadorDatos {
    @Bean
    CommandLineRunner seed(RepositorioPermiso permissions, RepositorioRol roles, RepositorioSucursal branches, RepositorioUsuario users, RepositorioPlantillaNotificacion templates, PasswordEncoder encoder) {
        return args -> initialize(permissions, roles, branches, users, templates, encoder);
    }

    @Transactional
    void initialize(RepositorioPermiso permissions, RepositorioRol roles, RepositorioSucursal branches, RepositorioUsuario users, RepositorioPlantillaNotificacion templates, PasswordEncoder encoder) {
        Map<CodigoPermiso, Permiso> ps = new EnumMap<>(CodigoPermiso.class);
        for (CodigoPermiso code : CodigoPermiso.values()) {
            Permiso p = permissions.findByCode(code).orElseGet(() -> {
                Permiso x = new Permiso();
                x.setCode(code);
                x.setDescription(code.name().replace('_', ' '));
                return permissions.save(x);
            });
            ps.put(code, p);
        }
        Map<CodigoRol, Set<CodigoPermiso>> defaults = Map.of(
                CodigoRol.AGENTE_ATENCION, EnumSet.of(CodigoPermiso.CASE_VIEW, CodigoPermiso.CASE_FOLLOWUP, CodigoPermiso.CASE_RESOLVE, CodigoPermiso.EVIDENCE_UPLOAD, CodigoPermiso.EVIDENCE_DOWNLOAD),
                CodigoRol.SUPERVISOR, EnumSet.of(CodigoPermiso.CASE_VIEW, CodigoPermiso.CASE_UPDATE, CodigoPermiso.CASE_ASSIGN, CodigoPermiso.CASE_FOLLOWUP, CodigoPermiso.CASE_CLOSE, CodigoPermiso.CASE_REOPEN, CodigoPermiso.CASE_PRIORITY, CodigoPermiso.EVIDENCE_UPLOAD, CodigoPermiso.EVIDENCE_DOWNLOAD, CodigoPermiso.REPORT_VIEW, CodigoPermiso.REPORT_EXPORT),
                CodigoRol.ADMINISTRADOR, EnumSet.allOf(CodigoPermiso.class),
                CodigoRol.AUDITOR, EnumSet.of(CodigoPermiso.AUDIT_VIEW, CodigoPermiso.AUDIT_EXPORT, CodigoPermiso.REPORT_VIEW, CodigoPermiso.REPORT_EXPORT)
        );
        for (CodigoRol rc : CodigoRol.values()) {
            Rol role = roles.findByCode(rc).orElseGet(() -> {
                Rol r = new Rol();
                r.setCode(rc);
                r.setName(pretty(rc));
                r.setDescription("Rol protegido del sistema");
                r.setProtectedRole(true);
                r.setActive(true);
                return r;
            });
            if (role.getPermissions().isEmpty()) defaults.get(rc).forEach(pc -> role.getPermissions().add(ps.get(pc)));
            roles.save(role);
        }
        Sucursal branch = branches.findAll().stream().findFirst().orElseGet(() -> {
            Sucursal b = new Sucursal();
            b.setCode("CENTRAL");
            b.setName("Sucursal Central");
            b.setAddress("Ciudad de Guatemala");
            b.setDepartment("Guatemala");
            b.setMunicipality("Guatemala");
            b.setBusinessHours("Lunes a domingo 08:00-21:00");
            b.setEmail("central@sgq.local");
            b.setPhone("22223333");
            return branches.save(b);
        });
        createUser(users, roles, encoder, "Administrador del Sistema", "admin", "admin@sgq.local", "Admin#2026", CodigoRol.ADMINISTRADOR, null);
        Usuario sup = createUser(users, roles, encoder, "Supervisor General", "supervisor", "supervisor@sgq.local", "Supervisor#2026", CodigoRol.SUPERVISOR, branch);
        createUser(users, roles, encoder, "Agente de Atención", "agente", "agente@sgq.local", "Agente#2026", CodigoRol.AGENTE_ATENCION, branch);
        createUser(users, roles, encoder, "Auditor del Sistema", "auditor", "auditor@sgq.local", "Auditor#2026", CodigoRol.AUDITOR, null);
        if (branch.getSupervisor() == null) {
            branch.setSupervisor(sup);
            branches.save(branch);
        }
        for (EventoNotificacion event : EventoNotificacion.values()) {
            if (templates.findByEventAndChannelAndActiveTrue(event, CanalNotificacion.CORREO).isEmpty()) {
                PlantillaNotificacion t = new PlantillaNotificacion();
                t.setEvent(event);
                t.setChannel(CanalNotificacion.CORREO);
                t.setName("Plantilla " + event.name());
                t.setSubject("SGQ - {{evento}} - {{codigoCaso}}");
                t.setContent("Caso {{codigoCaso}}: {{mensaje}}");
                templates.save(t);
            }
        }
    }

    private Usuario createUser(RepositorioUsuario users, RepositorioRol roles, PasswordEncoder encoder, String name, String username, String email, String password, CodigoRol role, Sucursal branch) {
        return users.findByUsernameIgnoreCase(username).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setFullName(name);
            u.setUsername(username);
            u.setEmail(email);
            u.setPasswordHash(encoder.encode(password));
            u.setMustChangePassword(false);
            u.setRole(roles.findByCode(role).orElseThrow());
            u.setBranch(branch);
            u.setStatus(EstadoRegistro.ACTIVO);
            return users.save(u);
        });
    }

    private String pretty(CodigoRol rc) {
        switch (rc) {
            case AGENTE_ATENCION:
                return "Agente de Atención";
            case SUPERVISOR:
                return "Supervisor";
            case ADMINISTRADOR:
                return "Administrador";
            case AUDITOR:
                return "Auditor";
            default:
                throw new IllegalArgumentException("Rol no soportado: " + rc);
        }
    }
}
