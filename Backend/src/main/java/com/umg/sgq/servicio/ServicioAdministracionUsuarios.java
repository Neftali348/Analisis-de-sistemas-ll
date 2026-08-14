package com.umg.sgq.servicio;

import com.umg.sgq.dto.DtosAdministracion.*;
import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.*;
import com.umg.sgq.repositorio.*;
import com.umg.sgq.utilidad.UtilidadClaveAleatoria;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class ServicioAdministracionUsuarios {
    private static final Pattern PWD = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
    private final RepositorioUsuario users;
    private final RepositorioRol roles;
    private final RepositorioSucursal branches;
    private final PasswordEncoder encoder;
    private final ServicioAuditoria audit;

    public ServicioAdministracionUsuarios(RepositorioUsuario users, RepositorioRol roles, RepositorioSucursal branches, PasswordEncoder encoder, ServicioAuditoria audit) {
        this.users = users;
        this.roles = roles;
        this.branches = branches;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<VistaUsuario> list() {
        return users.findAll().stream().sorted(Comparator.comparing(Usuario::getFullName)).map(this::view).toList();
    }

    @Transactional
    public RespuestaCreacionUsuario create(SolicitudUsuario r, HttpServletRequest req) {
        validate(r, null, true);
        Rol role = roles.findByCode(r.role()).orElseThrow();
        Sucursal branch = resolveBranch(r.role(), r.branchId());
        String pwd = r.temporaryPassword();
        boolean generated = pwd == null || pwd.isBlank();
        if (generated) pwd = UtilidadClaveAleatoria.tempPassword();
        validatePassword(pwd);
        Usuario u = new Usuario();
        apply(u, r, role, branch);
        u.setPasswordHash(encoder.encode(pwd));
        u.setMustChangePassword(true);
        u.setCredentialsChangedAt(java.time.LocalDateTime.now());
        u = users.save(u);
        audit.log(req, "USUARIOS", "CREACION", "USUARIO", u.getId().toString(), "Usuario creado: " + u.getUsername(), ResultadoAuditoria.EXITOSO, null, Map.of("usuario", u.getUsername(), "rol", u.getRole().getCode().name()));
        return new RespuestaCreacionUsuario(view(u), generated ? pwd : null);
    }

    @Transactional
    public VistaUsuario update(Long id, SolicitudUsuario r, HttpServletRequest req) {
        Usuario u = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        Map<String, Object> old = Map.of("usuario", u.getUsername(), "correo", u.getEmail(), "rol", u.getRole().getCode().name(), "estado", u.getStatus().name());
        validate(r, id, false);
        protectLastAdministrator(u, r.role(), r.status());
        Rol role = roles.findByCode(r.role()).orElseThrow();
        Sucursal branch = resolveBranch(r.role(), r.branchId());
        apply(u, r, role, branch);
        u.setCredentialsChangedAt(java.time.LocalDateTime.now());
        u.setCredentialVersion(u.getCredentialVersion() + 1);
        u.setLastActivityAt(null);
        u = users.save(u);
        audit.log(req, "USUARIOS", "ACTUALIZACION", "USUARIO", id.toString(), "Usuario actualizado", ResultadoAuditoria.EXITOSO, old, Map.of("usuario", u.getUsername(), "correo", u.getEmail(), "rol", u.getRole().getCode().name(), "estado", u.getStatus().name()));
        return view(u);
    }

    @Transactional
    public String resetPassword(Long id, HttpServletRequest req) {
        Usuario u = users.findById(id).orElseThrow();
        String pwd = UtilidadClaveAleatoria.tempPassword();
        u.setPasswordHash(encoder.encode(pwd));
        u.setMustChangePassword(true);
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        u.setLastActivityAt(null);
        u.setCredentialsChangedAt(java.time.LocalDateTime.now());
        u.setCredentialVersion(u.getCredentialVersion() + 1);
        users.save(u);
        audit.log(req, "USUARIOS", "RESTABLECER_PASSWORD", "USUARIO", id.toString(), "Contraseña restablecida por Administrador", ResultadoAuditoria.EXITOSO, null, null);
        return pwd;
    }

    @Transactional
    public VistaUsuario unlock(Long id, HttpServletRequest req) {
        Usuario u = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        users.save(u);
        audit.log(req, "USUARIOS", "DESBLOQUEO", "USUARIO", id.toString(), "Usuario desbloqueado por Administrador", ResultadoAuditoria.EXITOSO, null, null);
        return view(u);
    }

    @Transactional
    public VistaUsuario changeStatus(Long id, EstadoRegistro status, HttpServletRequest req) {
        Usuario u = users.findById(id).orElseThrow();
        EstadoRegistro old = u.getStatus();
        protectLastAdministrator(u, u.getRole().getCode(), status);
        u.setStatus(status);
        u.setCredentialsChangedAt(java.time.LocalDateTime.now());
        u.setCredentialVersion(u.getCredentialVersion() + 1);
        u.setLastActivityAt(null);
        if (status == EstadoRegistro.INACTIVO) {
            u.setLockedUntil(null);
        }
        users.save(u);
        audit.log(req, "USUARIOS", "CAMBIO_ESTADO", "USUARIO", id.toString(), "Estado de usuario modificado", ResultadoAuditoria.EXITOSO, Map.of("estado", old.name()), Map.of("estado", status.name()));
        return view(u);
    }

    private void protectLastAdministrator(Usuario target, CodigoRol newRole, EstadoRegistro newStatus) {
        if (target.getRole().getCode() != CodigoRol.ADMINISTRADOR) return;
        if (newRole == CodigoRol.ADMINISTRADOR && newStatus == EstadoRegistro.ACTIVO) return;
        long activeAdmins = users.findAll().stream().filter(x -> x.getRole().getCode() == CodigoRol.ADMINISTRADOR && x.getStatus() == EstadoRegistro.ACTIVO).count();
        if (activeAdmins <= 1)
            throw new IllegalArgumentException("Debe conservarse al menos una cuenta Administrador activa.");
    }

    private void apply(Usuario u, SolicitudUsuario r, Rol role, Sucursal branch) {
        u.setFullName(clean(r.fullName()));
        u.setUsername(r.username().trim());
        u.setEmail(r.email().trim().toLowerCase(Locale.ROOT));
        u.setRole(role);
        u.setBranch(branch);
        u.setStatus(r.status());
    }

    private void validate(SolicitudUsuario r, Long id, boolean creating) {
        if (r.fullName().trim().split("\\s+").length < 2)
            throw new IllegalArgumentException("El nombre completo debe contener al menos dos palabras.");
        users.findByUsernameIgnoreCase(r.username()).filter(x -> !Objects.equals(x.getId(), id)).ifPresent(x -> {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el nombre de usuario o correo ingresado.");
        });
        users.findByEmailIgnoreCase(r.email()).filter(x -> !Objects.equals(x.getId(), id)).ifPresent(x -> {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el nombre de usuario o correo ingresado.");
        });
        Rol role = roles.findByCode(r.role()).orElseThrow(() -> new IllegalArgumentException("Rol inexistente."));
        if (!role.isActive()) throw new IllegalArgumentException("No podrá asignarse un rol inexistente o inactivo.");
        if (creating && r.temporaryPassword() != null && !r.temporaryPassword().isBlank())
            validatePassword(r.temporaryPassword());
    }

    private Sucursal resolveBranch(CodigoRol role, Long id) {
        if (role == CodigoRol.AGENTE_ATENCION || role == CodigoRol.SUPERVISOR) {
            if (id == null) throw new IllegalArgumentException("La sucursal es obligatoria para Agente y Supervisor.");
            Sucursal b = branches.findById(id).orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada."));
            if (b.getStatus() != EstadoRegistro.ACTIVO)
                throw new IllegalArgumentException("La sucursal seleccionada se encuentra inactiva.");
            return b;
        }
        return id == null ? null : branches.findById(id).orElse(null);
    }

    private void validatePassword(String p) {
        if (!PWD.matcher(p).matches())
            throw new IllegalArgumentException("La contraseña no cumple con los requisitos de seguridad.");
    }

    private String clean(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }

    private VistaUsuario view(Usuario u) {
        return new VistaUsuario(u.getId(), u.getFullName(), u.getUsername(), u.getEmail(), u.getRole().getCode(), u.getBranch() == null ? null : u.getBranch().getId(), u.getBranch() == null ? null : u.getBranch().getName(), u.getStatus(), u.getFailedAttempts(), u.getLockedUntil() == null ? null : u.getLockedUntil().toString(), u.getLastLoginAt() == null ? null : u.getLastLoginAt().toString(), u.isMustChangePassword(), u.getVersion());
    }
}
