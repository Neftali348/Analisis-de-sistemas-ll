package com.umg.sgq.servicio;

import com.umg.sgq.dto.DtosNotificaciones.*;
import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.*;
import com.umg.sgq.repositorio.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ServicioAdministracionNotificaciones {
    private static final Set<String> ALLOWED = Set.of("{{codigoCaso}}", "{{evento}}", "{{mensaje}}");
    private final RepositorioPlantillaNotificacion templates;
    private final ServicioNotificaciones notifications;
    private final ServicioAuditoria audit;

    public ServicioAdministracionNotificaciones(RepositorioPlantillaNotificacion templates, ServicioNotificaciones notifications, ServicioAuditoria audit) {
        this.templates = templates;
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<VistaPlantilla> templates() {
        return templates.findAll().stream().sorted(Comparator.comparing(x -> x.getEvent().name())).map(this::view).toList();
    }

    @Transactional
    public VistaPlantilla update(Long id, SolicitudPlantilla r, HttpServletRequest req) {
        PlantillaNotificacion t = templates.findById(id).orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada."));
        validateContent(r.subject());
        validateContent(r.content());
        if (!r.active()) throw new IllegalArgumentException("Debe existir al menos un canal activo para este evento.");
        Map<String, Object> old = Map.of("name", t.getName(), "active", t.isActive(), "content", t.getContent());
        t.setName(r.name().trim());
        t.setSubject(blank(r.subject()));
        t.setContent(r.content().trim());
        t.setActive(true);
        templates.save(t);
        audit.log(req, "NOTIFICACIONES", "ACTUALIZAR_PLANTILLA", "PLANTILLA", id.toString(), "Plantilla de notificación actualizada", ResultadoAuditoria.EXITOSO, old, Map.of("name", t.getName(), "active", true, "content", t.getContent()));
        return view(t);
    }

    @Transactional(readOnly = true)
    public List<VistaNotificacion> history() {
        return notifications.history().stream().map(this::view).toList();
    }

    @Transactional
    public VistaNotificacion resend(Long id, String reason, HttpServletRequest req) {
        return view(notifications.resend(id, req, reason));
    }

    @Transactional
    public void cancel(Long id, String reason, HttpServletRequest req) {
        notifications.cancel(id, reason, req);
    }

    private void validateContent(String text) {
        if (text == null) return;
        String low = text.toLowerCase(Locale.ROOT);
        if (low.contains("<script") || low.contains("javascript:") || low.contains("<iframe") || low.contains("<object") || low.contains("<embed"))
            throw new IllegalArgumentException("La plantilla contiene contenido inseguro.");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{\\{[^}]+}}", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
        while (m.find()) if (!ALLOWED.contains(m.group()))
            throw new IllegalArgumentException("La variable " + m.group() + " no está disponible para este evento.");
    }

    private VistaPlantilla view(PlantillaNotificacion t) {
        return new VistaPlantilla(t.getId(), t.getEvent(), t.getChannel(), t.getName(), t.getSubject(), t.getContent(), t.isActive(), t.getUpdatedAt());
    }

    private VistaNotificacion view(Notificacion n) {
        return new VistaNotificacion(n.getId(), n.getComplaintCase() == null ? null : n.getComplaintCase().getCode(), n.getEvent(), n.getChannel(), n.getStatus(), mask(n.getRecipient()), n.getSubject(), n.getAttempts(), n.getLastError(), n.getCreatedAt(), n.getSentAt());
    }

    private String mask(String r) {
        if (r == null) return null;
        if (r.contains("@")) {
            int at = r.indexOf('@');
            return r.substring(0, Math.min(1, at)) + "***" + r.substring(at);
        }
        return r;
    }

    private String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
