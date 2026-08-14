package com.umg.sgq.controlador;

import com.umg.sgq.entidad.RegistroAuditoria;
import com.umg.sgq.enumeracion.ResultadoAuditoria;
import com.umg.sgq.repositorio.RepositorioRegistroAuditoria;
import com.umg.sgq.servicio.ServicioAuditoria;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("@authz.has('AUDIT_VIEW')")
public class ControladorAuditoria {

    private final RepositorioRegistroAuditoria repositorio;
    private final ServicioAuditoria servicioAuditoria;

    public ControladorAuditoria(
            RepositorioRegistroAuditoria repositorio,
            ServicioAuditoria servicioAuditoria
    ) {
        this.repositorio = repositorio;
        this.servicioAuditoria = servicioAuditoria;
    }

    // =========================================================
    // CONSULTAR BITÁCORA
    // =========================================================

    @GetMapping
    public Map<String, Object> buscar(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String ip,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            HttpServletRequest peticion
    ) {

        LocalDate fechaDesde =
                from == null
                        ? LocalDate.now().minusDays(30)
                        : from;

        LocalDate fechaHasta =
                to == null
                        ? LocalDate.now()
                        : to;

        // Validar fechas
        if (fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final."
            );
        }

        // Máximo 90 días
        if (fechaDesde.plusDays(90).isBefore(fechaHasta)) {
            throw new IllegalArgumentException(
                    "El rango máximo para consulta interactiva es de 90 días."
            );
        }

        // Limitar cantidad de resultados por página
        size = Math.max(1, Math.min(size, 100));
        page = Math.max(0, page);

        LocalDateTime fechaHoraDesde =
                fechaDesde.atStartOfDay();

        LocalDateTime fechaHoraHasta =
                fechaHasta
                        .plusDays(1)
                        .atStartOfDay()
                        .minusNanos(1);

        // =====================================================
        // FILTRO BASE: FECHAS
        // =====================================================

        Specification<RegistroAuditoria> filtros =
                (root, query, cb) ->
                        cb.between(
                                root.get("createdAt"),
                                fechaHoraDesde,
                                fechaHoraHasta
                        );

        // =====================================================
        // FILTRO POR USUARIO
        // =====================================================

        if (username != null && !username.isBlank()) {

            String usuarioBuscado =
                    "%" +
                            username
                                    .trim()
                                    .toLowerCase(Locale.ROOT)
                            + "%";

            filtros = filtros.and(
                    (root, query, cb) ->
                            cb.like(
                                    cb.lower(root.get("username")),
                                    usuarioBuscado
                            )
            );
        }

        // =====================================================
        // FILTRO POR MÓDULO
        // =====================================================

        if (module != null && !module.isBlank()) {

            String moduloBuscado = module.trim();

            filtros = filtros.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("module"),
                                    moduloBuscado
                            )
            );
        }

        // =====================================================
        // FILTRO POR ACCIÓN
        // =====================================================

        if (action != null && !action.isBlank()) {

            String accionBuscada = action.trim();

            filtros = filtros.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("action"),
                                    accionBuscada
                            )
            );
        }

        // =====================================================
        // FILTRO POR IP
        // =====================================================

        if (ip != null && !ip.isBlank()) {

            String ipBuscada = ip.trim();

            filtros = filtros.and(
                    (root, query, cb) ->
                            cb.equal(
                                    root.get("ipAddress"),
                                    ipBuscada
                            )
            );
        }

        // =====================================================
        // CONSULTAR BASE DE DATOS
        // =====================================================

        Page<RegistroAuditoria> datos =
                repositorio.findAll(
                        filtros,
                        PageRequest.of(page, size)
                );

        // Registrar la consulta en la propia bitácora
        servicioAuditoria.log(
                peticion,
                "BITACORA",
                "CONSULTA",
                "BITACORA",
                null,
                "Consulta de bitácora de auditoría",
                ResultadoAuditoria.EXITOSO,
                null,
                Map.of(
                        "desde", fechaDesde.toString(),
                        "hasta", fechaHasta.toString()
                )
        );

        // =====================================================
        // RESPUESTA PARA ANGULAR
        // =====================================================

        Map<String, Object> respuesta =
                new LinkedHashMap<>();

        respuesta.put(
                "content",
                datos.getContent()
                        .stream()
                        .map(this::convertirVista)
                        .toList()
        );

        respuesta.put("page", datos.getNumber());

        respuesta.put("size", datos.getSize());

        respuesta.put(
                "totalElements",
                datos.getTotalElements()
        );

        respuesta.put(
                "totalPages",
                datos.getTotalPages()
        );

        return respuesta;
    }

    // =========================================================
    // VER DETALLE DE UN REGISTRO
    // =========================================================

    @GetMapping("/{id}")
    public Map<String, Object> detalle(
            @PathVariable Long id,
            HttpServletRequest peticion
    ) {

        RegistroAuditoria registro =
                repositorio.findById(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Evento no encontrado."
                                )
                        );

        servicioAuditoria.log(
                peticion,
                "BITACORA",
                "CONSULTA_DETALLE",
                "BITACORA",
                id.toString(),
                "Consulta de detalle de evento de auditoría",
                ResultadoAuditoria.EXITOSO,
                null,
                null
        );

        return convertirVista(registro);
    }

    // =========================================================
    // VERIFICAR INTEGRIDAD DE LA BITÁCORA
    // =========================================================

    @GetMapping("/integrity")
    public Map<String, Object> verificarIntegridad() {

        return servicioAuditoria.verifyChain();
    }

    // =========================================================
    // CONVERTIR ENTIDAD A RESPUESTA
    // =========================================================

    private Map<String, Object> convertirVista(
            RegistroAuditoria registro
    ) {

        Map<String, Object> datos =
                new LinkedHashMap<>();

        datos.put("id", registro.getId());

        datos.put(
                "createdAt",
                registro.getCreatedAt()
        );

        datos.put(
                "username",
                registro.getUsername()
        );

        datos.put(
                "role",
                registro.getRole()
        );

        datos.put(
                "ipAddress",
                registro.getIpAddress()
        );

        datos.put(
                "module",
                registro.getModule()
        );

        datos.put(
                "action",
                registro.getAction()
        );

        datos.put(
                "entityType",
                registro.getEntityType()
        );

        datos.put(
                "entityReference",
                registro.getEntityReference()
        );

        datos.put(
                "description",
                registro.getDescription()
        );

        datos.put(
                "result",
                registro.getResult()
        );

        datos.put(
                "oldValues",
                registro.getOldValues()
        );

        datos.put(
                "newValues",
                registro.getNewValues()
        );

        datos.put(
                "integrityHash",
                registro.getIntegrityHash()
        );

        return datos;
    }
}