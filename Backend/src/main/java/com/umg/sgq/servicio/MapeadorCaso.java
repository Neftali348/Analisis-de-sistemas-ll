package com.umg.sgq.servicio;

import com.umg.sgq.dto.DtosCasos.*;
import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.EstadoEvidencia;
import com.umg.sgq.repositorio.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MapeadorCaso {
    private final RepositorioSeguimiento followUps;
    private final RepositorioEvidencia evidences;

    public MapeadorCaso(RepositorioSeguimiento followUps, RepositorioEvidencia evidences) {
        this.followUps = followUps;
        this.evidences = evidences;
    }

    public VistaSeguimiento follow(Seguimiento f) {
        return new VistaSeguimiento(f.getId(), f.getType(), f.getDescription(), f.isVisibleToClient(), f.getResultingStatus(), f.getCreatedAt(), f.getAuthor() != null ? f.getAuthor().getFullName() : f.getAuthorLabel());
    }

    public VistaEvidencia evidence(Evidencia e) {
        return new VistaEvidencia(e.getId(), e.getOriginalName(), e.getContentType(), e.getSizeBytes(), e.getStatus().name(), e.getDescription(), e.isVisibleToClient(), e.getUploadedAt());
    }

    public VistaCasoPublico publicView(Caso c) {
        var fs = followUps.findByComplaintCaseIdAndVisibleToClientTrueOrderByCreatedAtAsc(c.getId()).stream().map(this::follow).toList();
        var es = evidences.findByComplaintCaseIdOrderByUploadedAtAsc(c.getId()).stream().filter(e -> e.getStatus() == EstadoEvidencia.ACTIVA && e.isVisibleToClient()).map(this::evidence).toList();
        return new VistaCasoPublico(c.getCode(), c.getType(), c.getStatus(), c.getPriority(), c.getBranch().getName(), c.isConfidential() ? "Información restringida" : c.getDescription(), c.getIncidentAt(), c.getCreatedAt(), c.getResolution(), c.getClosedAt(), fs, es);
    }

    public VistaCasoInterno internalView(Caso c, boolean revealSensitive) {
        String name = revealSensitive ? c.getFullName() : mask(c.getFullName());
        String email = revealSensitive ? c.getEmail() : maskEmail(c.getEmail());
        String phone = revealSensitive ? c.getPhone() : maskPhone(c.getPhone());
        var fs = followUps.findByComplaintCaseIdOrderByCreatedAtAsc(c.getId()).stream().map(this::follow).toList();
        var es = evidences.findByComplaintCaseIdOrderByUploadedAtAsc(c.getId()).stream().map(this::evidence).toList();
        return new VistaCasoInterno(c.getId(), c.getCode(), c.getType(), c.getStatus(), c.getPriority(), c.getCategory(), c.isAnonymous(), c.isConfidential(), name, email, phone, c.getBranch().getName(), c.getBranch().getId(), c.getOrderNumber(), c.getAdministrativeObservation(), c.getIncidentAt(), c.getDescription(), c.isContactAuthorized(), c.getResponsible() == null ? null : c.getResponsible().getId(), c.getResponsible() == null ? null : c.getResponsible().getFullName(), c.getResolution(), c.getResolutionAt(), c.getCloseReason(), c.getCloseComment(), c.getClosedAt(), c.getCreatedAt(), c.getUpdatedAt(), c.getFirstResponseAt(), c.getSlaDeadlineAt(), c.isSlaWarningSent(), c.isSlaBreached(), fs, es, c.getVersion());
    }

    private String mask(String s) {
        if (s == null || s.isBlank()) return null;
        return s.charAt(0) + "***";
    }

    private String maskEmail(String s) {
        if (s == null || !s.contains("@")) return null;
        return s.charAt(0) + "***@" + s.substring(s.indexOf('@') + 1);
    }

    private String maskPhone(String s) {
        if (s == null || s.length() < 4) return null;
        return "****" + s.substring(s.length() - 4);
    }
}
