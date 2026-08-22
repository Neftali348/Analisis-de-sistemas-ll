package com.umg.sgq.servicio;

import com.umg.sgq.dto.DtosAutenticacion.*;
import com.umg.sgq.entidad.Usuario;
import com.umg.sgq.enumeracion.*;
import com.umg.sgq.repositorio.RepositorioUsuario;
import com.umg.sgq.seguridad.ServicioJwt;
import com.umg.sgq.seguridad.ServicioUsuarioActual;
import com.umg.sgq.utilidad.UtilidadHash;
import com.umg.sgq.utilidad.UtilidadClaveAleatoria;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
public class ServicioAutenticacion {
    private static final Pattern PWD = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
    private final RepositorioUsuario users;
    private final PasswordEncoder encoder;
    private final ServicioJwt jwt;
    private final ServicioAuditoria audit;
    private final ServicioUsuarioActual current;
    private final JavaMailSender mail;
    private final int maxAttempts;
    private final int lockMinutes;
    private final boolean mailEnabled;
    private final String mailFrom;
    private final String frontendUrl;

    public ServicioAutenticacion(RepositorioUsuario users, PasswordEncoder encoder, ServicioJwt jwt, ServicioAuditoria audit, ServicioUsuarioActual current, JavaMailSender mail, @Value("${app.security.max-failed-attempts:5}") int maxAttempts, @Value("${app.security.lock-minutes:15}") int lockMinutes, @Value("${app.mail.enabled:false}") boolean mailEnabled, @Value("${app.mail.from:no-reply@sgq.local}") String mailFrom, @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        this.current = current;
        this.mail = mail;
        this.maxAttempts = maxAttempts;
        this.lockMinutes = lockMinutes;
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom;
        this.frontendUrl = frontendUrl;
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public RespuestaInicioSesion login(
            SolicitudInicioSesion solicitud,
            HttpServletRequest peticion
    ) {

        String nombreUsuario = solicitud.username().trim();

        Usuario usuario = users
                .findByUsernameIgnoreCase(nombreUsuario)
                .orElse(null);

        LocalDateTime ahora = LocalDateTime.now();

        // =========================================================
        // FA02 - USUARIO NO EXISTE
        // =========================================================
        if (usuario == null) {

            audit.logActor(
                    peticion,
                    nombreUsuario,
                    "NO_AUTENTICADO",
                    "AUTENTICACION",
                    "INTENTO_FALLIDO",
                    "USUARIO",
                    nombreUsuario,
                    "Intento fallido con credenciales inválidas",
                    ResultadoAuditoria.FALLIDO
            );

            throw new IllegalArgumentException(
                    "Usuario o contraseña incorrectos."
            );
        }

        // =========================================================
        // FA03 - CUENTA BLOQUEADA
        // =========================================================
        if (
                usuario.getLockedUntil() != null
                        && usuario.getLockedUntil().isAfter(ahora)
        ) {

            audit.logActor(
                    peticion,
                    usuario.getUsername(),
                    usuario.getRole() != null
                            ? usuario.getRole().getCode().name()
                            : "SIN_ROL",
                    "AUTENTICACION",
                    "INTENTO_BLOQUEADO",
                    "USUARIO",
                    usuario.getId().toString(),
                    "Intento de acceso a cuenta bloqueada temporalmente",
                    ResultadoAuditoria.FALLIDO
            );

            throw new IllegalArgumentException(
                    "El usuario se encuentra inactivo o bloqueado."
            );
        }
        // =========================================================
// FA04 - ROL SIN PERMISOS
// =========================================================
        if (
                usuario.getRole().getPermissions() == null
                        || usuario.getRole().getPermissions().isEmpty()
        ) {

            audit.logActor(
                    peticion,
                    usuario.getUsername(),
                    usuario.getRole().getCode().name(),
                    "AUTENTICACION",
                    "ACCESO_DENEGADO",
                    "USUARIO",
                    usuario.getId().toString(),
                    "Usuario sin permisos de acceso",
                    ResultadoAuditoria.FALLIDO
            );

            throw new IllegalArgumentException(
                    "El usuario no posee permisos de acceso."
            );
        }

        // =========================================================
        // SI EL BLOQUEO DE 15 MINUTOS YA TERMINÓ
        // =========================================================
        if (
                usuario.getLockedUntil() != null
                        && !usuario.getLockedUntil().isAfter(ahora)
        ) {

            usuario.setLockedUntil(null);
            usuario.setFailedAttempts(0);

            users.saveAndFlush(usuario);
        }

        // =========================================================
        // FA03 - USUARIO INACTIVO
        // =========================================================
        if (usuario.getStatus() != EstadoRegistro.ACTIVO) {

            audit.logActor(
                    peticion,
                    usuario.getUsername(),
                    usuario.getRole() != null
                            ? usuario.getRole().getCode().name()
                            : "SIN_ROL",
                    "AUTENTICACION",
                    "ACCESO_DENEGADO",
                    "USUARIO",
                    usuario.getId().toString(),
                    "Intento de acceso de usuario inactivo",
                    ResultadoAuditoria.FALLIDO
            );

            throw new IllegalArgumentException(
                    "El usuario se encuentra inactivo o bloqueado."
            );
        }

        // =========================================================
        // FA04 - SIN ROL O ROL INACTIVO
        // =========================================================
        if (
                usuario.getRole() == null
                        || !usuario.getRole().isActive()
        ) {

            audit.logActor(
                    peticion,
                    usuario.getUsername(),
                    usuario.getRole() != null
                            ? usuario.getRole().getCode().name()
                            : "SIN_ROL",
                    "AUTENTICACION",
                    "ACCESO_DENEGADO",
                    "USUARIO",
                    usuario.getId().toString(),
                    "Usuario sin rol válido o rol inactivo",
                    ResultadoAuditoria.FALLIDO
            );

            throw new IllegalArgumentException(
                    "El usuario no posee permisos de acceso."
            );
        }

        // =========================================================
        // FA02 - CONTRASEÑA INCORRECTA
        // =========================================================
        if (
                !encoder.matches(
                        solicitud.password(),
                        usuario.getPasswordHash()
                )
        ) {

            int intentos =
                    usuario.getFailedAttempts() + 1;

            usuario.setFailedAttempts(intentos);

            // Registrar intento fallido
            audit.logActor(
                    peticion,
                    usuario.getUsername(),
                    usuario.getRole().getCode().name(),
                    "AUTENTICACION",
                    "INTENTO_FALLIDO",
                    "USUARIO",
                    usuario.getId().toString(),
                    "Intento fallido de inicio de sesión. Intento "
                            + intentos
                            + " de "
                            + maxAttempts,
                    ResultadoAuditoria.FALLIDO
            );

            // =====================================================
            // RN21 - BLOQUEAR DESPUÉS DEL QUINTO INTENTO
            // =====================================================
            if (intentos >= maxAttempts) {

                /*
                 * IMPORTANTE:
                 * NO lo regreses a cero aquí.
                 *
                 * Déjalo en 5 para saber por qué
                 * la cuenta quedó bloqueada.
                 */
                usuario.setFailedAttempts(maxAttempts);

                usuario.setLockedUntil(
                        ahora.plusMinutes(lockMinutes)
                );

                users.saveAndFlush(usuario);

                audit.logActor(
                        peticion,
                        usuario.getUsername(),
                        usuario.getRole().getCode().name(),
                        "AUTENTICACION",
                        "BLOQUEO",
                        "USUARIO",
                        usuario.getId().toString(),
                        "Cuenta bloqueada durante "
                                + lockMinutes
                                + " minutos por alcanzar "
                                + maxAttempts
                                + " intentos fallidos consecutivos",
                        ResultadoAuditoria.FALLIDO
                );

                throw new IllegalArgumentException(
                        "El usuario se encuentra inactivo o bloqueado."
                );
            }

            users.saveAndFlush(usuario);

            throw new IllegalArgumentException(
                    "Usuario o contraseña incorrectos."
            );
        }

        // =========================================================
        // LOGIN CORRECTO
        // =========================================================

        /*
         * Flujo normal paso 12:
         * reiniciar contador después de autenticación exitosa.
         */
        usuario.setFailedAttempts(0);
        usuario.setLockedUntil(null);

        usuario.setLastLoginAt(ahora);
        usuario.setLastActivityAt(ahora);

        users.saveAndFlush(usuario);

        // =========================================================
        // RN18/RN19 - LOGIN EXITOSO
        // =========================================================

        /*
         * Usar logActor y no log porque en este momento
         * todavía no se ha instalado el JWT en el contexto
         * de Spring Security.
         */
        audit.logActor(
                peticion,
                usuario.getUsername(),
                usuario.getRole().getCode().name(),
                "AUTENTICACION",
                "INICIO_SESION",
                "USUARIO",
                usuario.getId().toString(),
                "Inicio de sesión exitoso",
                ResultadoAuditoria.EXITOSO
        );

        var permisos =
                usuario.getRole()
                        .getPermissions()
                        .stream()
                        .map(
                                permiso ->
                                        permiso.getCode().name()
                        )
                        .collect(Collectors.toSet());

        return new RespuestaInicioSesion(
                jwt.generate(
                        usuario.getUsername(),
                        usuario.getCredentialVersion()
                ),
                usuario.getId(),
                usuario.getFullName(),
                usuario.getUsername(),
                usuario.getRole().getCode().name(),

                usuario.getBranch() == null
                        ? null
                        : usuario.getBranch().getId(),

                permisos,

                usuario.isMustChangePassword(),

                usuario.isMustChangePassword()
                        ? "Debe cambiar la contraseña temporal antes de continuar."
                        : "Inicio de sesión realizado con éxito."
        );
    }

    @Transactional
    public void logout(HttpServletRequest peticion) {

        Usuario usuario = current.require();

        // Invalida todos los JWT emitidos con la versión anterior.
        usuario.setCredentialVersion(
                usuario.getCredentialVersion() + 1
        );

        usuario.setLastActivityAt(null);

        users.saveAndFlush(usuario);

        audit.logActor(
                peticion,
                usuario.getUsername(),
                usuario.getRole().getCode().name(),
                "AUTENTICACION",
                "CIERRE_SESION",
                "USUARIO",
                usuario.getId().toString(),
                "Cierre de sesión",
                ResultadoAuditoria.EXITOSO
        );
    }

    @Transactional
    public void expirarSesion(HttpServletRequest peticion) {

        Usuario usuario = current.require();

        usuario.setCredentialVersion(
                usuario.getCredentialVersion() + 1
        );

        usuario.setLastActivityAt(null);

        users.saveAndFlush(usuario);

        audit.logActor(
                peticion,
                usuario.getUsername(),
                usuario.getRole().getCode().name(),
                "AUTENTICACION",
                "EXPIRACION",
                "SESION",
                usuario.getId().toString(),
                "Sesión expirada por 15 minutos de inactividad",
                ResultadoAuditoria.EXITOSO
        );
    }
    @Transactional
    public void registrarAccesoDenegado(
            String permiso,
            String ruta,
            HttpServletRequest peticion
    ) {

        Usuario usuario = current.require();

        audit.logActor(
                peticion,
                usuario.getUsername(),
                usuario.getRole().getCode().name(),
                "AUTORIZACION",
                "ACCESO_DENEGADO",
                "RUTA",
                ruta == null ? "NO_IDENTIFICADA" : ruta,
                "Intento de acceso sin permiso requerido: "
                        + permiso,
                ResultadoAuditoria.FALLIDO
        );
    }


    @Transactional
    public String forgotPassword(SolicitudRecuperarContrasena r, HttpServletRequest req) {
        Usuario u = users.findByUsernameIgnoreCase(r.usernameOrEmail().trim()).orElseGet(() -> users.findByEmailIgnoreCase(r.usernameOrEmail().trim()).orElse(null));
        if (u != null && u.getStatus() == EstadoRegistro.ACTIVO) {
            String token = UtilidadClaveAleatoria.trackingKey();
            u.setPasswordResetTokenHash(UtilidadHash.sha256(token));
            u.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(30));
            users.save(u);
            if (mailEnabled) {
                try {
                    SimpleMailMessage m = new SimpleMailMessage();
                    m.setFrom(mailFrom);
                    m.setTo(u.getEmail());
                    m.setSubject("SGQ - Recuperación de contraseña");
                    m.setText("Use este enlace durante los próximos 30 minutos: " + frontendUrl + "/restablecer-contrasena?token=" + token);
                    mail.send(m);
                } catch (Exception ignored) {
                }
            }
            audit.logActor(null, u.getUsername(), u.getRole().getCode().name(), "AUTENTICACION", "SOLICITAR_RECUPERACION", "USUARIO", u.getId().toString(), "Solicitud de recuperación de contraseña", ResultadoAuditoria.EXITOSO);
        } else {
            audit.logSystem("AUTENTICACION", "SOLICITAR_RECUPERACION", "USUARIO", "NO_IDENTIFICADO", "Solicitud de recuperación sin coincidencia con una cuenta activa", ResultadoAuditoria.FALLIDO);
        }
        return "Si la información coincide con una cuenta activa, se enviarán instrucciones al correo institucional registrado.";
    }

    @Transactional
    public void changePassword(SolicitudCambiarContrasena r, HttpServletRequest req) {
        Usuario u = current.require();
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash()))
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        validateNewPassword(r.newPassword(), r.confirmPassword());
        u.setPasswordHash(encoder.encode(r.newPassword()));
        u.setMustChangePassword(false);
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        u.setCredentialsChangedAt(LocalDateTime.now());
        u.setCredentialVersion(u.getCredentialVersion() + 1);
        users.save(u);
        audit.log(
                req,
                "AUTENTICACION",
                "CAMBIO_PASSWORD",
                "USUARIO",
                u.getId().toString(),
                "Contraseña actualizada por el usuario",
                ResultadoAuditoria.EXITOSO,
                null,
                null
        );
    }

    @Transactional
    public void resetPassword(SolicitudRestablecerContrasena r, HttpServletRequest req) {
        validateNewPassword(r.newPassword(), r.confirmPassword());
        String h = UtilidadHash.sha256(r.token().trim());
        Usuario u = users.findAll().stream().filter(x -> h.equals(x.getPasswordResetTokenHash()) && x.getPasswordResetExpiresAt() != null && x.getPasswordResetExpiresAt().isAfter(LocalDateTime.now()) && x.getStatus() == EstadoRegistro.ACTIVO).findFirst().orElseThrow(() -> new IllegalArgumentException("El enlace de recuperación no es válido o ha expirado."));
        u.setPasswordHash(encoder.encode(r.newPassword()));
        u.setMustChangePassword(false);
        u.setCredentialsChangedAt(LocalDateTime.now());
        u.setCredentialVersion(u.getCredentialVersion() + 1);
        u.setPasswordResetTokenHash(null);
        u.setPasswordResetExpiresAt(null);
        u.setFailedAttempts(0);
        u.setLockedUntil(null);
        users.save(u);
        audit.logActor(req, u.getUsername(), u.getRole().getCode().name(), "AUTENTICACION", "RESTABLECER_PASSWORD", "USUARIO", u.getId().toString(), "Contraseña restablecida mediante enlace temporal", ResultadoAuditoria.EXITOSO);
    }

    private void validateNewPassword(String password, String confirm) {
        if (!password.equals(confirm)) throw new IllegalArgumentException("La confirmación de contraseña no coincide.");
        if (!PWD.matcher(password).matches())
            throw new IllegalArgumentException("La nueva contraseña debe tener mínimo 8 caracteres, mayúscula, minúscula, número y carácter especial.");
    }

}
