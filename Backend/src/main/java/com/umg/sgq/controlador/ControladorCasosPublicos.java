package com.umg.sgq.controlador;

import com.umg.sgq.dto.DtosCasos.*;
import com.umg.sgq.entidad.Sucursal;
import com.umg.sgq.enumeracion.EstadoRegistro;
import com.umg.sgq.repositorio.RepositorioSucursal;
import com.umg.sgq.servicio.ServicioCasosPublicos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;

import java.nio.charset.StandardCharsets;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/public")
public class ControladorCasosPublicos {
    private final ServicioCasosPublicos service;
    private final RepositorioSucursal branches;

    public ControladorCasosPublicos(ServicioCasosPublicos service, RepositorioSucursal branches) {
        this.service = service;
        this.branches = branches;
    }

    @GetMapping("/branches")
    public List<Map<String, Object>> branches() {
        return branches.findByStatusOrderByName(EstadoRegistro.ACTIVO).stream().map(b -> Map.<String, Object>of("id", b.getId(), "code", b.getCode(), "name", b.getName(), "address", b.getAddress())).toList();
    }

    @PostMapping(value = "/cases", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RespuestaCreacionPublica register(@Valid @RequestPart("data") SolicitudCreacionPublica data, @RequestPart(value = "files", required = false) List<MultipartFile> files, HttpServletRequest req) {
        return service.register(data, files, req);
    }

    @PostMapping("/cases/lookup")
    public VistaCasoPublico lookup(@Valid @RequestBody SolicitudConsultaPublica r, HttpServletRequest req) {
        return service.lookup(r, req);
    }

    @PostMapping(value = "/cases/{code}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VistaEvidencia evidence(@PathVariable String code, @RequestParam(required = false) String email, @RequestParam(required = false) String trackingKey, @RequestPart("file") MultipartFile file, HttpServletRequest req) {
        return service.addEvidence(code, email, trackingKey, file, req);
    }

    @PostMapping(value = "/cases/{code}/response", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VistaCasoPublico respond(@PathVariable String code, @RequestParam(required = false) String email, @RequestParam(required = false) String trackingKey, @Valid @RequestPart("data") SolicitudRespuestaPublica data, @RequestPart(value = "files", required = false) List<MultipartFile> files, HttpServletRequest req) {
        return service.respond(code, email, trackingKey, data, files, req);
    }

    @PostMapping("/cases/{code}/cancel")
    public VistaCasoPublico cancel(@PathVariable String code, @RequestParam(required = false) String email, @RequestParam(required = false) String trackingKey, @Valid @RequestBody SolicitudMotivoPublico r, HttpServletRequest req) {
        return service.cancel(code, email, trackingKey, r, req);
    }

    @PostMapping("/cases/{code}/reopen-request")
    public Map<String, String> reopenRequest(@PathVariable String code, @RequestParam(required = false) String email, @RequestParam(required = false) String trackingKey, @Valid @RequestBody SolicitudMotivoPublico r, HttpServletRequest req) {
        service.requestReopen(code, email, trackingKey, r, req);
        return Map.of("message", "Solicitud de reapertura registrada para revisión.");
    }

    @GetMapping("/cases/{code}/evidences/{evidenceId}/download")
    public ResponseEntity<?> download(@PathVariable String code, @PathVariable Long evidenceId, @RequestParam(required = false) String email, @RequestParam(required = false) String trackingKey, HttpServletRequest req) {
        var d = service.download(code, evidenceId, email, trackingKey, req);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType())).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.filename(), StandardCharsets.UTF_8).build().toString()).body(d.resource());
    }

    @PostMapping("/cases/{code}/satisfaction")
    public Map<String, String> satisfaction(@PathVariable String code, @RequestParam(required = false) String email, @RequestParam(required = false) String trackingKey, @Valid @RequestBody SolicitudSatisfaccion r, HttpServletRequest req) {
        service.rate(code, email, trackingKey, r, req);
        return Map.of("message", "Gracias por calificar la atención.");
    }
}
