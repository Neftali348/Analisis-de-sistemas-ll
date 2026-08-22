package com.umg.sgq.servicio;

import com.umg.sgq.dto.DtosCasos.*;
import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.EstadoCaso;
import com.umg.sgq.enumeracion.EstadoEvidencia;
import com.umg.sgq.repositorio.RepositorioEvidencia;
import com.umg.sgq.repositorio.RepositorioSeguimiento;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MapeadorCaso {

    private final RepositorioSeguimiento followUps;
    private final RepositorioEvidencia evidences;

    public MapeadorCaso(
            RepositorioSeguimiento followUps,
            RepositorioEvidencia evidences
    ) {
        this.followUps = followUps;
        this.evidences = evidences;
    }

    // =========================================================
    // SEGUIMIENTO INTERNO
    // =========================================================

    public VistaSeguimiento follow(
            Seguimiento f
    ) {

        String autor =
                f.getAuthor() != null
                        ? f.getAuthor().getFullName()
                        : f.getAuthorLabel();

        return new VistaSeguimiento(
                f.getId(),
                f.getType(),
                f.getDescription(),
                f.isVisibleToClient(),
                f.getResultingStatus(),
                f.getCreatedAt(),
                autor
        );
    }

    // =========================================================
    // SEGUIMIENTO PARA PORTAL PÚBLICO
    // CU-03 / RN21
    // =========================================================

    private VistaSeguimiento followPublic(
            Seguimiento f,
            boolean confidential
    ) {

        String autor;

        /*
         * En una denuncia confidencial no debemos revelar
         * el nombre del usuario interno.
         */
        if (confidential) {

            if (
                    f.getAuthor() == null
                            && "CLIENTE".equalsIgnoreCase(
                            f.getAuthorLabel()
                    )
            ) {

                autor = "CLIENTE";

            } else {

                autor = "ATENCIÓN AL CLIENTE";
            }

        } else {

            autor =
                    f.getAuthor() != null
                            ? f.getAuthor().getFullName()
                            : f.getAuthorLabel();
        }

        return new VistaSeguimiento(
                f.getId(),
                f.getType(),
                f.getDescription(),
                true,
                f.getResultingStatus(),
                f.getCreatedAt(),
                autor
        );
    }

    // =========================================================
    // EVIDENCIA
    // =========================================================

    public VistaEvidencia evidence(
            Evidencia e
    ) {

        return new VistaEvidencia(
                e.getId(),
                e.getOriginalName(),
                e.getContentType(),
                e.getSizeBytes(),
                e.getStatus().name(),
                e.getDescription(),
                e.isVisibleToClient(),
                e.getUploadedAt()
        );
    }

    // =========================================================
    // CU-03 - VISTA PÚBLICA DEL CASO
    // =========================================================

    public VistaCasoPublico publicView(
            Caso c
    ) {

        boolean confidential =
                c.isConfidential();

        // =====================================================
        // SOLO SEGUIMIENTOS VISIBLES AL CLIENTE
        // =====================================================

        List<VistaSeguimiento> seguimientosPublicos =
                followUps
                        .findByComplaintCaseIdAndVisibleToClientTrueOrderByCreatedAtAsc(
                                c.getId()
                        )
                        .stream()
                        .map(
                                seguimiento ->
                                        followPublic(
                                                seguimiento,
                                                confidential
                                        )
                        )
                        .toList();

        // =====================================================
        // SOLO EVIDENCIAS:
        // - ACTIVAS
        // - VISIBLES AL CLIENTE
        // =====================================================

        List<VistaEvidencia> evidenciasPublicas =
                evidences
                        .findByComplaintCaseIdOrderByUploadedAtAsc(
                                c.getId()
                        )
                        .stream()
                        .filter(
                                evidencia ->
                                        evidencia.getStatus()
                                                == EstadoEvidencia.ACTIVA
                        )
                        .filter(
                                Evidencia::isVisibleToClient
                        )
                        .map(
                                this::evidence
                        )
                        .toList();

        // =====================================================
        // CU-03 FA13 / FA14
        // REAPERTURA DENTRO DE 15 DÍAS
        // =====================================================

        boolean puedeSolicitarReapertura =
                puedeSolicitarReapertura(c);

        boolean reaperturaSolicitada =
                c.getReopenRequestedAt() != null;

        // =====================================================
        // CU-03 FA06
        // DENUNCIA CONFIDENCIAL
        // =====================================================

        if (confidential) {

            /*
             * CU-03 indica que para una denuncia confidencial
             * solamente deben mostrarse:
             *
             * - código
             * - estado
             * - fechas
             * - seguimientos autorizados
             * - mensajes dirigidos al cliente
             *
             * No mostramos:
             *
             * - tipo
             * - prioridad
             * - categoría
             * - sucursal
             * - descripción original
             * - datos internos
             * - usuarios internos
             * - observaciones privadas
             */

            return new VistaCasoPublico(

                    // Código
                    c.getCode(),

                    // Tipo restringido
                    null,

                    // Estado
                    c.getStatus(),

                    // Prioridad restringida
                    null,

                    // Categoría restringida
                    null,

                    // Sucursal restringida
                    null,

                    // Descripción restringida
                    "Información restringida por confidencialidad.",

                    // Fecha del incidente
                    c.getIncidentAt(),

                    // Fecha de creación
                    c.getCreatedAt(),

                    // Última actualización
                    c.getUpdatedAt(),

                    // Resolución restringida
                    null,

                    // Fecha de resolución
                    c.getResolutionAt(),

                    // Motivo de cierre restringido
                    null,

                    // Comentario de cierre restringido
                    null,

                    // Fecha de cierre
                    c.getClosedAt(),

                    // Confidencial
                    true,

                    // Puede solicitar reapertura
                    puedeSolicitarReapertura,

                    // Ya existe solicitud
                    reaperturaSolicitada,

                    // Seguimientos públicos
                    seguimientosPublicos,

                    // Evidencias autorizadas
                    evidenciasPublicas
            );
        }

        // =====================================================
        // CASO NORMAL NO CONFIDENCIAL
        // =====================================================

        return new VistaCasoPublico(

                // Código
                c.getCode(),

                // Tipo
                c.getType(),

                // Estado
                c.getStatus(),

                // Prioridad
                c.getPriority(),

                // Categoría
                c.getCategory(),

                // Sucursal
                c.getBranch() == null
                        ? null
                        : c.getBranch().getName(),

                // Descripción
                c.getDescription(),

                // Fecha incidente
                c.getIncidentAt(),

                // Fecha registro
                c.getCreatedAt(),

                // Última actualización
                c.getUpdatedAt(),

                // Resolución
                c.getResolution(),

                // Fecha resolución
                c.getResolutionAt(),

                // Motivo cierre
                c.getCloseReason(),

                // Comentario cierre
                c.getCloseComment(),

                // Fecha cierre
                c.getClosedAt(),

                // Confidencial
                false,

                // Puede solicitar reapertura
                puedeSolicitarReapertura,

                // Reapertura ya solicitada
                reaperturaSolicitada,

                // Seguimientos
                seguimientosPublicos,

                // Evidencias
                evidenciasPublicas
        );
    }

    // =========================================================
    // VALIDAR PLAZO DE REAPERTURA
    // CU-03 FA13 / FA14
    // =========================================================

    private boolean puedeSolicitarReapertura(
            Caso c
    ) {

        if (
                c.getStatus()
                        != EstadoCaso.CERRADO
        ) {
            return false;
        }

        if (
                c.getClosedAt()
                        == null
        ) {
            return false;
        }

        /*
         * Si ya existe una solicitud,
         * no mostramos nuevamente la opción.
         */
        if (
                c.getReopenRequestedAt()
                        != null
        ) {
            return false;
        }

        LocalDateTime limite =
                c.getClosedAt()
                        .plusDays(15);

        /*
         * Disponible mientras todavía no haya
         * pasado la fecha límite.
         */
        return !limite.isBefore(
                LocalDateTime.now()
        );
    }

    // =========================================================
    // VISTA INTERNA
    // =========================================================

    public VistaCasoInterno internalView(
            Caso c,
            boolean revealSensitive
    ) {

        String name =
                revealSensitive
                        ? c.getFullName()
                        : mask(
                        c.getFullName()
                );

        String email =
                revealSensitive
                        ? c.getEmail()
                        : maskEmail(
                        c.getEmail()
                );

        String phone =
                revealSensitive
                        ? c.getPhone()
                        : maskPhone(
                        c.getPhone()
                );

        List<VistaSeguimiento> seguimientos =
                followUps
                        .findByComplaintCaseIdOrderByCreatedAtAsc(
                                c.getId()
                        )
                        .stream()
                        .map(
                                this::follow
                        )
                        .toList();

        List<VistaEvidencia> evidencias =
                evidences
                        .findByComplaintCaseIdOrderByUploadedAtAsc(
                                c.getId()
                        )
                        .stream()
                        .map(
                                this::evidence
                        )
                        .toList();

        return new VistaCasoInterno(

                c.getId(),

                c.getCode(),

                c.getType(),

                c.getStatus(),

                c.getPriority(),

                c.getCategory(),

                c.isAnonymous(),

                c.isConfidential(),

                name,

                email,

                phone,

                c.getBranch() == null
                        ? null
                        : c.getBranch().getName(),

                c.getBranch() == null
                        ? null
                        : c.getBranch().getId(),

                c.getOrderNumber(),

                c.getAdministrativeObservation(),

                c.getIncidentAt(),

                c.getDescription(),

                c.isContactAuthorized(),

                c.getResponsible() == null
                        ? null
                        : c.getResponsible().getId(),

                c.getResponsible() == null
                        ? null
                        : c.getResponsible().getFullName(),

                c.getResolution(),

                c.getResolutionAt(),

                c.getCloseReason(),

                c.getCloseComment(),

                c.getClosedAt(),

                c.getCreatedAt(),

                c.getUpdatedAt(),

                c.getFirstResponseAt(),

                c.getSlaDeadlineAt(),

                c.isSlaWarningSent(),

                c.isSlaBreached(),

                seguimientos,

                evidencias,

                c.getVersion()
        );
    }

    // =========================================================
    // OCULTAR NOMBRE
    // =========================================================

    private String mask(
            String s
    ) {

        if (
                s == null
                        || s.isBlank()
        ) {
            return null;
        }

        return s.charAt(0)
                + "***";
    }

    // =========================================================
    // OCULTAR CORREO
    // =========================================================

    private String maskEmail(
            String s
    ) {

        if (
                s == null
                        || !s.contains("@")
        ) {
            return null;
        }

        return s.charAt(0)
                + "***@"
                + s.substring(
                s.indexOf('@') + 1
        );
    }

    // =========================================================
    // OCULTAR TELÉFONO
    // =========================================================

    private String maskPhone(
            String s
    ) {

        if (
                s == null
                        || s.length() < 4
        ) {
            return null;
        }

        return "****"
                + s.substring(
                s.length() - 4
        );
    }
}