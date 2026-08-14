package com.umg.sgq.seguridad;

import com.umg.sgq.entidad.Usuario;
import com.umg.sgq.repositorio.RepositorioUsuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ServicioUsuarioActual {
    private final RepositorioUsuario users;

    public ServicioUsuarioActual(RepositorioUsuario users) {
        this.users = users;
    }

    public Usuario require() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal()))
            throw new IllegalStateException("Sesión no válida");
        return users.findByUsernameIgnoreCase(a.getName()).orElseThrow();
    }

    public Usuario orNull() {
        try {
            return require();
        } catch (Exception e) {
            return null;
        }
    }
}
