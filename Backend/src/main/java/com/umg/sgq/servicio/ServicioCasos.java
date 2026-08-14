package com.umg.sgq.servicio;

import com.umg.sgq.dto.DtosCasos.*;
import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.*;
import com.umg.sgq.repositorio.*;
import com.umg.sgq.seguridad.ServicioUsuarioActual;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.util.*;

@Service
public class ServicioCasos {
    private final RepositorioCaso cases;
    private final RepositorioUsuario users;
    private final RepositorioSeguimiento followUps;
    private final MapeadorCaso mapper;
    private final ServicioUsuarioActual current;
    private final ServicioReglasCaso rules;
    private final ServicioAuditoria audit;
    private final ServicioNotificaciones notifications;
    private final ServicioEvidencias evidence;
    private final RepositorioSucursal branchRepository;

    public ServicioCasos(RepositorioCaso cases, RepositorioUsuario users, RepositorioSeguimiento followUps, MapeadorCaso mapper, ServicioUsuarioActual current, ServicioReglasCaso rules, ServicioAuditoria audit, ServicioNotificaciones notifications, ServicioEvidencias evidence, RepositorioSucursal branchRepository) {
        this.cases = cases;
        this.users = users;
        this.followUps = followUps;
        this.mapper = mapper;
        this.current = current;
        this.rules = rules;
        this.audit = audit;
        this.notifications = notifications;
        this.evidence = evidence;
        this.branchRepository = branchRepository;
    }

    @Transactional(readOnly = true)
    public PaginaCasos search(List<EstadoCaso> statuses, String q, LocalDate from, LocalDate to, TipoCaso type, Prioridad priority, Long branchId, CategoriaCaso category, Long responsibleId, IndicadorSla sla, int page, int size) {
        Usuario u = current.require();
        Long scopeBranch = null, scopeResponsible = null;
        if (u.getRole().getCode() == CodigoRol.AGENTE_ATENCION) {
            scopeResponsible = u.getId();
            scopeBranch = u.getBranch() == null ? null : u.getBranch().getId();
        } else if (u.getRole().getCode() == CodigoRol.SUPERVISOR) {
            scopeBranch = u.getBranch() == null ? null : u.getBranch().getId();
        }
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        final Long sb = scopeBranch, sr = scopeResponsible;
        final String search = blank(q);
        final LocalDateTime f = from == null ? null : from.atStartOfDay();
        final LocalDateTime t = to == null ? null : to.plusDays(1).atStartOfDay().minusNanos(1);
        Specification<Caso> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (sb != null) ps.add(cb.equal(root.get("branch").get("id"), sb));
            if (sr != null) ps.add(cb.equal(root.get("responsible").get("id"), sr));
            if (branchId != null) ps.add(cb.equal(root.get("branch").get("id"), branchId));
            if (responsibleId != null) ps.add(cb.equal(root.get("responsible").get("id"), responsibleId));
            if (statuses != null && !statuses.isEmpty()) ps.add(root.get("status").in(statuses));
            if (type != null) ps.add(cb.equal(root.get("type"), type));
            if (priority != null) ps.add(cb.equal(root.get("priority"), priority));
            if (category != null) ps.add(cb.equal(root.get("category"), category));
            if (f != null) ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f));
            if (t != null) ps.add(cb.lessThanOrEqualTo(root.get("createdAt"), t));
            if (search != null) {
                String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("code")), like), cb.like(cb.lower(root.get("fullName")), like)));
            }
            if (sla == IndicadorSla.VENCIDO) ps.add(cb.isTrue(root.get("slaBreached")));
            else if (sla == IndicadorSla.PROXIMO_A_VENCER) {
                ps.add(cb.isTrue(root.get("slaWarningSent")));
                ps.add(cb.isFalse(root.get("slaBreached")));
            } else if (sla == IndicadorSla.EN_TIEMPO) {
                ps.add(cb.isFalse(root.get("slaWarningSent")));
                ps.add(cb.isFalse(root.get("slaBreached")));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
        int safeSize = Math.max(5, Math.min(size, 100));
        Page<Caso> result = cases.findAll(spec, PageRequest.of(Math.max(0, page), safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        boolean revealRole = u.getRole().getCode() != CodigoRol.AGENTE_ATENCION;
        List<VistaCasoInterno> content = result.getContent().stream().map(c -> mapper.internalView(c, revealRole || !c.isConfidential())).toList();
        return new PaginaCasos(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public VistaCasoInterno detail(Long id, HttpServletRequest req) {
        Caso c = requireAccessible(id);
        Usuario u = current.require();
        boolean reveal = u.getRole().getCode() != CodigoRol.AGENTE_ATENCION || !c.isConfidential();
        audit.log(req, "CASOS", "CONSULTA_DETALLE", "CASO", c.getCode(), "Consulta de detalle de caso", ResultadoAuditoria.EXITOSO, null, null);
        return mapper.internalView(c, reveal);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> availableAgents(Long caseId) {
        Caso c = requireAccessible(caseId);
        return users.findActiveByRoleAndBranch(CodigoRol.AGENTE_ATENCION, EstadoRegistro.ACTIVO, c.getBranch().getId()).stream().map(u -> Map.<String, Object>of("id", u.getId(), "name", u.getFullName(), "openCases", cases.countByResponsibleIdAndStatusNotIn(u.getId(), List.of(EstadoCaso.CERRADO, EstadoCaso.CANCELADO, EstadoCaso.RECHAZADO)))).toList();
    }

    @Transactional
    public VistaCasoInterno edit(Long id, SolicitudEdicionCaso r, HttpServletRequest req) {
        requireSupervisorOrAdmin();
        Caso c = requireAccessible(id);
        if (c.getStatus() == EstadoCaso.CERRADO)
            throw new IllegalArgumentException("El caso se encuentra cerrado y no puede modificarse.");
        Sucursal branch = branchesRef(r.branchId());
        if (c.getResponsible() != null && !Objects.equals(c.getBranch().getId(), branch.getId()))
            throw new IllegalArgumentException("Reasigne o retire el responsable antes de cambiar la sucursal.");
        Map<String, Object> old = new LinkedHashMap<>();
        old.put("tipo", c.getType());
        old.put("sucursal", c.getBranch().getId());
        old.put("categoria", c.getCategory());
        old.put("prioridad", c.getPriority());
        old.put("pedido", c.getOrderNumber());
        Prioridad oldPriority = c.getPriority();
        c.setType(r.type());
        c.setBranch(branch);
        c.setCategory(r.category());
        c.setPriority(r.priority());
        c.setOrderNumber(blank(r.orderNumber()));
        c.setAdministrativeObservation(blank(r.administrativeObservation()));
        if (oldPriority != r.priority() && c.getFirstResponseAt() == null) {
            rules.initializeSla(c);
            c.setSlaWarningSent(false);
            c.setSlaBreached(false);
        }
        cases.save(c);
        Map<String, Object> nv = new LinkedHashMap<>();
        nv.put("tipo", c.getType());
        nv.put("sucursal", c.getBranch().getId());
        nv.put("categoria", c.getCategory());
        nv.put("prioridad", c.getPriority());
        nv.put("pedido", c.getOrderNumber());
        audit.log(req, "CASOS", "ACTUALIZACION", "CASO", c.getCode(), "Información administrativa del caso actualizada", ResultadoAuditoria.EXITOSO, old, nv);
        return mapper.internalView(c, true);
    }

    @Transactional
    public VistaCasoInterno assign(Long id, SolicitudAsignacion r, HttpServletRequest req) {
        requireSupervisorOrAdmin();
        Caso c = requireAccessible(id);
        if (Set.of(EstadoCaso.CERRADO, EstadoCaso.CANCELADO, EstadoCaso.RECHAZADO).contains(c.getStatus()))
            throw new IllegalArgumentException("El caso se encuentra en estado " + c.getStatus() + " y no permite esta operación.");
        Usuario agent = users.findById(r.responsibleId()).orElseThrow(() -> new IllegalArgumentException("El responsable seleccionado no existe."));
        if (agent.getStatus() != EstadoRegistro.ACTIVO || agent.getRole().getCode() != CodigoRol.AGENTE_ATENCION)
            throw new IllegalArgumentException("El responsable seleccionado no se encuentra activo.");
        if (agent.getBranch() == null || !Objects.equals(agent.getBranch().getId(), c.getBranch().getId()))
            throw new IllegalArgumentException("El responsable no tiene acceso a la sucursal del caso.");
        Usuario old = c.getResponsible();
        boolean reassignment = old != null && !Objects.equals(old.getId(), agent.getId());
        if (old != null && Objects.equals(old.getId(), agent.getId()))
            throw new IllegalArgumentException("El caso ya cuenta con ese responsable activo.");
        if (reassignment && (r.reason() == null || r.reason().isBlank()))
            throw new IllegalArgumentException("La reasignación requiere un motivo obligatorio.");
        EstadoCaso oldStatus = c.getStatus();
        c.setResponsible(agent);
        if (old == null && (oldStatus == EstadoCaso.PENDIENTE_ASIGNACION || oldStatus == EstadoCaso.REABIERTO)) {
            rules.requireTransition(oldStatus, EstadoCaso.ASIGNADO);
            c.setStatus(EstadoCaso.ASIGNADO);
        }
        cases.save(c);
        String action = reassignment ? "REASIGNACION" : "ASIGNACION";
        audit.log(req, "CASOS", action, "CASO", c.getCode(), reassignment ? "Caso reasignado. Motivo: " + r.reason() : "Caso asignado a " + agent.getUsername(), ResultadoAuditoria.EXITOSO, old == null ? null : Map.of("responsable", old.getUsername()), Map.of("responsable", agent.getUsername()));
        notifications.internal(reassignment ? EventoNotificacion.REASIGNACION : EventoNotificacion.ASIGNACION, c, agent, "Se le asignó el caso " + c.getCode());
        notifications.email(reassignment ? EventoNotificacion.REASIGNACION : EventoNotificacion.ASIGNACION, c, agent.getEmail(), "Se le asignó el caso " + c.getCode());
        return mapper.internalView(c, true);
    }

    @Transactional
    public VistaCasoInterno followUp(Long id, SolicitudSeguimiento r, List<MultipartFile> files, HttpServletRequest req) {
        Caso c = requireAccessible(id);
        Usuario u = current.require();
        if (c.getStatus() == EstadoCaso.CERRADO)
            throw new IllegalArgumentException("El caso se encuentra cerrado y no puede modificarse.");
        if (u.getRole().getCode() == CodigoRol.AGENTE_ATENCION && (c.getResponsible() == null || !Objects.equals(c.getResponsible().getId(), u.getId())))
            throw new IllegalArgumentException("No posee permisos para realizar esta acción.");
        boolean visible = r.type() == TipoSeguimiento.COMENTARIO_INTERNO ? false : r.visibleToClient();
        EstadoCaso target = r.newStatus();
        if (target == EstadoCaso.CERRADO || target == EstadoCaso.REABIERTO)
            throw new IllegalArgumentException("Ese estado requiere su operación específica.");
        if (target == null && (c.getStatus() == EstadoCaso.ASIGNADO || c.getStatus() == EstadoCaso.REABIERTO))
            target = EstadoCaso.EN_PROCESO;
        EstadoCaso oldStatus = c.getStatus();
        if (target != null && target != oldStatus) {
            rules.requireTransition(oldStatus, target);
            rules.applyTransitionSla(c, oldStatus, target);
            c.setStatus(target);
        }
        Seguimiento f = new Seguimiento();
        f.setComplaintCase(c);
        f.setAuthor(u);
        f.setAuthorLabel(u.getFullName());
        f.setType(r.type());
        f.setDescription(r.description().trim());
        f.setVisibleToClient(visible);
        f.setResultingStatus(c.getStatus());
        f = followUps.saveAndFlush(f);
        if (c.getFirstResponseAt() == null) {
            c.setFirstResponseAt(LocalDateTime.now());
        }
        cases.save(c);
        if (files != null && !files.isEmpty()) evidence.store(c, f, files, req);
        audit.log(req, "SEGUIMIENTOS", "CREACION", "CASO", c.getCode(), "Seguimiento registrado: " + r.type(), ResultadoAuditoria.EXITOSO, Map.of("estado", oldStatus.name()), Map.of("estado", c.getStatus().name(), "visibleCliente", visible));
        if (r.type() == TipoSeguimiento.SOLICITUD_INFORMACION && c.getEmail() != null)
            notifications.email(EventoNotificacion.SOLICITUD_INFORMACION, c, c.getEmail(), r.description());
        else if (visible && c.getEmail() != null)
            notifications.email(EventoNotificacion.SEGUIMIENTO_VISIBLE, c, c.getEmail(), "Existe un nuevo seguimiento visible en su caso.");
        return mapper.internalView(c, canReveal(c, u));
    }

    @Transactional
    public VistaCasoInterno resolve(Long id, SolicitudResolucion r, HttpServletRequest req) {
        Caso c = requireAccessible(id);
        Usuario u = current.require();
        if (c.getResponsible() == null) throw new IllegalArgumentException("El caso no posee responsable.");
        if (u.getRole().getCode() == CodigoRol.AGENTE_ATENCION && !Objects.equals(c.getResponsible().getId(), u.getId()))
            throw new IllegalArgumentException("No posee permisos para realizar esta acción.");
        if (followUps.countByComplaintCaseId(c.getId()) < 1)
            throw new IllegalArgumentException("Debe registrar al menos un seguimiento antes de resolver el caso.");
        EstadoCaso old = c.getStatus();
        rules.requireTransition(old, EstadoCaso.RESUELTO);
        rules.applyTransitionSla(c, old, EstadoCaso.RESUELTO);
        c.setResolution(r.resolution().trim());
        c.setResolutionAt(LocalDateTime.now());
        c.setResolvedBy(u);
        c.setStatus(EstadoCaso.RESUELTO);
        cases.save(c);
        audit.log(req, "CASOS", "RESOLUCION", "CASO", c.getCode(), "Resolución registrada", ResultadoAuditoria.EXITOSO, Map.of("estado", old.name()), Map.of("estado", c.getStatus().name()));
        if (c.getEmail() != null)
            notifications.email(EventoNotificacion.CAMBIO_RESUELTO, c, c.getEmail(), "El caso fue marcado como Resuelto.");
        if (c.getBranch().getSupervisor() != null)
            notifications.internal(EventoNotificacion.CAMBIO_RESUELTO, c, c.getBranch().getSupervisor(), "Caso resuelto pendiente de cierre: " + c.getCode());
        return mapper.internalView(c, canReveal(c, u));
    }

    @Transactional
    public VistaCasoInterno close(Long id, SolicitudCierre r, HttpServletRequest req) {
        requireSupervisorOrAdmin();
        Caso c = requireAccessible(id);
        Usuario closer = current.require();
        if (c.getResponsible() == null)
            throw new IllegalArgumentException("No podrá cerrarse un caso sin responsable.");
        if (followUps.countByComplaintCaseId(c.getId()) < 1)
            throw new IllegalArgumentException("Debe registrar al menos un seguimiento antes de resolver el caso.");
        if (c.getResolution() == null || c.getResolution().isBlank())
            throw new IllegalArgumentException("Debe registrar una resolución antes de cerrar el caso.");
        if ((c.getPriority() == Prioridad.CRITICA || c.isConfidential()) && !r.criticalReviewConfirmed())
            throw new IllegalArgumentException("La revisión obligatoria del caso crítico se encuentra pendiente.");
        if (r.reason() == MotivoCierre.OTRO && (r.detailReason() == null || r.detailReason().trim().length() < 10))
            throw new IllegalArgumentException("El detalle del motivo Otro debe contener al menos 10 caracteres.");
        if (r.reason() == MotivoCierre.DUPLICADO) {
            if (r.duplicateCaseCode() == null || r.duplicateCaseCode().isBlank())
                throw new IllegalArgumentException("Debe indicar el código del caso principal duplicado.");
            Caso main = cases.findByCode(r.duplicateCaseCode().trim().toUpperCase(Locale.ROOT)).orElseThrow(() -> new IllegalArgumentException("El código de seguimiento no existe o los datos de consulta son incorrectos."));
            if (Objects.equals(main.getId(), c.getId()))
                throw new IllegalArgumentException("El caso duplicado no puede referirse a sí mismo.");
            c.setDuplicateCaseCode(main.getCode());
        }
        if (r.reason() == MotivoCierre.CLIENTE_NO_RESPONDIO && followUps.findByComplaintCaseIdOrderByCreatedAtAsc(c.getId()).stream().noneMatch(f -> f.getType() == TipoSeguimiento.SOLICITUD_INFORMACION))
            throw new IllegalArgumentException("El motivo Cliente no respondió requiere una solicitud de información previa.");
        EstadoCaso old = c.getStatus();
        rules.requireTransition(old, EstadoCaso.CERRADO);
        c.setStatus(EstadoCaso.CERRADO);
        c.setCloseReason(r.reason());
        c.setCloseComment(r.summary().trim() + (r.detailReason() == null || r.detailReason().isBlank() ? "" : " | " + r.detailReason().trim()));
        c.setCloseInternalObservation(blank(r.internalObservation()));
        c.setClosedAt(LocalDateTime.now());
        c.setClosedBy(closer);
        cases.save(c);
        audit.log(req, "CASOS", "CIERRE", "CASO", c.getCode(), "Caso cerrado. Motivo: " + r.reason(), ResultadoAuditoria.EXITOSO, Map.of("estado", old.name()), Map.of("estado", c.getStatus().name(), "motivo", r.reason().name()));
        if (r.notifyClient() && c.getEmail() != null)
            notifications.email(EventoNotificacion.CIERRE, c, c.getEmail(), "El caso ha sido cerrado." + (r.sendSurvey() ? " Puede responder la encuesta de satisfacción." : ""));
        if (c.getResponsible() != null)
            notifications.internal(EventoNotificacion.CIERRE, c, c.getResponsible(), "El caso " + c.getCode() + " fue cerrado.");
        return mapper.internalView(c, true);
    }

    @Transactional
    public VistaCasoInterno reopen(Long id, SolicitudReapertura r, HttpServletRequest req) {
        requireSupervisorOrAdmin();
        Caso c = requireAccessible(id);
        EstadoCaso old = c.getStatus();
        if (!Set.of(EstadoCaso.CERRADO, EstadoCaso.RESUELTO, EstadoCaso.RECHAZADO, EstadoCaso.CANCELADO).contains(old))
            throw new IllegalArgumentException("El caso se encuentra en estado " + old + " y no permite esta operación.");
        if (old == EstadoCaso.CERRADO && c.getClosedAt() != null && c.getClosedAt().plusDays(15).isBefore(LocalDateTime.now()) && !r.specialJustification())
            throw new IllegalArgumentException("El plazo ordinario para reabrir el caso ha vencido.");
        rules.requireTransition(old, EstadoCaso.REABIERTO);
        c.setStatus(EstadoCaso.REABIERTO);
        c.setReopenedAt(LocalDateTime.now());
        c.setReopenReason(r.reason().trim());
        c.setReopenRequestedAt(null);
        c.setReopenRequestReason(null);
        cases.save(c);
        audit.log(req, "CASOS", "REAPERTURA", "CASO", c.getCode(), "Caso reabierto. Motivo: " + r.reason(), ResultadoAuditoria.EXITOSO, Map.of("estado", old.name()), Map.of("estado", c.getStatus().name()));
        if (c.getResponsible() != null)
            notifications.internal(EventoNotificacion.REASIGNACION, c, c.getResponsible(), "El caso " + c.getCode() + " fue reabierto.");
        if (c.getEmail() != null)
            notifications.email(EventoNotificacion.REASIGNACION, c, c.getEmail(), "Su caso fue reabierto.");
        return mapper.internalView(c, true);
    }

    @Transactional
    public VistaCasoInterno priority(Long id, SolicitudPrioridad r, HttpServletRequest req) {
        requireSupervisorOrAdmin();
        Caso c = requireAccessible(id);
        Prioridad old = c.getPriority();
        c.setPriority(r.priority());
        if (c.getFirstResponseAt() == null) {
            rules.initializeSla(c);
            c.setSlaWarningSent(false);
            c.setSlaBreached(false);
        }
        cases.save(c);
        audit.log(req, "CASOS", "CAMBIO_PRIORIDAD", "CASO", c.getCode(), "Prioridad modificada", ResultadoAuditoria.EXITOSO, Map.of("prioridad", old.name()), Map.of("prioridad", c.getPriority().name()));
        return mapper.internalView(c, true);
    }

    @Transactional
    public VistaCasoInterno status(Long id, SolicitudEstado r, HttpServletRequest req) {
        requireSupervisorOrAdmin();
        Caso c = requireAccessible(id);
        if (!Set.of(EstadoCaso.RECHAZADO, EstadoCaso.CANCELADO).contains(r.status()))
            throw new IllegalArgumentException("Use la operación específica para el estado solicitado.");
        EstadoCaso old = c.getStatus();
        rules.requireTransition(old, r.status());
        rules.applyTransitionSla(c, old, r.status());
        c.setStatus(r.status());
        cases.save(c);
        audit.log(req, "CASOS", "CAMBIO_ESTADO", "CASO", c.getCode(), "Estado cambiado a " + r.status() + ". Motivo: " + r.reason(), ResultadoAuditoria.EXITOSO, Map.of("estado", old.name()), Map.of("estado", r.status().name()));
        if (c.getEmail() != null)
            notifications.email(r.status() == EstadoCaso.RECHAZADO ? EventoNotificacion.RECHAZO : EventoNotificacion.CIERRE, c, c.getEmail(), "El caso cambió a " + r.status() + ".");
        return mapper.internalView(c, true);
    }

    @Transactional
    public VistaEvidencia addEvidence(Long id, String description, boolean visibleToClient, MultipartFile file, HttpServletRequest req) {
        Caso c = requireAccessible(id);
        if (c.getStatus() == EstadoCaso.CERRADO)
            throw new IllegalArgumentException("El caso se encuentra cerrado y no puede modificarse.");
        Usuario u = current.require();
        if (u.getRole().getCode() == CodigoRol.AGENTE_ATENCION && (c.getResponsible() == null || !Objects.equals(c.getResponsible().getId(), u.getId())))
            throw new IllegalArgumentException("No posee permisos para realizar esta acción.");
        Evidencia e = evidence.store(c, null, List.of(file), visibleToClient, description, req).get(0);
        if (visibleToClient && c.getEmail() != null)
            notifications.email(EventoNotificacion.SEGUIMIENTO_VISIBLE, c, c.getEmail(), "Se adjuntó una nueva evidencia visible a su caso.");
        return mapper.evidence(e);
    }

    public Caso requireAccessible(Long id) {
        Caso c = cases.findById(id).orElseThrow(() -> new IllegalArgumentException("Caso no encontrado."));
        Usuario u = current.require();
        CodigoRol role = u.getRole().getCode();
        if (role == CodigoRol.AGENTE_ATENCION) {
            if (c.getResponsible() == null || !Objects.equals(c.getResponsible().getId(), u.getId()))
                throw new IllegalArgumentException("No posee permisos para realizar esta acción.");
        }
        if (role == CodigoRol.SUPERVISOR && u.getBranch() != null && !Objects.equals(c.getBranch().getId(), u.getBranch().getId()))
            throw new IllegalArgumentException("No posee permisos para realizar esta acción.");
        return c;
    }

    private Sucursal branchesRef(Long id) {
        return cBranchRepo().findById(id).filter(b -> b.getStatus() == EstadoRegistro.ACTIVO).orElseThrow(() -> new IllegalArgumentException("La sucursal seleccionada se encuentra inactiva."));
    }

    private RepositorioSucursal cBranchRepo() {
        return branchRepository;
    }

    private void requireSupervisorOrAdmin() {
        CodigoRol r = current.require().getRole().getCode();
        if (r != CodigoRol.SUPERVISOR && r != CodigoRol.ADMINISTRADOR)
            throw new IllegalArgumentException("No posee permisos para realizar esta acción.");
    }

    private boolean canReveal(Caso c, Usuario u) {
        return !c.isConfidential() || u.getRole().getCode() != CodigoRol.AGENTE_ATENCION;
    }

    private String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
