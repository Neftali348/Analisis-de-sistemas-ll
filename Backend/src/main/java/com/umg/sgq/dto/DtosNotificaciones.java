package com.umg.sgq.dto;

import com.umg.sgq.enumeracion.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public final class DtosNotificaciones {
    private DtosNotificaciones() {
    }

    public record SolicitudPlantilla(@NotBlank @Size(max = 120) String name, @Size(max = 250) String subject,
                                  @NotBlank @Size(max = 4000) String content, boolean active) {
    }

    public record VistaPlantilla(Long id, EventoNotificacion event, CanalNotificacion channel, String name,
                               String subject, String content, boolean active, LocalDateTime updatedAt) {
    }

    public record VistaNotificacion(Long id, String caseCode, EventoNotificacion event, CanalNotificacion channel,
                                   EstadoNotificacion status, String recipient, String subject, int attempts,
                                   String lastError, LocalDateTime createdAt, LocalDateTime sentAt) {
    }

    public record SolicitudReenvio(String reason) {
    }

    public record SolicitudCancelacion(@NotBlank @Size(max = 1000) String reason) {
    }
}
