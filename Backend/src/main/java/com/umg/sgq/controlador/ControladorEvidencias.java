package com.umg.sgq.controlador;

import com.umg.sgq.servicio.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/evidences")
public class ControladorEvidencias {
    private final ServicioEvidencias service;
    private final ServicioCasos cases;

    public ControladorEvidencias(ServicioEvidencias service, ServicioCasos cases) {
        this.service = service;
        this.cases = cases;
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("@authz.has('EVIDENCE_DOWNLOAD')")
    public ResponseEntity<?> download(@PathVariable Long id, HttpServletRequest req) {
        var e = service.require(id);
        cases.requireAccessible(e.getComplaintCase().getId());
        var d = service.download(id, req);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType())).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.filename(), StandardCharsets.UTF_8).build().toString()).body(d.resource());
    }

    @PostMapping("/{id}/annul")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('SUPERVISOR')")
    public void annul(@PathVariable Long id, @RequestBody java.util.Map<String, String> body, HttpServletRequest req) {
        var e = service.require(id);
        cases.requireAccessible(e.getComplaintCase().getId());
        service.annul(id, body.get("reason"), req);
    }
}
