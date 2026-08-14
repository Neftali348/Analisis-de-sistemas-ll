package com.umg.sgq.seguridad;

import com.umg.sgq.enumeracion.CodigoPermiso;
import org.springframework.stereotype.Component;

@Component("authz")
public class ServicioAutorizacion {
    private final ServicioUsuarioActual current;

    public ServicioAutorizacion(ServicioUsuarioActual current) {
        this.current = current;
    }

    public boolean has(String code) {
        try {
            CodigoPermiso p = CodigoPermiso.valueOf(code);
            return current.require().getRole().isActive() && current.require().getRole().getPermissions().stream().anyMatch(x -> x.getCode() == p);
        } catch (Exception e) {
            return false;
        }
    }
}
