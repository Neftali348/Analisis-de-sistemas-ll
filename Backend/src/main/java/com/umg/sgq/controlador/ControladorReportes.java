package com.umg.sgq.controlador;

import com.umg.sgq.dto.DtosReportes.*;
import com.umg.sgq.servicio.ServicioReportes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reports")
public class ControladorReportes {
    private final ServicioReportes service;

    public ControladorReportes(ServicioReportes service) {
        this.service = service;
    }

    @PostMapping("/preview")
    @PreAuthorize("@authz.has('REPORT_VIEW')")
    public VistaPreviaReporte preview(@Valid @RequestBody FiltroReporte f, HttpServletRequest req) {
        return service.preview(f, req);
    }

    @PostMapping("/export/{format}")
    @PreAuthorize("@authz.has('REPORT_EXPORT')")
    public ResponseEntity<byte[]> export(@PathVariable String format, @Valid @RequestBody FiltroReporte f, HttpServletRequest req) {
        var e = service.export(f, format, req);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(e.contentType())).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(e.filename(), StandardCharsets.UTF_8).build().toString()).body(e.bytes());
    }
}
