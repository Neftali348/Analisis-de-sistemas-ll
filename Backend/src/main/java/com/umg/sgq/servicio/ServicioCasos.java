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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    private final RepositorioEvidencia evidenceRepository;

    public ServicioCasos(
            RepositorioCaso cases,
            RepositorioUsuario users,
            RepositorioSeguimiento followUps,
            MapeadorCaso mapper,
            ServicioUsuarioActual current,
            ServicioReglasCaso rules,
            ServicioAuditoria audit,
            ServicioNotificaciones notifications,
            ServicioEvidencias evidence,
            RepositorioSucursal branchRepository,
            RepositorioEvidencia evidenceRepository
    ) {
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
        this.evidenceRepository = evidenceRepository;
    }

    @Transactional(readOnly = true)
    public PaginaCasos search(
            List<EstadoCaso> statuses,
            String code,
            LocalDate from,
            LocalDate to,
            TipoCaso type,
            Prioridad priority,
            Long branchId,
            CategoriaCaso category,
            Long responsibleId,
            IndicadorSla sla,
            int page,
            int size
    ) {

        Usuario u = current.require();

        Long scopeBranch = null;
        Long scopeResponsible = null;

        if (u.getRole().getCode() == CodigoRol.AGENTE_ATENCION) {

            scopeResponsible = u.getId();

            scopeBranch =
                    u.getBranch() == null
                            ? null
                            : u.getBranch().getId();

        } else if (
                u.getRole().getCode() == CodigoRol.SUPERVISOR
        ) {

            scopeBranch =
                    u.getBranch() == null
                            ? null
                            : u.getBranch().getId();
        }

        // =====================================================
        // FA02 - rango de fechas
        // =====================================================

        if (
                from != null &&
                        to != null &&
                        from.isAfter(to)
        ) {

            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final."
            );
        }

        final Long sb = scopeBranch;
        final Long sr = scopeResponsible;

        final String codigo = blank(code);

        /*
         * FA01 / RN05
         *
         * El CU permite buscar por código completo
         * o una parte del código.
         *
         * Si tiene la longitud de un código completo,
         * validamos su estructura.
         */
        if (
                codigo != null &&
                        codigo.length() >= 15 &&
                        !codigo.toUpperCase(Locale.ROOT)
                                .matches(
                                        "^(QUE|REC|DEN|SUG)-\\d{4}-\\d{6}$"
                                )
        ) {

            throw new IllegalArgumentException(
                    "El código de seguimiento no existe o los datos de consulta son incorrectos."
            );
        }

        final LocalDateTime f =
                from == null
                        ? null
                        : from.atStartOfDay();

        final LocalDateTime t =
                to == null
                        ? null
                        : to.plusDays(1)
                        .atStartOfDay()
                        .minusNanos(1);

        Specification<Caso> spec =
                (root, query, cb) -> {

                    List<Predicate> ps =
                            new ArrayList<>();

                    // =================================================
                    // RN01 / RN21
                    // Ámbito del usuario
                    // =================================================

                    if (sb != null) {

                        ps.add(
                                cb.equal(
                                        root.get("branch").get("id"),
                                        sb
                                )
                        );
                    }

                    if (sr != null) {

                        ps.add(
                                cb.equal(
                                        root.get("responsible").get("id"),
                                        sr
                                )
                        );
                    }

                    // =================================================
                    // FA06 - Sucursal
                    // =================================================

                    if (branchId != null) {

                        ps.add(
                                cb.equal(
                                        root.get("branch").get("id"),
                                        branchId
                                )
                        );
                    }

                    // =================================================
                    // FA06 - Responsable
                    // =================================================

                    if (responsibleId != null) {

                        ps.add(
                                cb.equal(
                                        root.get("responsible").get("id"),
                                        responsibleId
                                )
                        );
                    }

                    // =================================================
                    // FA04 - uno o varios estados
                    // =================================================

                    if (
                            statuses != null &&
                                    !statuses.isEmpty()
                    ) {

                        ps.add(
                                root.get("status")
                                        .in(statuses)
                        );
                    }

                    // =================================================
                    // FA03 - Tipo
                    // =================================================

                    if (type != null) {

                        ps.add(
                                cb.equal(
                                        root.get("type"),
                                        type
                                )
                        );
                    }

                    // =================================================
                    // FA05 - Prioridad
                    // =================================================

                    if (priority != null) {

                        ps.add(
                                cb.equal(
                                        root.get("priority"),
                                        priority
                                )
                        );
                    }

                    // =================================================
                    // FA06 - Categoría
                    // =================================================

                    if (category != null) {

                        ps.add(
                                cb.equal(
                                        root.get("category"),
                                        category
                                )
                        );
                    }

                    // =================================================
                    // FA02 - Fechas
                    // =================================================

                    if (f != null) {

                        ps.add(
                                cb.greaterThanOrEqualTo(
                                        root.get("createdAt"),
                                        f
                                )
                        );
                    }

                    if (t != null) {

                        ps.add(
                                cb.lessThanOrEqualTo(
                                        root.get("createdAt"),
                                        t
                                )
                        );
                    }

                    // =================================================
                    // FA01 - SOLO código
                    // =================================================

                    if (codigo != null) {

                        String like =
                                "%" +
                                        codigo
                                                .toLowerCase(Locale.ROOT) +
                                        "%";

                        ps.add(
                                cb.like(
                                        cb.lower(
                                                root.get("code")
                                        ),
                                        like
                                )
                        );
                    }

                    // =================================================
                    // FA07 / RN20 - SLA
                    // =================================================

                    if (
                            sla ==
                                    IndicadorSla.VENCIDO
                    ) {

                        ps.add(
                                cb.isTrue(
                                        root.get("slaBreached")
                                )
                        );

                    } else if (
                            sla ==
                                    IndicadorSla.PROXIMO_A_VENCER
                    ) {

                        ps.add(
                                cb.isTrue(
                                        root.get(
                                                "slaWarningSent"
                                        )
                                )
                        );

                        ps.add(
                                cb.isFalse(
                                        root.get(
                                                "slaBreached"
                                        )
                                )
                        );

                    } else if (
                            sla ==
                                    IndicadorSla.EN_TIEMPO
                    ) {

                        ps.add(
                                cb.isFalse(
                                        root.get(
                                                "slaWarningSent"
                                        )
                                )
                        );

                        ps.add(
                                cb.isFalse(
                                        root.get(
                                                "slaBreached"
                                        )
                                )
                        );
                    }

                    return cb.and(
                            ps.toArray(
                                    Predicate[]::new
                            )
                    );
                };

        int safeSize =
                Math.max(
                        5,
                        Math.min(size, 100)
                );

        Page<Caso> result =
                cases.findAll(
                        spec,
                        PageRequest.of(
                                Math.max(0, page),
                                safeSize,
                                Sort.by(
                                        Sort.Direction.DESC,
                                        "createdAt"
                                )
                        )
                );

        boolean revealRole =
                u.getRole().getCode() !=
                        CodigoRol.AGENTE_ATENCION;

        List<VistaCasoInterno> content =
                result
                        .getContent()
                        .stream()
                        .map(
                                c ->
                                        mapper.internalView(
                                                c,
                                                revealRole ||
                                                        !c.isConfidential()
                                        )
                        )
                        .toList();

        return new PaginaCasos(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
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
    public List<VistaAgenteAsignacion> availableAgents(
            Long caseId,
            String q,
            Long branchId,
            CategoriaCaso category,
            Boolean available,
            Integer maxOpenCases
    ) {

        // CU-05 / RN01 / RN21
        // Solo Supervisor o Administrador puede asignar responsables.
        requireSupervisorOrAdmin();

        Caso caso = requireAccessible(caseId);

        // CU-05 FA03:
        // No se permite asignar/reasignar en estados finales.
        if (Set.of(
                EstadoCaso.CERRADO,
                EstadoCaso.CANCELADO,
                EstadoCaso.RECHAZADO
        ).contains(caso.getStatus())) {

            throw new IllegalArgumentException(
                    "El caso se encuentra en estado "
                            + caso.getStatus()
                            + " y no permite esta operación."
            );
        }

        /*
         * Una asignación inicial solamente corresponde cuando el caso
         * está Pendiente de Asignación o Reabierto sin responsable.
         *
         * Si ya existe responsable, se trata como reasignación y RN09
         * permite gestionarla mientras el caso no esté en estado final.
         */
        if (caso.getResponsible() == null
                && !Set.of(
                EstadoCaso.PENDIENTE_ASIGNACION,
                EstadoCaso.REABIERTO
        ).contains(caso.getStatus())) {

            throw new IllegalArgumentException(
                    "El caso se encuentra en estado "
                            + caso.getStatus()
                            + " y no permite esta operación."
            );
        }

        if (maxOpenCases != null && maxOpenCases < 0) {
            throw new IllegalArgumentException(
                    "La cantidad de casos abiertos no puede ser negativa."
            );
        }

        String busqueda = blank(q);

        List<EstadoCaso> estadosFinales = List.of(
                EstadoCaso.CERRADO,
                EstadoCaso.CANCELADO,
                EstadoCaso.RECHAZADO
        );

        LocalDateTime ahora = LocalDateTime.now();

        return users.findActiveByRole(
                        CodigoRol.AGENTE_ATENCION,
                        EstadoRegistro.ACTIVO
                )
                .stream()

                // =====================================================
                // RN09
                // Debe poseer acceso por sucursal O categoría.
                // =====================================================
                .filter(usuario -> {

                    boolean mismaSucursal =
                            usuario.getBranch() != null
                                    && Objects.equals(
                                    usuario.getBranch().getId(),
                                    caso.getBranch().getId()
                            );

                    boolean categoriaAutorizada =
                            usuario.getAuthorizedCategories() != null
                                    && usuario
                                    .getAuthorizedCategories()
                                    .contains(caso.getCategory());

                    return mismaSucursal || categoriaAutorizada;
                })

                // =====================================================
                // FA04
                // Nombre completo, parte del nombre o username.
                // =====================================================
                .filter(usuario -> {

                    if (busqueda == null) {
                        return true;
                    }

                    String valor =
                            busqueda.toLowerCase(Locale.ROOT);

                    return usuario
                            .getFullName()
                            .toLowerCase(Locale.ROOT)
                            .contains(valor)
                            ||
                            usuario
                                    .getUsername()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(valor);
                })

                // =====================================================
                // FA05 - Sucursal
                // =====================================================
                .filter(usuario ->
                        branchId == null
                                ||
                                (
                                        usuario.getBranch() != null
                                                && Objects.equals(
                                                usuario.getBranch().getId(),
                                                branchId
                                        )
                                )
                )

                // =====================================================
                // FA05 - Categoría / especialidad
                // =====================================================
                .filter(usuario ->
                        category == null
                                ||
                                (
                                        usuario.getAuthorizedCategories() != null
                                                && usuario
                                                .getAuthorizedCategories()
                                                .contains(category)
                                )
                )

                .map(usuario -> {

                    long abiertos =
                            cases.countByResponsibleIdAndStatusNotIn(
                                    usuario.getId(),
                                    estadosFinales
                            );

                    long vencidos =
                            cases.countByResponsibleIdAndSlaBreachedTrueAndStatusNotIn(
                                    usuario.getId(),
                                    estadosFinales
                            );

                    /*
                     * Disponibilidad CU-05:
                     * - usuario marcado como disponible;
                     * - cuenta activa;
                     * - cuenta no bloqueada en este momento.
                     */
                    boolean noBloqueado =
                            usuario.getLockedUntil() == null
                                    || !usuario.getLockedUntil().isAfter(ahora);

                    boolean disponible =
                            usuario.isAvailableForAssignment()
                                    && usuario.getStatus() == EstadoRegistro.ACTIVO
                                    && noBloqueado;

                    List<CategoriaCaso> categorias =
                            usuario.getAuthorizedCategories() == null
                                    ? List.of()
                                    : usuario
                                    .getAuthorizedCategories()
                                    .stream()
                                    .sorted(
                                            Comparator.comparing(Enum::name)
                                    )
                                    .toList();

                    return new VistaAgenteAsignacion(
                            usuario.getId(),
                            usuario.getFullName(),
                            usuario.getUsername(),

                            usuario.getBranch() == null
                                    ? null
                                    : usuario.getBranch().getId(),

                            usuario.getBranch() == null
                                    ? "Sin sucursal"
                                    : usuario.getBranch().getName(),

                            categorias,
                            abiertos,
                            vencidos,
                            disponible
                    );
                })

                // =====================================================
                // FA05 - Disponibilidad
                // =====================================================
                .filter(agente ->
                        available == null
                                || agente.available() == available
                )

                // =====================================================
                // FA05 - Cantidad de casos abiertos
                // =====================================================
                .filter(agente ->
                        maxOpenCases == null
                                || agente.openCases() <= maxOpenCases
                )

                /*
                 * FA18:
                 * La lista favorece agentes disponibles y menor carga.
                 * Esto también ayuda a un caso de prioridad Crítica.
                 */
                .sorted(
                        Comparator
                                .comparing(
                                        VistaAgenteAsignacion::available
                                )
                                .reversed()
                                .thenComparingLong(
                                        VistaAgenteAsignacion::overdueCases
                                )
                                .thenComparingLong(
                                        VistaAgenteAsignacion::openCases
                                )
                                .thenComparing(
                                        VistaAgenteAsignacion::fullName,
                                        String.CASE_INSENSITIVE_ORDER
                                )
                )
                .toList();
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

    @Transactional(readOnly = true)
    public List<Map<String, Object>>
    responsablesDisponiblesFiltro() {

        Usuario usuario =
                current.require();

        CodigoRol rol =
                usuario.getRole().getCode();

        /*
         * AGENTE:
         * Solo puede consultar sus propios casos,
         * por lo tanto solamente se muestra él.
         */
        if (
                rol ==
                        CodigoRol.AGENTE_ATENCION
        ) {

            return List.of(
                    Map.of(
                            "id",
                            usuario.getId(),
                            "name",
                            usuario.getFullName()
                    )
            );
        }

        /*
         * SUPERVISOR:
         * únicamente agentes activos
         * de su sucursal.
         */
        if (
                rol ==
                        CodigoRol.SUPERVISOR
        ) {

            if (
                    usuario.getBranch() == null
            ) {

                return List.of();
            }

            return users
                    .findActiveByRoleAndBranch(
                            CodigoRol.AGENTE_ATENCION,
                            EstadoRegistro.ACTIVO,
                            usuario
                                    .getBranch()
                                    .getId()
                    )
                    .stream()
                    .map(
                            agente ->
                                    Map.<String, Object>of(
                                            "id",
                                            agente.getId(),
                                            "name",
                                            agente.getFullName()
                                    )
                    )
                    .toList();
        }

        /*
         * ADMINISTRADOR:
         * todos los agentes activos.
         *
         * Si RepositorioUsuario extiende JpaRepository,
         * findAll() ya está disponible.
         */
        return users
                .findAll()
                .stream()
                .filter(
                        agente ->
                                agente.getStatus() ==
                                        EstadoRegistro.ACTIVO
                )
                .filter(
                        agente ->
                                agente.getRole()
                                        .getCode() ==
                                        CodigoRol.AGENTE_ATENCION
                )
                .map(
                        agente ->
                                Map.<String, Object>of(
                                        "id",
                                        agente.getId(),
                                        "name",
                                        agente.getFullName()
                                )
                )
                .toList();
    }
    @Transactional(readOnly = true)
    public List<Map<String, Object>>
    sucursalesDisponiblesFiltro() {

        Usuario usuario =
                current.require();

        CodigoRol rol =
                usuario.getRole().getCode();

        if (
                rol ==
                        CodigoRol.AGENTE_ATENCION ||
                        rol ==
                                CodigoRol.SUPERVISOR
        ) {

            if (
                    usuario.getBranch() == null
            ) {

                return List.of();
            }

            Sucursal sucursal =
                    usuario.getBranch();

            return List.of(
                    Map.of(
                            "id",
                            sucursal.getId(),
                            "name",
                            sucursal.getName()
                    )
            );
        }

        return branchRepository
                .findAll()
                .stream()
                .filter(
                        sucursal ->
                                sucursal.getStatus() ==
                                        EstadoRegistro.ACTIVO
                )
                .map(
                        sucursal ->
                                Map.<String, Object>of(
                                        "id",
                                        sucursal.getId(),
                                        "name",
                                        sucursal.getName()
                                )
                )
                .toList();
    }

    @Transactional
    public VistaCasoInterno assign(
            Long id,
            SolicitudAsignacion r,
            HttpServletRequest req
    ) {

        requireSupervisorOrAdmin();

        Caso c = requireAccessible(id);

        // =====================================================
        // FA12 - CONCURRENCIA
        // =====================================================
        if (c.getVersion() != r.version()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La operación fue rechazada porque la información cambió durante el proceso."
            );
        }

        // =====================================================
        // FA03 - ESTADO DEL CASO
        // =====================================================
        if (Set.of(
                EstadoCaso.CERRADO,
                EstadoCaso.CANCELADO,
                EstadoCaso.RECHAZADO
        ).contains(c.getStatus())) {

            throw new IllegalArgumentException(
                    "El caso se encuentra en estado "
                            + c.getStatus()
                            + " y no permite esta operación."
            );
        }

        Usuario old = c.getResponsible();

        /*
         * Asignación inicial:
         * Pendiente de Asignación o Reabierto sin responsable.
         *
         * Si ya existe responsable, la operación es una reasignación.
         */
        if (old == null
                && !Set.of(
                EstadoCaso.PENDIENTE_ASIGNACION,
                EstadoCaso.REABIERTO
        ).contains(c.getStatus())) {

            throw new IllegalArgumentException(
                    "El caso se encuentra en estado "
                            + c.getStatus()
                            + " y no permite esta operación."
            );
        }

        Usuario agent =
                users.findById(r.responsibleId())
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "El responsable seleccionado no existe."
                                )
                        );

        // =====================================================
        // FA09 / RN09 / RN15
        // ACTIVO, ROL CORRECTO Y NO BLOQUEADO
        // =====================================================
        if (agent.getStatus() != EstadoRegistro.ACTIVO
                || agent.getRole().getCode() != CodigoRol.AGENTE_ATENCION) {

            throw new IllegalArgumentException(
                    "El responsable seleccionado no se encuentra activo."
            );
        }

        boolean bloqueado =
                agent.getLockedUntil() != null
                        && agent
                        .getLockedUntil()
                        .isAfter(LocalDateTime.now());

        if (bloqueado) {

            throw new IllegalArgumentException(
                    "El responsable seleccionado no se encuentra activo."
            );
        }

        if (!agent.isAvailableForAssignment()) {

            throw new IllegalArgumentException(
                    "El responsable seleccionado no se encuentra disponible."
            );
        }

        // =====================================================
        // RN09 / FA10
        // ACCESO POR SUCURSAL O CATEGORÍA
        // =====================================================
        boolean accesoSucursal =
                agent.getBranch() != null
                        && Objects.equals(
                        agent.getBranch().getId(),
                        c.getBranch().getId()
                );

        boolean accesoCategoria =
                agent.getAuthorizedCategories() != null
                        && agent
                        .getAuthorizedCategories()
                        .contains(c.getCategory());

        if (!accesoSucursal && !accesoCategoria) {

            throw new IllegalArgumentException(
                    "El responsable seleccionado no posee acceso a la sucursal o categoría del caso."
            );
        }

        // =====================================================
        // FA15 - MISMO RESPONSABLE
        // =====================================================
        if (old != null
                && Objects.equals(
                old.getId(),
                agent.getId()
        )) {

            throw new IllegalArgumentException(
                    "Seleccione un responsable diferente al actual"
            );
        }

        boolean reassignment = old != null;

        // =====================================================
        // FA16 - MOTIVO OBLIGATORIO EN REASIGNACIÓN
        // =====================================================
        if (reassignment
                && (
                r.reason() == null
                        || r.reason().isBlank()
        )) {

            throw new IllegalArgumentException(
                    "Complete todos los campos obligatorios."
            );
        }

        Usuario ejecutor = current.require();

        EstadoCaso oldStatus = c.getStatus();

        // =====================================================
        // REGISTRO DE LA ASIGNACIÓN
        // =====================================================
        c.setResponsible(agent);
        c.setAssignedAt(LocalDateTime.now());
        c.setAssignedBy(ejecutor);
        c.setAssignmentObservation(blank(r.reason()));

        // =====================================================
        // RN10
        // Asignación inicial cambia a ASIGNADO.
        // =====================================================
        if (old == null
                && (
                oldStatus == EstadoCaso.PENDIENTE_ASIGNACION
                        || oldStatus == EstadoCaso.REABIERTO
        )) {

            rules.requireTransition(
                    oldStatus,
                    EstadoCaso.ASIGNADO
            );

            c.setStatus(
                    EstadoCaso.ASIGNADO
            );
        }

        cases.save(c);

        String action =
                reassignment
                        ? "REASIGNACION"
                        : "ASIGNACION";

        Map<String, Object> anterior =
                old == null
                        ? null
                        : Map.of(
                        "responsable",
                        old.getUsername()
                );

        Map<String, Object> nuevo =
                new LinkedHashMap<>();

        nuevo.put(
                "responsable",
                agent.getUsername()
        );

        nuevo.put(
                "estado",
                c.getStatus().name()
        );

        nuevo.put(
                "fechaAsignacion",
                c.getAssignedAt()
        );

        nuevo.put(
                "asignadoPor",
                ejecutor.getUsername()
        );

        if (blank(r.reason()) != null) {
            nuevo.put(
                    "observacion",
                    blank(r.reason())
            );
        }

        // =====================================================
        // RN18 / RN19 - BITÁCORA
        // =====================================================
        audit.log(
                req,
                "CASOS",
                action,
                "CASO",
                c.getCode(),

                reassignment
                        ? "Caso reasignado. Motivo: " + r.reason()
                        : "Caso asignado a " + agent.getUsername(),

                ResultadoAuditoria.EXITOSO,
                anterior,
                nuevo
        );

        // =====================================================
        // RN14 - NUEVO RESPONSABLE
        // =====================================================
        notifications.internal(
                reassignment
                        ? EventoNotificacion.REASIGNACION
                        : EventoNotificacion.ASIGNACION,
                c,
                agent,
                "Se le asignó el caso " + c.getCode()
        );

        if (agent.getEmail() != null
                && !agent.getEmail().isBlank()) {

            notifications.email(
                    reassignment
                            ? EventoNotificacion.REASIGNACION
                            : EventoNotificacion.ASIGNACION,
                    c,
                    agent.getEmail(),
                    "Se le asignó el caso " + c.getCode()
            );
        }

        // =====================================================
        // FA14 - RESPONSABLE ANTERIOR
        // =====================================================
        if (reassignment && old != null) {

            notifications.internal(
                    EventoNotificacion.REASIGNACION,
                    c,
                    old,
                    "El caso "
                            + c.getCode()
                            + " fue reasignado."
            );

            if (old.getEmail() != null
                    && !old.getEmail().isBlank()) {

                notifications.email(
                        EventoNotificacion.REASIGNACION,
                        c,
                        old.getEmail(),
                        "El caso "
                                + c.getCode()
                                + " fue reasignado."
                );
            }
        }

        return mapper.internalView(
                c,
                true
        );
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

    // =========================================================
    // CU-07 - CANTIDAD DISPONIBLE
    // =========================================================

    @Transactional(readOnly = true)
    public Map<String, Object> evidenceAvailability(
            Long id,
            TipoAsociacionEvidencia associationType,
            Long followUpId
    ) {
        Caso c = requireAccessible(id);
        validarEstadoParaEvidencia(c);

        Seguimiento seguimiento = resolverSeguimientoEvidencia(c, associationType, followUpId);

        long usados = seguimiento == null
                ? evidenceRepository.countByComplaintCaseIdAndFollowUpIsNullAndStatus(
                c.getId(), EstadoEvidencia.ACTIVA)
                : evidenceRepository.countByFollowUpIdAndStatus(
                seguimiento.getId(), EstadoEvidencia.ACTIVA);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("limit", 5);
        respuesta.put("used", usados);
        respuesta.put("available", Math.max(0, 5 - usados));
        respuesta.put("visibilityEditable", seguimiento == null);
        respuesta.put("visibleToClient", seguimiento != null && seguimiento.isVisibleToClient());

        return respuesta;
    }

    // =========================================================
    // CU-07 - ADJUNTAR EVIDENCIA
    // =========================================================

    @Transactional
    public VistaEvidencia addEvidence(
            Long id,
            TipoAsociacionEvidencia associationType,
            Long followUpId,
            String description,
            boolean visibleToClient,
            long version,
            MultipartFile file,
            HttpServletRequest req
    ) {
        Caso c = requireAccessible(id);

        // FA05 / FA17 - información cambió durante el proceso
        if (c.getVersion() != version) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La operación fue rechazada porque la información cambió durante el proceso."
            );
        }

        validarEstadoParaEvidencia(c);

        Usuario u = current.require();

        if (u.getRole().getCode() == CodigoRol.AGENTE_ATENCION
                && (c.getResponsible() == null
                || !Objects.equals(c.getResponsible().getId(), u.getId()))) {
            throw new IllegalArgumentException(
                    "No posee permisos para realizar esta acción."
            );
        }

        String descripcion = blank(description);

        if (descripcion == null) {
            throw new IllegalArgumentException(
                    "Complete todos los campos obligatorios."
            );
        }

        Seguimiento seguimiento = resolverSeguimientoEvidencia(
                c,
                associationType,
                followUpId
        );

        // FA08 - si pertenece a seguimiento, hereda su visibilidad.
        boolean visibilidadFinal = seguimiento != null
                ? seguimiento.isVisibleToClient()
                : visibleToClient;

        List<Evidencia> guardadas = evidence.store(
                c,
                seguimiento,
                List.of(file),
                visibilidadFinal,
                descripcion,
                req
        );

        if (guardadas.isEmpty()) {
            throw new IllegalArgumentException(
                    "No fue posible almacenar el archivo."
            );
        }

        Evidencia e = guardadas.get(0);

        if (visibilidadFinal
                && c.getEmail() != null
                && !c.getEmail().isBlank()) {
            notifications.email(
                    EventoNotificacion.SEGUIMIENTO_VISIBLE,
                    c,
                    c.getEmail(),
                    "Se adjuntó una nueva evidencia visible a su caso."
            );
        }

        return mapper.evidence(e);
    }

    private void validarEstadoParaEvidencia(Caso c) {
        if (c.getStatus() == EstadoCaso.CERRADO) {
            throw new IllegalArgumentException(
                    "El caso se encuentra cerrado y no puede modificarse."
            );
        }

        if (Set.of(EstadoCaso.RECHAZADO, EstadoCaso.CANCELADO)
                .contains(c.getStatus())) {
            throw new IllegalArgumentException(
                    "El caso se encuentra en estado "
                            + c.getStatus()
                            + " y no permite esta operación."
            );
        }
    }

    private Seguimiento resolverSeguimientoEvidencia(
            Caso c,
            TipoAsociacionEvidencia associationType,
            Long followUpId
    ) {
        if (associationType == null) {
            throw new IllegalArgumentException(
                    "Complete todos los campos obligatorios."
            );
        }

        if (associationType == TipoAsociacionEvidencia.CASO) {
            return null;
        }

        if (followUpId == null) {
            throw new IllegalArgumentException(
                    "Complete todos los campos obligatorios."
            );
        }

        Seguimiento seguimiento = followUps.findById(followUpId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existen seguimientos disponibles para asociar la evidencia"
                ));

        if (seguimiento.getComplaintCase() == null
                || !Objects.equals(
                seguimiento.getComplaintCase().getId(),
                c.getId()
        )) {
            throw new IllegalArgumentException(
                    "No existen seguimientos disponibles para asociar la evidencia"
            );
        }

        return seguimiento;
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
