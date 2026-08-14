package com.umg.sgq.seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.sgq.enumeracion.ResultadoAuditoria;
import com.umg.sgq.servicio.ServicioAuditoria;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class ManejadorAccesoDenegadoRest implements AccessDeniedHandler {
    private final ObjectMapper mapper;
    private final ServicioAuditoria audit;

    public ManejadorAccesoDenegadoRest(ObjectMapper mapper, ServicioAuditoria audit) {
        this.mapper = mapper;
        this.audit = audit;
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException, ServletException {
        try {
            audit.log(req, "SEGURIDAD", "ACCESO_DENEGADO", "RUTA", req.getRequestURI(), "Intento de acceso sin permiso", ResultadoAuditoria.FALLIDO, null, null);
        } catch (Exception ignored) {
        }
        res.setStatus(403);
        res.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(res.getWriter(), Map.of("message", "No posee permisos para realizar esta acción."));
    }
}
