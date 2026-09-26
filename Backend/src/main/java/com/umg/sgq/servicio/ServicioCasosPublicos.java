package com.umg.sgq.servicio;
import com.umg.sgq.dto.DtosCasos.*;
import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.*;
import com.umg.sgq.repositorio.*;
import com.umg.sgq.utilidad.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.*;
@Service
public class ServicioCasosPublicos {
    private static final String MENSAJE_CONSULTA_INVALIDA =
            "El código de seguimiento no existe o los datos de consulta son incorrectos.";
    private static final String PATRON_CODIGO =
            "^(QUE|REC|DEN|SUG)-\\d{4}-\\d{6}$";
    private final RepositorioCaso cases;
    private final RepositorioSucursal branches;
    private final ServicioCodigoCaso codes;
    private final ServicioReglasCaso rules;
    private final ServicioEvidencias evidence;
    private final MapeadorCaso mapper;
    private final ServicioAuditoria audit;
    private final ServicioNotificaciones notifications;
    private final RepositorioSatisfaccion satisfaction;
    private final RepositorioSeguimiento followUps;
    private final RepositorioUsuario users;
    private final ServicioVerificacionPublica verificacionPublica;
    public ServicioCasosPublicos(
            RepositorioCaso cases,
            RepositorioSucursal branches,
            ServicioCodigoCaso codes,
            ServicioReglasCaso rules,
            ServicioEvidencias evidence,
            MapeadorCaso mapper,
            ServicioAuditoria audit,
            ServicioNotificaciones notifications,
            RepositorioSatisfaccion satisfaction,
            RepositorioSeguimiento followUps,
            RepositorioUsuario users,
            ServicioVerificacionPublica verificacionPublica
    ) {
        this.cases = cases;
        this.branches = branches;
        this.codes = codes;
        this.rules = rules;
        this.evidence = evidence;
        this.mapper = mapper;
        this.audit = audit;
        this.notifications = notifications;
        this.satisfaction = satisfaction;
        this.followUps = followUps;
        this.users = users;
        this.verificacionPublica = verificacionPublica;
    }
    // =========================================================
    // REGISTRAR CASO PÚBLICO
    // CU-02
    // =========================================================
    @Transactional
    public RespuestaCreacionPublica register(
            SolicitudCreacionPublica r,
            List<MultipartFile> files,
            HttpServletRequest req
    ) {
        validateIdentity(r);
        Sucursal branch = branches
                .findById(r.branchId())
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "La sucursal seleccionada no existe."
                        )
                );
        if (branch.getStatus() != EstadoRegistro.ACTIVO) {
            throw new IllegalArgumentException(
                    "La sucursal seleccionada se encuentra inactiva."
            );
        }
        if (files != null && files.size() > 5) {
            throw new IllegalArgumentException(
                    "Se alcanzó la cantidad máxima de archivos permitidos."
            );
        }
        String key =
                UtilidadClaveAleatoria.trackingKey();
        Caso c =
                new Caso();
        c.setCode(
                codes.next(r.type())
        );
        c.setType(
                r.type()
        );
        c.setPriority(
                initialPriority(
                        r.type(),
                        r.category()
                )
        );
        c.setCategory(
                r.category()
        );
        c.setStatus(
                EstadoCaso.REGISTRADO
        );
        c.setAnonymous(
                r.anonymous()
        );
        c.setConfidential(
                r.confidential()
        );
        c.setFullName(
                r.anonymous()
                        ? null
                        : clean(r.fullName())
        );
        c.setEmail(
                r.anonymous()
                        ? null
                        : blankToNull(r.email())
        );
        c.setPhone(
                r.anonymous()
                        ? null
                        : blankToNull(r.phone())
        );
        c.setTrackingKeyHash(
                UtilidadHash.sha256(key)
        );
        c.setBranch(
                branch
        );
        c.setOrderNumber(
                blankToNull(
                        r.orderNumber()
                )
        );
        c.setIncidentAt(
                r.incidentAt()
        );
        c.setDescription(
                r.description().trim()
        );
        c.setContactAuthorized(
                !r.anonymous()
                        && r.contactAuthorized()
        );
        c =
                cases.saveAndFlush(c);
        rules.initializeSla(c);
        EstadoCaso old =
                c.getStatus();
        rules.requireTransition(
                old,
                EstadoCaso.PENDIENTE_ASIGNACION
        );
        c.setStatus(
                EstadoCaso.PENDIENTE_ASIGNACION
        );
        cases.saveAndFlush(c);
        audit.log(
                req,
                "CASOS",
                "CREACION",
                "CASO",
                c.getCode(),
                "Caso registrado y movido automáticamente a Pendiente de Asignación",
                ResultadoAuditoria.EXITOSO,
                Map.of(
                        "estado",
                        old.name()
                ),
                Map.of(
                        "estado",
                        c.getStatus().name(),
                        "tipo",
                        c.getType().name(),
                        "prioridad",
                        c.getPriority().name()
                )
        );
        if (
                files != null
                        && !files.isEmpty()
        ) {
            evidence.store(
                    c,
                    null,
                    files,
                    req
            );
        }
        // =====================================================
        // CONFIRMACIÓN POR CORREO
        // =====================================================
        if (
                c.getEmail() != null
                        && !c.getEmail().isBlank()
        ) {
            String mensaje = """
                    Hola,
                    Su caso fue registrado correctamente en el Sistema de Gestión de Quejas.
                    Código de seguimiento:
                    %s
                    Estado actual:
                    Pendiente de Asignación
                    Puede utilizar este código junto con su correo electrónico
                    para consultar el estado de su caso.
                    Conserve este mensaje para futuras consultas.
                    Atentamente,
                    Sistema de Gestión de Quejas
                    Este es un mensaje automático. Por favor, no responda a este correo.
                    """.formatted(
                    c.getCode()
            );
            notifications.email(
                    EventoNotificacion.REGISTRO_CASO,
                    c,
                    c.getEmail(),
                    mensaje
            );
        }
        return new RespuestaCreacionPublica(
                c.getCode(),
                r.anonymous()
                        ? key
                        : null,
                c.getStatus().name(),
                "El caso se registró con éxito. Código de seguimiento: "
                        + c.getCode()
                        + "."
        );
    }
    // =========================================================
    // CU-03 - CONSULTAR ESTADO DEL CASO
    // =========================================================
    /*
     * noRollbackFor es importante porque si la consulta falla
     * necesitamos conservar el registro de auditoría del intento
     * fallido.
     */
    @Transactional(
            noRollbackFor = IllegalArgumentException.class
    )
    public VistaCasoPublico lookup(
            SolicitudConsultaPublica r,
            HttpServletRequest req
    ) {
        String codigo =
                normalizarCodigo(
                        r.code()
                );
        // =========================================================
        // CU-03 - VERIFICACIÓN DE SEGURIDAD
        // =========================================================
        boolean seguridadValida =
                verificacionPublica.validar(
                        r.securityChallengeId(),
                        r.securityAnswer()
                );
        if (!seguridadValida) {
            audit.logConsultaPublicaFallida(
                    req,
                    codigo
            );
            throw new IllegalArgumentException(
                    "La verificación de seguridad no es válida o ha expirado. Genere una nueva verificación e intente nuevamente."
            );
        }
        Caso c;
        try {
            // =====================================================
            // FA04 - VALIDAR FORMATO DEL CÓDIGO
            // =====================================================
            validarFormatoCodigo(
                    codigo
            );
            // =====================================================
            // FA01 / FA02 / FA05
            // VALIDAR CORREO O CLAVE TEMPORAL
            // =====================================================
            c = verify(
                    codigo,
                    r.email(),
                    r.trackingKey()
            );
        } catch (IllegalArgumentException ex) {
            // =====================================================
            // FA05 - CONSULTA FALLIDA
            // No almacenamos correo ni clave temporal.
            // =====================================================
            audit.logConsultaPublicaFallida(
                    req,
                    codigo
            );
            throw new IllegalArgumentException(
                    MENSAJE_CONSULTA_INVALIDA
            );
        }
        // =========================================================
        // CONSULTA EXITOSA
        // =========================================================
        audit.log(
                req,
                "CASOS",
                "CONSULTA_PUBLICA",
                "CASO",
                c.getCode(),
                "Consulta pública de estado del caso",
                ResultadoAuditoria.EXITOSO,
                null,
                null
        );
        return mapper.publicView(
                c
        );
    }
    // =========================================================
    // AGREGAR EVIDENCIA DESDE EL PORTAL PÚBLICO
    // =========================================================
    @Transactional
    public VistaEvidencia addEvidence(
            String code,
            String email,
            String trackingKey,
            MultipartFile file,
            HttpServletRequest req
    ) {
        Caso c =
                verify(
                        code,
                        email,
                        trackingKey
                );
        // CU-03 FA09
        // No permitir nuevas evidencias en estados finales.
        if (
                Set.of(
                        EstadoCaso.RESUELTO,
                        EstadoCaso.CERRADO,
                        EstadoCaso.RECHAZADO,
                        EstadoCaso.CANCELADO
                ).contains(
                        c.getStatus()
                )
        ) {
            throw new IllegalArgumentException(
                    "El estado actual del caso no permite adjuntar nuevas evidencias."
            );
        }
        Evidencia e =
                evidence.store(
                        c,
                        null,
                        List.of(file),
                        req
                ).get(0);
        return mapper.evidence(e);
    }
    // =========================================================
    // CU-03 FA11
    // RESPONDER SOLICITUD DE INFORMACIÓN
    // =========================================================
    @Transactional
    public VistaCasoPublico respond(
            String code,
            String email,
            String trackingKey,
            SolicitudRespuestaPublica r,
            List<MultipartFile> files,
            HttpServletRequest req
    ) {
        Caso c =
                verify(
                        code,
                        email,
                        trackingKey
                );
        // Únicamente cuando el sistema espera al cliente.
        if (
                c.getStatus()
                        != EstadoCaso.EN_ESPERA_CLIENTE
        ) {
            throw new IllegalArgumentException(
                    "El caso se encuentra en estado "
                            + c.getStatus()
                            + " y no permite esta operación."
            );
        }
        if (
                r.response() == null
                        || r.response().trim().length() < 10
        ) {
            throw new IllegalArgumentException(
                    "La respuesta debe contener al menos 10 caracteres."
            );
        }
        if (
                files != null
                        && files.size() > 5
        ) {
            throw new IllegalArgumentException(
                    "Puede adjuntar un máximo de cinco archivos."
            );
        }
        EstadoCaso old =
                c.getStatus();
        // EN_ESPERA_CLIENTE -> EN_PROCESO
        rules.requireTransition(
                old,
                EstadoCaso.EN_PROCESO
        );
        rules.applyTransitionSla(
                c,
                old,
                EstadoCaso.EN_PROCESO
        );
        c.setStatus(
                EstadoCaso.EN_PROCESO
        );
        cases.save(c);
        // =====================================================
        // SEGUIMIENTO VISIBLE
        // =====================================================
        Seguimiento f =
                new Seguimiento();
        f.setComplaintCase(
                c
        );
        // La respuesta fue realizada por cliente público.
        f.setAuthor(null);
        f.setAuthorLabel(
                "CLIENTE"
        );
        f.setType(
                TipoSeguimiento.RESPUESTA_CLIENTE
        );
        f.setDescription(
                r.response().trim()
        );
        f.setVisibleToClient(
                true
        );
        f.setResultingStatus(
                EstadoCaso.EN_PROCESO
        );
        f =
                followUps.saveAndFlush(f);
        // =====================================================
        // EVIDENCIAS DE LA RESPUESTA
        // =====================================================
        if (
                files != null
                        && !files.isEmpty()
        ) {
            evidence.store(
                    c,
                    f,
                    files,
                    true,
                    "Evidencia enviada por el cliente",
                    req
            );
        }
        // =====================================================
        // AUDITORÍA
        // =====================================================
        audit.log(
                req,
                "SEGUIMIENTOS",
                "RESPUESTA_CLIENTE",
                "CASO",
                c.getCode(),
                "El cliente respondió una solicitud de información",
                ResultadoAuditoria.EXITOSO,
                Map.of(
                        "estado",
                        old.name()
                ),
                Map.of(
                        "estado",
                        c.getStatus().name()
                )
        );
        // =====================================================
        // NOTIFICAR AL RESPONSABLE
        // =====================================================
        if (
                c.getResponsible()
                        != null
        ) {
            notifications.internal(
                    EventoNotificacion.SEGUIMIENTO_VISIBLE,
                    c,
                    c.getResponsible(),
                    "El cliente respondió el caso "
                            + c.getCode()
            );
        }
        return mapper.publicView(c);
    }
    // =========================================================
    // CU-03 FA10
    // CANCELACIÓN POR EL CLIENTE
    // =========================================================
    @Transactional
    public VistaCasoPublico cancel(
            String code,
            String email,
            String trackingKey,
            SolicitudMotivoPublico r,
            HttpServletRequest req
    ) {
        Caso c =
                verify(
                        code,
                        email,
                        trackingKey
                );
        // =====================================================
        // ESTADOS PERMITIDOS POR CU-03
        // =====================================================
        if (
                !Set.of(
                        EstadoCaso.REGISTRADO,
                        EstadoCaso.PENDIENTE_ASIGNACION,
                        EstadoCaso.ASIGNADO,
                        EstadoCaso.EN_PROCESO
                ).contains(
                        c.getStatus()
                )
        ) {
            throw new IllegalArgumentException(
                    "El caso se encuentra en estado "
                            + c.getStatus()
                            + " y no permite esta operación."
            );
        }
        if (
                r.reason() == null
                        || r.reason().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Debe indicar el motivo de cancelación."
            );
        }
        EstadoCaso old =
                c.getStatus();
        rules.requireTransition(
                old,
                EstadoCaso.CANCELADO
        );
        rules.applyTransitionSla(
                c,
                old,
                EstadoCaso.CANCELADO
        );
        c.setStatus(
                EstadoCaso.CANCELADO
        );
        /*
         * Se conserva el motivo para poder mostrarlo
         * posteriormente en la consulta pública.
         */
        c.setCloseComment(
                "Cancelación solicitada por cliente: "
                        + r.reason().trim()
        );
        cases.save(c);
        // =====================================================
        // AUDITORÍA
        // =====================================================
        audit.log(
                req,
                "CASOS",
                "CANCELACION_CLIENTE",
                "CASO",
                c.getCode(),
                "Caso cancelado por solicitud del cliente. Motivo: "
                        + r.reason().trim(),
                ResultadoAuditoria.EXITOSO,
                Map.of(
                        "estado",
                        old.name()
                ),
                Map.of(
                        "estado",
                        c.getStatus().name()
                )
        );
        // =====================================================
        // NOTIFICAR RESPONSABLE
        // =====================================================
        if (
                c.getResponsible()
                        != null
        ) {
            notifications.internal(
                    EventoNotificacion.CIERRE,
                    c,
                    c.getResponsible(),
                    "El cliente canceló el caso "
                            + c.getCode()
            );
        }
        return mapper.publicView(c);
    }
    // =========================================================
    // CU-03 FA13 / FA14
    // SOLICITAR REAPERTURA
    // =========================================================
    @Transactional
    public void requestReopen(
            String code,
            String email,
            String trackingKey,
            SolicitudMotivoPublico r,
            HttpServletRequest req
    ) {
        Caso c =
                verify(
                        code,
                        email,
                        trackingKey
                );
        if (
                c.getStatus()
                        != EstadoCaso.CERRADO
        ) {
            throw new IllegalArgumentException(
                    "Solo se puede solicitar reapertura de un caso cerrado."
            );
        }
        if (
                c.getClosedAt()
                        == null
        ) {
            throw new IllegalArgumentException(
                    "No fue posible determinar la fecha de cierre del caso."
            );
        }
        // =====================================================
        // PLAZO ORDINARIO DE 15 DÍAS
        // =====================================================
        if (
                c.getClosedAt()
                        .plusDays(15)
                        .isBefore(
                                LocalDateTime.now()
                        )
        ) {
            throw new IllegalArgumentException(
                    "El plazo ordinario para reabrir el caso ha vencido. Puede registrar un nuevo caso relacionado."
            );
        }
        if (
                r.reason() == null
                        || r.reason().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Debe indicar el motivo de reapertura."
            );
        }
        // Evitar solicitudes repetidas.
        if (
                c.getReopenRequestedAt()
                        != null
        ) {
            throw new IllegalArgumentException(
                    "Ya existe una solicitud de reapertura pendiente de revisión."
            );
        }
        c.setReopenRequestedAt(
                LocalDateTime.now()
        );
        c.setReopenRequestReason(
                r.reason().trim()
        );
        cases.save(c);
        // =====================================================
        // AUDITORÍA
        // =====================================================
        audit.log(
                req,
                "CASOS",
                "SOLICITUD_REAPERTURA",
                "CASO",
                c.getCode(),
                "Cliente solicitó reapertura. Motivo: "
                        + r.reason().trim(),
                ResultadoAuditoria.EXITOSO,
                null,
                Map.of(
                        "motivo",
                        r.reason().trim()
                )
        );
        // =====================================================
        // NOTIFICAR SUPERVISOR
        // =====================================================
        if (
                c.getBranch()
                        != null
                        && c.getBranch()
                        .getSupervisor()
                        != null
        ) {
            notifications.internal(
                    EventoNotificacion.REASIGNACION,
                    c,
                    c.getBranch()
                            .getSupervisor(),
                    "Solicitud de reapertura del caso "
                            + c.getCode()
            );
        }
        // =====================================================
        // NOTIFICAR ADMINISTRADORES
        // =====================================================
        for (
                Usuario administrador :
                users.findActiveByRoleAndBranch(
                        CodigoRol.ADMINISTRADOR,
                        EstadoRegistro.ACTIVO,
                        null
                )
        ) {
            notifications.internal(
                    EventoNotificacion.REASIGNACION,
                    c,
                    administrador,
                    "Solicitud de reapertura del caso "
                            + c.getCode()
            );
        }
    }
    // =========================================================
    // CU-03 FA12
    // DESCARGAR EVIDENCIA AUTORIZADA
    // =========================================================
    @Transactional
    public ServicioEvidencias.Download download(
            String code,
            Long evidenceId,
            String email,
            String trackingKey,
            HttpServletRequest req
    ) {
        Caso c =
                verify(
                        code,
                        email,
                        trackingKey
                );
        Evidencia e =
                evidence.require(
                        evidenceId
                );
        // La evidencia debe pertenecer al caso consultado.
        if (
                e.getComplaintCase()
                        == null
                        || !Objects.equals(
                        e.getComplaintCase()
                                .getId(),
                        c.getId()
                )
        ) {
            audit.log(
                    req,
                    "EVIDENCIAS",
                    "DESCARGA_DENEGADA",
                    "CASO",
                    c.getCode(),
                    "Intento de descargar evidencia que no pertenece al caso consultado",
                    ResultadoAuditoria.FALLIDO,
                    null,
                    null
            );
            throw new IllegalArgumentException(
                    "No posee permisos para descargar esta evidencia."
            );
        }
        // La evidencia debe estar autorizada para el cliente.
        if (
                !e.isVisibleToClient()
        ) {
            audit.log(
                    req,
                    "EVIDENCIAS",
                    "DESCARGA_DENEGADA",
                    "CASO",
                    c.getCode(),
                    "Intento de descargar evidencia no visible para el cliente",
                    ResultadoAuditoria.FALLIDO,
                    null,
                    null
            );
            throw new IllegalArgumentException(
                    "No posee permisos para descargar esta evidencia."
            );
        }
        /*
         * ServicioEvidencias.download recibe HttpServletRequest
         * para realizar la descarga controlada y su auditoría.
         */
        return evidence.download(
                evidenceId,
                req
        );
    }
    // =========================================================
    // CALIFICACIÓN
    // =========================================================
    @Transactional
    public void rate(
            String code,
            String email,
            String trackingKey,
            SolicitudSatisfaccion r,
            HttpServletRequest req
    ) {
        Caso c =
                verify(
                        code,
                        email,
                        trackingKey
                );
        if (
                c.getStatus()
                        != EstadoCaso.CERRADO
        ) {
            throw new IllegalArgumentException(
                    "La encuesta está disponible únicamente para casos cerrados."
            );
        }
        if (
                satisfaction
                        .existsByComplaintCaseId(
                                c.getId()
                        )
        ) {
            throw new IllegalArgumentException(
                    "La encuesta ya fue respondida."
            );
        }
        Satisfaccion s =
                new Satisfaccion();
        s.setComplaintCase(
                c
        );
        s.setRating(
                r.rating()
        );
        s.setComment(
                blankToNull(
                        r.comment()
                )
        );
        satisfaction.save(s);
        audit.log(
                req,
                "CASOS",
                "ENCUESTA_SATISFACCION",
                "CASO",
                c.getCode(),
                "Encuesta de satisfacción registrada",
                ResultadoAuditoria.EXITOSO,
                null,
                Map.of(
                        "calificacion",
                        r.rating()
                )
        );
    }
    // =========================================================
    // VERIFICAR ACCESO PÚBLICO
    // CU-03 FA01 / FA02 / FA05
    // =========================================================
    public Caso verify(
            String code,
            String email,
            String trackingKey
    ) {
        String codigo =
                normalizarCodigo(
                        code
                );
        validarFormatoCodigo(
                codigo
        );
        Caso c =
                cases.findByCode(
                                codigo
                        )
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                MENSAJE_CONSULTA_INVALIDA
                                        )
                        );
        boolean valid =
                false;
        // =====================================================
        // CASO IDENTIFICADO: CÓDIGO + CORREO
        // =====================================================
        if (
                email != null
                        && !email.isBlank()
                        && c.getEmail()
                        != null
                        && !c.getEmail().isBlank()
        ) {
            valid =
                    c.getEmail()
                            .equalsIgnoreCase(
                                    email.trim()
                            );
        }
        // =====================================================
        // CASO ANÓNIMO: CÓDIGO + CLAVE TEMPORAL
        // =====================================================
        if (
                !valid
                        && trackingKey
                        != null
                        && !trackingKey.isBlank()
                        && c.getTrackingKeyHash()
                        != null
        ) {
            String hash =
                    UtilidadHash.sha256(
                            trackingKey
                                    .trim()
                                    .toUpperCase(
                                            Locale.ROOT
                                    )
                    );
            valid =
                    hash.equals(
                            c.getTrackingKeyHash()
                    );
        }
        // =====================================================
        // MENSAJE GENÉRICO
        // NO REVELAR QUÉ DATO FALLÓ
        // =====================================================
        if (!valid) {
            throw new IllegalArgumentException(
                    MENSAJE_CONSULTA_INVALIDA
            );
        }
        return c;
    }
    // =========================================================
    // VALIDAR FORMATO DEL CÓDIGO
    // CU-03 FA04
    // =========================================================
    private void validarFormatoCodigo(
            String codigo
    ) {
        if (
                codigo == null
                        || !codigo.matches(
                        PATRON_CODIGO
                )
        ) {
            throw new IllegalArgumentException(
                    MENSAJE_CONSULTA_INVALIDA
            );
        }
    }
    // =========================================================
    // NORMALIZAR CÓDIGO
    // =========================================================
    private String normalizarCodigo(
            String codigo
    ) {
        if (codigo == null) {
            return "";
        }
        return codigo
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }
    // =========================================================
    // VALIDACIONES REGISTRO
    // =========================================================
    private void validateIdentity(
            SolicitudCreacionPublica r
    ) {
        if (
                r.incidentAt()
                        .isAfter(
                                LocalDateTime.now()
                        )
        ) {
            throw new IllegalArgumentException(
                    "La fecha del incidente no puede ser posterior a la fecha actual."
            );
        }
        if (
                r.confidential()
                        && r.type()
                        != TipoCaso.DENUNCIA
        ) {
            throw new IllegalArgumentException(
                    "La confidencialidad especial aplica únicamente a denuncias."
            );
        }
        if (
                !r.anonymous()
        ) {
            if (
                    r.fullName()
                            == null
                            || r.fullName()
                            .trim()
                            .split("\\s+")
                            .length < 2
            ) {
                throw new IllegalArgumentException(
                        "El nombre completo debe contener al menos dos palabras."
                );
            }
            if (
                    r.email()
                            == null
                            || r.email()
                            .isBlank()
            ) {
                throw new IllegalArgumentException(
                        "El correo electrónico es obligatorio para un registro identificado."
                );
            }
        }
    }
    // =========================================================
    // PRIORIDAD INICIAL
    // =========================================================
    private Prioridad initialPriority(
            TipoCaso type,
            CategoriaCaso category
    ) {
        if (
                category
                        == CategoriaCaso.HIGIENE
                        && type
                        != TipoCaso.SUGERENCIA
        ) {
            return Prioridad.CRITICA;
        }
        return rules.defaultPriority(
                type
        );
    }
    // =========================================================
    // LIMPIAR TEXTO
    // =========================================================
    private String clean(
            String s
    ) {
        return s == null
                ? null
                : s.trim()
                .replaceAll(
                        "\\s+",
                        " "
                );
    }
    // =========================================================
    // BLANCO A NULL
    // =========================================================
    private String blankToNull(
            String s
    ) {
        return s == null
                || s.isBlank()
                ? null
                : s.trim();
    }
}
