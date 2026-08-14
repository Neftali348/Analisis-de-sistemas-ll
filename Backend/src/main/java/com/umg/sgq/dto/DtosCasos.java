package com.umg.sgq.dto;

import com.umg.sgq.enumeracion.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public final class DtosCasos {
    private DtosCasos() {
    }

    public record SolicitudCreacionPublica(
            @NotNull TipoCaso type, boolean anonymous, boolean confidential,
            String fullName, @Email String email,
            @Pattern(regexp = "^$|\\d{8,15}$", message = "El teléfono debe contener entre 8 y 15 dígitos") String phone,
            @NotNull Long branchId, @Size(max = 30) String orderNumber,
            @NotNull @PastOrPresent(message = "La fecha del incidente no puede ser posterior a la fecha actual") LocalDateTime incidentAt,
            @NotNull CategoriaCaso category,
            @NotBlank @Size(min = 20, max = 3000, message = "La descripción debe contener entre 20 y 3000 caracteres") String description,
            boolean contactAuthorized,
            @AssertTrue(message = "Debe aceptar el aviso de privacidad") boolean privacyAccepted) {
    }

    public record RespuestaCreacionPublica(String code, String trackingKey, String status, String message) {
    }

    public record SolicitudConsultaPublica(@NotBlank String code, String email, String trackingKey) {
    }

    public record VistaSeguimiento(Long id, TipoSeguimiento type, String description, boolean visibleToClient,
                               EstadoCaso resultingStatus, LocalDateTime createdAt, String author) {
    }

    public record VistaEvidencia(Long id, String originalName, String contentType, long sizeBytes, String status,
                               String description, boolean visibleToClient, LocalDateTime uploadedAt) {
    }

    public record SolicitudRespuestaPublica(@NotBlank @Size(min = 10, max = 3000) String response) {
    }

    public record SolicitudMotivoPublico(@NotBlank @Size(max = 1000) String reason) {
    }

    public record VistaCasoPublico(String code, TipoCaso type, EstadoCaso status, Prioridad priority, String branch,
                                 String description, LocalDateTime incidentAt, LocalDateTime createdAt,
                                 String resolution, LocalDateTime closedAt, List<VistaSeguimiento> followUps,
                                 List<VistaEvidencia> evidences) {
    }

    public record VistaCasoInterno(Long id, String code, TipoCaso type, EstadoCaso status, Prioridad priority,
                                   CategoriaCaso category, boolean anonymous, boolean confidential, String fullName,
                                   String email, String phone, String branch, Long branchId, String orderNumber,
                                   String administrativeObservation, LocalDateTime incidentAt, String description,
                                   boolean contactAuthorized, Long responsibleId, String responsible, String resolution,
                                   LocalDateTime resolutionAt, MotivoCierre closeReason, String closeComment,
                                   LocalDateTime closedAt, LocalDateTime createdAt, LocalDateTime updatedAt,
                                   LocalDateTime firstResponseAt, LocalDateTime slaDeadlineAt, boolean slaWarningSent,
                                   boolean slaBreached, List<VistaSeguimiento> followUps, List<VistaEvidencia> evidences,
                                   long version) {
    }

    public record PaginaCasos(List<VistaCasoInterno> content, int page, int size, long totalElements, int totalPages) {
    }

    public record SolicitudEdicionCaso(@NotNull TipoCaso type, @NotNull Long branchId, @NotNull CategoriaCaso category,
                                  @NotNull Prioridad priority, @Size(max = 30) String orderNumber,
                                  @Size(max = 1000) String administrativeObservation) {
    }

    public record SolicitudAsignacion(@NotNull Long responsibleId, String reason) {
    }

    public record SolicitudSeguimiento(@NotNull TipoSeguimiento type, @NotBlank @Size(min = 10, max = 3000) String description,
                                  boolean visibleToClient, EstadoCaso newStatus) {
    }

    public record SolicitudResolucion(@NotBlank @Size(min = 10, max = 3000) String resolution) {
    }

    public record SolicitudCierre(@NotNull MotivoCierre reason, @NotBlank @Size(min = 10, max = 1000) String summary,
                               @Size(max = 1000) String internalObservation, @Size(max = 1000) String detailReason,
                               String duplicateCaseCode, boolean notifyClient, boolean sendSurvey,
                               boolean criticalReviewConfirmed) {
    }

    public record SolicitudReapertura(@NotBlank @Size(max = 1000) String reason, boolean specialJustification) {
    }

    public record SolicitudPrioridad(@NotNull Prioridad priority) {
    }

    public record SolicitudEstado(@NotNull EstadoCaso status, @NotBlank @Size(max = 1000) String reason) {
    }

    public record SolicitudSatisfaccion(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {
    }
}
