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

    public ServicioCasosPublicos(RepositorioCaso cases, RepositorioSucursal branches, ServicioCodigoCaso codes, ServicioReglasCaso rules, ServicioEvidencias evidence, MapeadorCaso mapper, ServicioAuditoria audit, ServicioNotificaciones notifications, RepositorioSatisfaccion satisfaction, RepositorioSeguimiento followUps, RepositorioUsuario users) {
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
    }

    @Transactional
    public RespuestaCreacionPublica register(SolicitudCreacionPublica r, List<MultipartFile> files, HttpServletRequest req) {
        validateIdentity(r);
        Sucursal branch = branches.findById(r.branchId()).orElseThrow(() -> new IllegalArgumentException("La sucursal seleccionada no existe."));
        if (branch.getStatus() != EstadoRegistro.ACTIVO)
            throw new IllegalArgumentException("La sucursal seleccionada se encuentra inactiva.");
        if (files != null) {
            if (files.size() > 5)
                throw new IllegalArgumentException("Se alcanzó la cantidad máxima de archivos permitidos.");
            files.forEach(f -> {
            });
        } // validación completa se realiza después de persistir el caso
        String key = UtilidadClaveAleatoria.trackingKey();
        Caso c = new Caso();
        c.setCode(codes.next(r.type()));
        c.setType(r.type());
        c.setPriority(initialPriority(r.type(), r.category()));
        c.setCategory(r.category());
        c.setStatus(EstadoCaso.REGISTRADO);
        c.setAnonymous(r.anonymous());
        c.setConfidential(r.confidential());
        c.setFullName(r.anonymous() ? null : clean(r.fullName()));
        c.setEmail(r.anonymous() ? null : blankToNull(r.email()));
        c.setPhone(r.anonymous() ? null : blankToNull(r.phone()));
        c.setTrackingKeyHash(UtilidadHash.sha256(key));
        c.setBranch(branch);
        c.setOrderNumber(blankToNull(r.orderNumber()));
        c.setIncidentAt(r.incidentAt());
        c.setDescription(r.description().trim());
        c.setContactAuthorized(!r.anonymous() && r.contactAuthorized());
        c = cases.saveAndFlush(c);
        rules.initializeSla(c);
        EstadoCaso old = c.getStatus();
        rules.requireTransition(old, EstadoCaso.PENDIENTE_ASIGNACION);
        c.setStatus(EstadoCaso.PENDIENTE_ASIGNACION);
        cases.saveAndFlush(c);
        audit.log(req, "CASOS", "CREACION", "CASO", c.getCode(), "Caso registrado y movido automáticamente a Pendiente de Asignación", ResultadoAuditoria.EXITOSO, Map.of("estado", old.name()), Map.of("estado", c.getStatus().name(), "tipo", c.getType().name(), "prioridad", c.getPriority().name()));
        if (files != null && !files.isEmpty()) evidence.store(c, null, files, req);
        if (c.getEmail() != null)
            notifications.email(EventoNotificacion.REGISTRO_CASO, c, c.getEmail(), "El caso fue registrado correctamente. Conserve su código de seguimiento.");
        return new RespuestaCreacionPublica(c.getCode(), r.anonymous() ? key : null, c.getStatus().name(), "El caso se registró con éxito. Código de seguimiento: " + c.getCode() + ".");
    }

    @Transactional
    public VistaCasoPublico lookup(SolicitudConsultaPublica r, HttpServletRequest req) {
        Caso c = verify(r.code(), r.email(), r.trackingKey());
        audit.log(req, "CASOS", "CONSULTA_PUBLICA", "CASO", c.getCode(), "Consulta pública de estado del caso", ResultadoAuditoria.EXITOSO, null, null);
        return mapper.publicView(c);
    }

    @Transactional
    public VistaEvidencia addEvidence(String code, String email, String trackingKey, MultipartFile file, HttpServletRequest req) {
        Caso c = verify(code, email, trackingKey);
        if (c.getStatus() == EstadoCaso.CERRADO)
            throw new IllegalArgumentException("El caso se encuentra cerrado y no puede modificarse.");
        Evidencia e = evidence.store(c, null, List.of(file), req).get(0);
        return mapper.evidence(e);
    }

    @Transactional
    public VistaCasoPublico respond(String code, String email, String trackingKey, SolicitudRespuestaPublica r, List<MultipartFile> files, HttpServletRequest req) {
        Caso c = verify(code, email, trackingKey);
        if (c.getStatus() != EstadoCaso.EN_ESPERA_CLIENTE)
            throw new IllegalArgumentException("El caso se encuentra en estado " + c.getStatus() + " y no permite esta operación.");
        EstadoCaso old = c.getStatus();
        rules.requireTransition(old, EstadoCaso.EN_PROCESO);
        rules.applyTransitionSla(c, old, EstadoCaso.EN_PROCESO);
        c.setStatus(EstadoCaso.EN_PROCESO);
        cases.save(c);
        Seguimiento f = new Seguimiento();
        f.setComplaintCase(c);
        f.setAuthor(null);
        f.setAuthorLabel("CLIENTE");
        f.setType(TipoSeguimiento.RESPUESTA_CLIENTE);
        f.setDescription(r.response().trim());
        f.setVisibleToClient(true);
        f.setResultingStatus(EstadoCaso.EN_PROCESO);
        f = followUps.saveAndFlush(f);
        if (files != null && !files.isEmpty())
            evidence.store(c, f, files, true, "Evidencia enviada por el cliente", req);
        audit.log(req, "SEGUIMIENTOS", "RESPUESTA_CLIENTE", "CASO", c.getCode(), "El cliente respondió una solicitud de información", ResultadoAuditoria.EXITOSO, Map.of("estado", old.name()), Map.of("estado", c.getStatus().name()));
        if (c.getResponsible() != null)
            notifications.internal(EventoNotificacion.SEGUIMIENTO_VISIBLE, c, c.getResponsible(), "El cliente respondió el caso " + c.getCode());
        return mapper.publicView(c);
    }

    @Transactional
    public VistaCasoPublico cancel(String code, String email, String trackingKey, SolicitudMotivoPublico r, HttpServletRequest req) {
        Caso c = verify(code, email, trackingKey);
        if (!Set.of(EstadoCaso.REGISTRADO, EstadoCaso.PENDIENTE_ASIGNACION, EstadoCaso.ASIGNADO, EstadoCaso.EN_PROCESO).contains(c.getStatus()))
            throw new IllegalArgumentException("El caso se encuentra en estado " + c.getStatus() + " y no permite esta operación.");
        EstadoCaso old = c.getStatus();
        rules.requireTransition(old, EstadoCaso.CANCELADO);
        rules.applyTransitionSla(c, old, EstadoCaso.CANCELADO);
        c.setStatus(EstadoCaso.CANCELADO);
        c.setCloseComment("Cancelación solicitada por cliente: " + r.reason().trim());
        cases.save(c);
        audit.log(req, "CASOS", "CANCELACION_CLIENTE", "CASO", c.getCode(), "Caso cancelado por solicitud del cliente. Motivo: " + r.reason(), ResultadoAuditoria.EXITOSO, Map.of("estado", old.name()), Map.of("estado", c.getStatus().name()));
        if (c.getResponsible() != null)
            notifications.internal(EventoNotificacion.CIERRE, c, c.getResponsible(), "El cliente canceló el caso " + c.getCode());
        return mapper.publicView(c);
    }

    @Transactional
    public void requestReopen(String code, String email, String trackingKey, SolicitudMotivoPublico r, HttpServletRequest req) {
        Caso c = verify(code, email, trackingKey);
        if (c.getStatus() != EstadoCaso.CERRADO)
            throw new IllegalArgumentException("Solo se puede solicitar reapertura de un caso cerrado.");
        if (c.getClosedAt() == null || c.getClosedAt().plusDays(15).isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("El plazo ordinario para reabrir el caso ha vencido.");
        c.setReopenRequestedAt(LocalDateTime.now());
        c.setReopenRequestReason(r.reason().trim());
        cases.save(c);
        audit.log(req, "CASOS", "SOLICITUD_REAPERTURA", "CASO", c.getCode(), "Cliente solicitó reapertura. Motivo: " + r.reason(), ResultadoAuditoria.EXITOSO, null, Map.of("motivo", r.reason()));
        if (c.getBranch().getSupervisor() != null)
            notifications.internal(EventoNotificacion.REASIGNACION, c, c.getBranch().getSupervisor(), "Solicitud de reapertura del caso " + c.getCode());
        for (Usuario a : users.findActiveByRoleAndBranch(CodigoRol.ADMINISTRADOR, EstadoRegistro.ACTIVO, null))
            notifications.internal(EventoNotificacion.REASIGNACION, c, a, "Solicitud de reapertura del caso " + c.getCode());
    }

    @Transactional
    public ServicioEvidencias.Download download(String code, Long evidenceId, String email, String trackingKey, HttpServletRequest req) {
        Caso c = verify(code, email, trackingKey);
        Evidencia e = evidence.require(evidenceId);
        if (!Objects.equals(e.getComplaintCase().getId(), c.getId()) || !e.isVisibleToClient())
            throw new IllegalArgumentException("No posee permisos para descargar esta evidencia.");
        return evidence.download(evidenceId, req);
    }

    @Transactional
    public void rate(String code, String email, String trackingKey, SolicitudSatisfaccion r, HttpServletRequest req) {
        Caso c = verify(code, email, trackingKey);
        if (c.getStatus() != EstadoCaso.CERRADO)
            throw new IllegalArgumentException("La encuesta está disponible únicamente para casos cerrados.");
        if (satisfaction.existsByComplaintCaseId(c.getId()))
            throw new IllegalArgumentException("La encuesta ya fue respondida.");
        Satisfaccion s = new Satisfaccion();
        s.setComplaintCase(c);
        s.setRating(r.rating());
        s.setComment(blankToNull(r.comment()));
        satisfaction.save(s);
        audit.log(req, "CASOS", "ENCUESTA_SATISFACCION", "CASO", c.getCode(), "Encuesta de satisfacción registrada", ResultadoAuditoria.EXITOSO, null, Map.of("calificacion", r.rating()));
    }

    public Caso verify(String code, String email, String trackingKey) {
        Caso c = cases.findByCode(code == null ? "" : code.trim().toUpperCase(Locale.ROOT)).orElseThrow(() -> new IllegalArgumentException("El código de seguimiento no existe o los datos de consulta son incorrectos."));
        boolean valid = false;
        if (email != null && c.getEmail() != null) valid = c.getEmail().equalsIgnoreCase(email.trim());
        if (!valid && trackingKey != null && !trackingKey.isBlank())
            valid = UtilidadHash.sha256(trackingKey.trim().toUpperCase(Locale.ROOT)).equals(c.getTrackingKeyHash());
        if (!valid)
            throw new IllegalArgumentException("El código de seguimiento no existe o los datos de consulta son incorrectos.");
        return c;
    }

    private void validateIdentity(SolicitudCreacionPublica r) {
        if (r.incidentAt().isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("La fecha del incidente no puede ser posterior a la fecha actual.");
        if (r.confidential() && r.type() != TipoCaso.DENUNCIA)
            throw new IllegalArgumentException("La confidencialidad especial aplica únicamente a denuncias.");
        if (!r.anonymous()) {
            if (r.fullName() == null || r.fullName().trim().split("\\s+").length < 2)
                throw new IllegalArgumentException("El nombre completo debe contener al menos dos palabras.");
            if (r.email() == null || r.email().isBlank())
                throw new IllegalArgumentException("El correo electrónico es obligatorio para un registro identificado.");
        }
    }

    private Prioridad initialPriority(TipoCaso type, CategoriaCaso category) {
        if (category == CategoriaCaso.HIGIENE && type != TipoCaso.SUGERENCIA) return Prioridad.CRITICA;
        return rules.defaultPriority(type);
    }

    private boolean hasContact(SolicitudCreacionPublica r) {
        return (r.email() != null && !r.email().isBlank()) || (r.phone() != null && !r.phone().isBlank());
    }

    private String clean(String s) {
        return s == null ? null : s.trim().replaceAll("\\s+", " ");
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
