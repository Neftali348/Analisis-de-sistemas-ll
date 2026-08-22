package com.umg.sgq.dto;

import com.umg.sgq.enumeracion.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public final class DtosCasos {

    private DtosCasos() {
    }

    // =========================================================
    // CU-02 - REGISTRAR CASO
    // =========================================================

    public record SolicitudCreacionPublica(

            @NotNull
            TipoCaso type,

            boolean anonymous,

            boolean confidential,

            String fullName,

            @Email(
                    message = "El correo electrónico no tiene un formato válido"
            )
            String email,

            @Pattern(
                    regexp = "^$|\\d{8,15}$",
                    message = "El teléfono debe contener entre 8 y 15 dígitos"
            )
            String phone,

            @NotNull
            Long branchId,

            @Size(
                    max = 30,
                    message = "El número de pedido no puede superar los 30 caracteres"
            )
            String orderNumber,

            @NotNull
            @PastOrPresent(
                    message = "La fecha del incidente no puede ser posterior a la fecha actual"
            )
            LocalDateTime incidentAt,

            @NotNull
            CategoriaCaso category,

            @NotBlank
            @Size(
                    min = 20,
                    max = 3000,
                    message = "La descripción debe contener entre 20 y 3000 caracteres"
            )
            String description,

            boolean contactAuthorized,

            @AssertTrue(
                    message = "Debe aceptar el aviso de privacidad"
            )
            boolean privacyAccepted

    ) {
    }

    public record RespuestaCreacionPublica(

            String code,

            String trackingKey,

            String status,

            String message

    ) {
    }

    // =========================================================
    // CU-03 - CONSULTAR ESTADO DEL CASO
    // =========================================================

    public record SolicitudConsultaPublica(

            @NotBlank(
                    message = "Complete todos los campos obligatorios."
            )
            @Pattern(
                    regexp = "^(QUE|REC|DEN|SUG)-\\d{4}-\\d{6}$",
                    message = "El código de seguimiento no existe o los datos de consulta son incorrectos."
            )
            String code,

            @Email(
                    message = "El correo electrónico ingresado no es válido."
            )
            String email,

            String trackingKey,

            @NotBlank(
                    message = "Complete todos los campos obligatorios."
            )
            String securityChallengeId,

            @NotBlank(
                    message = "Complete todos los campos obligatorios."
            )
            String securityAnswer

    ) {
    }
    // =========================================================
    // SEGUIMIENTO
    // =========================================================

    public record VistaSeguimiento(

            Long id,

            TipoSeguimiento type,

            String description,

            boolean visibleToClient,

            EstadoCaso resultingStatus,

            LocalDateTime createdAt,

            String author

    ) {
    }

    // =========================================================
    // EVIDENCIA
    // =========================================================

    public record VistaEvidencia(

            Long id,

            String originalName,

            String contentType,

            long sizeBytes,

            String status,

            String description,

            boolean visibleToClient,

            LocalDateTime uploadedAt

    ) {
    }

    // =========================================================
    // CU-03 FA11
    // RESPUESTA DEL CLIENTE
    // =========================================================

    public record SolicitudRespuestaPublica(

            @NotBlank(
                    message = "La respuesta es obligatoria"
            )
            @Size(
                    min = 10,
                    max = 3000,
                    message = "La respuesta debe contener entre 10 y 3000 caracteres"
            )
            String response

    ) {
    }

    // =========================================================
    // CU-03 FA10 / FA13
    // CANCELACIÓN Y REAPERTURA
    // =========================================================

    public record SolicitudMotivoPublico(

            @NotBlank(
                    message = "Debe ingresar un motivo"
            )
            @Size(
                    max = 1000,
                    message = "El motivo no puede superar los 1000 caracteres"
            )
            String reason

    ) {
    }

    // =========================================================
    // CU-03 - VISTA PÚBLICA COMPLETA
    // =========================================================

    public record VistaCasoPublico(

            // Código único
            String code,

            // Tipo
            TipoCaso type,

            // Estado actual
            EstadoCaso status,

            // Prioridad
            Prioridad priority,

            // Categoría
            CategoriaCaso category,

            // Sucursal
            String branch,

            // Descripción registrada
            String description,

            // Fecha del incidente
            LocalDateTime incidentAt,

            // Fecha de registro
            LocalDateTime createdAt,

            // Última actualización
            LocalDateTime updatedAt,

            // Resolución
            String resolution,

            // Fecha de resolución
            LocalDateTime resolutionAt,

            // Motivo formal de cierre
            MotivoCierre closeReason,

            // Comentario / justificación final
            String closeComment,

            // Fecha de cierre
            LocalDateTime closedAt,

            // Indica si es una denuncia confidencial
            boolean confidential,

            // CU-03 FA13 / FA14
            // El backend decide si todavía puede pedir reapertura.
            boolean canRequestReopen,

            // Evita mostrar nuevamente el formulario
            // si ya existe solicitud de reapertura.
            boolean reopenRequested,

            // Seguimientos públicos
            List<VistaSeguimiento> followUps,

            // Evidencias públicas
            List<VistaEvidencia> evidences

    ) {
    }

    // =========================================================
    // VISTA INTERNA DEL CASO
    // =========================================================

    public record VistaCasoInterno(

            Long id,

            String code,

            TipoCaso type,

            EstadoCaso status,

            Prioridad priority,

            CategoriaCaso category,

            boolean anonymous,

            boolean confidential,

            String fullName,

            String email,

            String phone,

            String branch,

            Long branchId,

            String orderNumber,

            String administrativeObservation,

            LocalDateTime incidentAt,

            String description,

            boolean contactAuthorized,

            Long responsibleId,

            String responsible,

            String resolution,

            LocalDateTime resolutionAt,

            MotivoCierre closeReason,

            String closeComment,

            LocalDateTime closedAt,

            LocalDateTime createdAt,

            LocalDateTime updatedAt,

            LocalDateTime firstResponseAt,

            LocalDateTime slaDeadlineAt,

            boolean slaWarningSent,

            boolean slaBreached,

            List<VistaSeguimiento> followUps,

            List<VistaEvidencia> evidences,

            long version

    ) {
    }

    // =========================================================
    // PAGINACIÓN
    // =========================================================

    public record PaginaCasos(

            List<VistaCasoInterno> content,

            int page,

            int size,

            long totalElements,

            int totalPages

    ) {
    }

    // =========================================================
    // EDICIÓN INTERNA
    // =========================================================

    public record SolicitudEdicionCaso(

            @NotNull
            TipoCaso type,

            @NotNull
            Long branchId,

            @NotNull
            CategoriaCaso category,

            @NotNull
            Prioridad priority,

            @Size(
                    max = 30
            )
            String orderNumber,

            @Size(
                    max = 1000
            )
            String administrativeObservation

    ) {
    }

    // =========================================================
    // ASIGNACIÓN
    // =========================================================

    public record SolicitudAsignacion(

            @NotNull
            Long responsibleId,

            String reason

    ) {
    }

    // =========================================================
    // SEGUIMIENTO INTERNO
    // =========================================================

    public record SolicitudSeguimiento(

            @NotNull
            TipoSeguimiento type,

            @NotBlank
            @Size(
                    min = 10,
                    max = 3000
            )
            String description,

            boolean visibleToClient,

            EstadoCaso newStatus

    ) {
    }

    // =========================================================
    // RESOLUCIÓN
    // =========================================================

    public record SolicitudResolucion(

            @NotBlank
            @Size(
                    min = 10,
                    max = 3000
            )
            String resolution

    ) {
    }

    // =========================================================
    // CIERRE
    // =========================================================

    public record SolicitudCierre(

            @NotNull
            MotivoCierre reason,

            @NotBlank
            @Size(
                    min = 10,
                    max = 1000
            )
            String summary,

            @Size(
                    max = 1000
            )
            String internalObservation,

            @Size(
                    max = 1000
            )
            String detailReason,

            String duplicateCaseCode,

            boolean notifyClient,

            boolean sendSurvey,

            boolean criticalReviewConfirmed

    ) {
    }

    // =========================================================
    // REAPERTURA INTERNA
    // =========================================================

    public record SolicitudReapertura(

            @NotBlank
            @Size(
                    max = 1000
            )
            String reason,

            boolean specialJustification

    ) {
    }

    // =========================================================
    // PRIORIDAD
    // =========================================================

    public record SolicitudPrioridad(

            @NotNull
            Prioridad priority

    ) {
    }

    // =========================================================
    // CAMBIO DE ESTADO
    // =========================================================

    public record SolicitudEstado(

            @NotNull
            EstadoCaso status,

            @NotBlank
            @Size(
                    max = 1000
            )
            String reason

    ) {
    }

    // =========================================================
    // SATISFACCIÓN
    // =========================================================

    public record SolicitudSatisfaccion(

            @Min(
                    value = 1,
                    message = "La calificación mínima es 1"
            )
            @Max(
                    value = 5,
                    message = "La calificación máxima es 5"
            )
            int rating,

            @Size(
                    max = 1000
            )
            String comment

    ) {
    }
}