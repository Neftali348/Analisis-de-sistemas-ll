package com.umg.sgq.seguridad;

import com.umg.sgq.entidad.Usuario;
import com.umg.sgq.enumeracion.ResultadoAuditoria;
import com.umg.sgq.enumeracion.EstadoRegistro;
import com.umg.sgq.repositorio.RepositorioUsuario;
import com.umg.sgq.servicio.ServicioAuditoria;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class FiltroAutenticacionJwt extends OncePerRequestFilter {

    private final ServicioJwt jwt;
    private final RepositorioUsuario users;
    private final long inactivityMinutes;
    private final ServicioAuditoria audit;

    public FiltroAutenticacionJwt(
            ServicioJwt jwt,
            RepositorioUsuario users,
            @Value("${app.security.inactivity-minutes:15}")
            long inactivityMinutes,
            ServicioAuditoria audit
    ) {
        this.jwt = jwt;
        this.users = users;
        this.inactivityMinutes = inactivityMinutes;
        this.audit = audit;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain
    ) throws ServletException, IOException {

        String header = req.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            if (jwt.valid(token)) {

                users.findByUsernameIgnoreCase(jwt.username(token))
                        .ifPresent(user -> autenticar(user, token, req));
            }
        }

        chain.doFilter(req, res);
    }

    private void autenticar(
            Usuario user,
            String token,
            HttpServletRequest req
    ) {

        if (user.getStatus() != EstadoRegistro.ACTIVO) {
            return;
        }

        if (!user.getRole().isActive()) {
            return;
        }

        if (jwt.credentialVersion(token)
                != user.getCredentialVersion()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        /*
         * RN de sesión:
         * Expirar después de 15 minutos de inactividad.
         */
        if (user.getLastActivityAt() != null
                && user.getLastActivityAt()
                .plusMinutes(inactivityMinutes)
                .isBefore(now)) {

            try {
                audit.logActor(
                        req,
                        user.getUsername(),
                        user.getRole().getCode().name(),
                        "AUTENTICACION",
                        "EXPIRACION",
                        "SESION",
                        user.getId().toString(),
                        "Sesión expirada por inactividad",
                        ResultadoAuditoria.FALLIDO
                );
            } catch (Exception ignored) {
            }

            /*
             * IMPORTANTE:
             * No usar users.save(user).
             *
             * La actualización directa evita conflictos
             * de @Version entre peticiones simultáneas.
             */
            users.clearLastActivity(user.getId());

            return;
        }

        /*
         * Antes:
         *
         * user.setLastActivityAt(now);
         * users.save(user);
         *
         * Eso provocaba:
         * ObjectOptimisticLockingFailureException
         *
         * Ahora hacemos un UPDATE directo.
         */
        users.updateLastActivity(
                user.getId(),
                now
        );

        if (user.isMustChangePassword()
                && !Set.of(
                "/api/auth/change-password",
                "/api/auth/logout",
                "/api/auth/touch"
        ).contains(req.getRequestURI())) {

            return;
        }

        List<SimpleGrantedAuthority> authorities =
                new ArrayList<>();

        authorities.add(
                new SimpleGrantedAuthority(
                        "ROLE_" +
                                user.getRole().getCode().name()
                )
        );

        user.getRole()
                .getPermissions()
                .forEach(permission ->
                        authorities.add(
                                new SimpleGrantedAuthority(
                                        permission.getCode().name()
                                )
                        )
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        authorities
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
    }
}