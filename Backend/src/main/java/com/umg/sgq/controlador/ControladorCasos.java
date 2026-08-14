package com.umg.sgq.controlador;

import com.umg.sgq.dto.DtosCasos.*;
import com.umg.sgq.enumeracion.*;

import java.time.LocalDate;

import com.umg.sgq.servicio.ServicioCasos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/cases")
public class ControladorCasos {
    private final ServicioCasos service;

    public ControladorCasos(ServicioCasos service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("@authz.has('CASE_VIEW')")
    public PaginaCasos list(@RequestParam(required = false) List<EstadoCaso> status, @RequestParam(required = false) String q, @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, @RequestParam(required = false) TipoCaso type, @RequestParam(required = false) Prioridad priority, @RequestParam(required = false) Long branchId, @RequestParam(required = false) CategoriaCaso category, @RequestParam(required = false) Long responsibleId, @RequestParam(required = false) IndicadorSla sla, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.search(status, q, from, to, type, priority, branchId, category, responsibleId, sla, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('CASE_VIEW')")
    public VistaCasoInterno detail(@PathVariable Long id, HttpServletRequest req) {
        return service.detail(id, req);
    }

    @GetMapping("/{id}/agents")
    @PreAuthorize("@authz.has('CASE_ASSIGN')")
    public List<Map<String, Object>> agents(@PathVariable Long id) {
        return service.availableAgents(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('CASE_UPDATE')")
    public VistaCasoInterno edit(@PathVariable Long id, @Valid @RequestBody SolicitudEdicionCaso r, HttpServletRequest req) {
        return service.edit(id, r, req);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("@authz.has('CASE_ASSIGN')")
    public VistaCasoInterno assign(@PathVariable Long id, @Valid @RequestBody SolicitudAsignacion r, HttpServletRequest req) {
        return service.assign(id, r, req);
    }

    @PostMapping(value = "/{id}/follow-ups", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.has('CASE_FOLLOWUP')")
    public VistaCasoInterno follow(@PathVariable Long id, @Valid @RequestPart("data") SolicitudSeguimiento r, @RequestPart(value = "files", required = false) List<MultipartFile> files, HttpServletRequest req) {
        return service.followUp(id, r, files, req);
    }

    @PostMapping(value = "/{id}/evidences", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.has('EVIDENCE_UPLOAD')")
    public VistaEvidencia evidence(@PathVariable Long id, @RequestParam String description, @RequestParam(defaultValue = "false") boolean visibleToClient, @RequestPart("file") MultipartFile file, HttpServletRequest req) {
        return service.addEvidence(id, description, visibleToClient, file, req);
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("@authz.has('CASE_RESOLVE')")
    public VistaCasoInterno resolve(@PathVariable Long id, @Valid @RequestBody SolicitudResolucion r, HttpServletRequest req) {
        return service.resolve(id, r, req);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@authz.has('CASE_CLOSE')")
    public VistaCasoInterno close(@PathVariable Long id, @Valid @RequestBody SolicitudCierre r, HttpServletRequest req) {
        return service.close(id, r, req);
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("@authz.has('CASE_REOPEN')")
    public VistaCasoInterno reopen(@PathVariable Long id, @Valid @RequestBody SolicitudReapertura r, HttpServletRequest req) {
        return service.reopen(id, r, req);
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("@authz.has('CASE_PRIORITY')")
    public VistaCasoInterno priority(@PathVariable Long id, @Valid @RequestBody SolicitudPrioridad r, HttpServletRequest req) {
        return service.priority(id, r, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@authz.has('CASE_UPDATE')")
    public VistaCasoInterno status(@PathVariable Long id, @Valid @RequestBody SolicitudEstado r, HttpServletRequest req) {
        return service.status(id, r, req);
    }
}
