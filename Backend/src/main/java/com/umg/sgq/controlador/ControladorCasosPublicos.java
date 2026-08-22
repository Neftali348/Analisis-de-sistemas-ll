package com.umg.sgq.controlador;

import com.umg.sgq.dto.DtosCasos.*;
import com.umg.sgq.enumeracion.EstadoRegistro;
import com.umg.sgq.repositorio.RepositorioSucursal;
import com.umg.sgq.servicio.ServicioCasosPublicos;
import com.umg.sgq.servicio.ServicioVerificacionPublica;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class ControladorCasosPublicos {

    private final ServicioCasosPublicos service;
    private final RepositorioSucursal branches;
    private final ServicioVerificacionPublica verificacionPublica;

    public ControladorCasosPublicos(
            ServicioCasosPublicos service,
            RepositorioSucursal branches,
            ServicioVerificacionPublica verificacionPublica
    ) {
        this.service = service;
        this.branches = branches;
        this.verificacionPublica = verificacionPublica;
    }

    // =========================================================
    // SUCURSALES PÚBLICAS
    // =========================================================

    @GetMapping("/branches")
    public List<Map<String, Object>> branches() {

        return branches
                .findByStatusOrderByName(
                        EstadoRegistro.ACTIVO
                )
                .stream()
                .map(
                        branch -> Map.<String, Object>of(
                                "id",
                                branch.getId(),

                                "code",
                                branch.getCode(),

                                "name",
                                branch.getName(),

                                "address",
                                branch.getAddress()
                        )
                )
                .toList();
    }

    // =========================================================
    // CU-03
    // GENERAR VERIFICACIÓN DE SEGURIDAD
    // =========================================================

    @GetMapping("/cases/security-challenge")
    public Map<String, String> securityChallenge() {

        return verificacionPublica.crearDesafio();
    }

    // =========================================================
    // REGISTRAR CASO
    // =========================================================

    @PostMapping(
            value = "/cases",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public RespuestaCreacionPublica register(
            @Valid
            @RequestPart("data")
            SolicitudCreacionPublica data,

            @RequestPart(
                    value = "files",
                    required = false
            )
            List<MultipartFile> files,

            HttpServletRequest req
    ) {

        return service.register(
                data,
                files,
                req
        );
    }

    // =========================================================
    // CU-03
    // CONSULTAR ESTADO DEL CASO
    // =========================================================

    @PostMapping("/cases/lookup")
    public VistaCasoPublico lookup(
            @Valid
            @RequestBody
            SolicitudConsultaPublica r,

            HttpServletRequest req
    ) {

        return service.lookup(
                r,
                req
        );
    }

    // =========================================================
    // AGREGAR EVIDENCIA
    // =========================================================

    @PostMapping(
            value = "/cases/{code}/evidence",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public VistaEvidencia evidence(
            @PathVariable
            String code,

            @RequestParam(
                    required = false
            )
            String email,

            @RequestParam(
                    required = false
            )
            String trackingKey,

            @RequestPart("file")
            MultipartFile file,

            HttpServletRequest req
    ) {

        return service.addEvidence(
                code,
                email,
                trackingKey,
                file,
                req
        );
    }

    // =========================================================
    // CU-03 FA11
    // RESPONDER SOLICITUD
    // =========================================================

    @PostMapping(
            value = "/cases/{code}/response",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public VistaCasoPublico respond(
            @PathVariable
            String code,

            @RequestParam(
                    required = false
            )
            String email,

            @RequestParam(
                    required = false
            )
            String trackingKey,

            @Valid
            @RequestPart("data")
            SolicitudRespuestaPublica data,

            @RequestPart(
                    value = "files",
                    required = false
            )
            List<MultipartFile> files,

            HttpServletRequest req
    ) {

        return service.respond(
                code,
                email,
                trackingKey,
                data,
                files,
                req
        );
    }

    // =========================================================
    // CU-03 FA10
    // CANCELAR CASO
    // =========================================================

    @PostMapping("/cases/{code}/cancel")
    public VistaCasoPublico cancel(
            @PathVariable
            String code,

            @RequestParam(
                    required = false
            )
            String email,

            @RequestParam(
                    required = false
            )
            String trackingKey,

            @Valid
            @RequestBody
            SolicitudMotivoPublico r,

            HttpServletRequest req
    ) {

        return service.cancel(
                code,
                email,
                trackingKey,
                r,
                req
        );
    }

    // =========================================================
    // CU-03 FA13
    // SOLICITAR REAPERTURA
    // =========================================================

    @PostMapping("/cases/{code}/reopen-request")
    public Map<String, String> reopenRequest(
            @PathVariable
            String code,

            @RequestParam(
                    required = false
            )
            String email,

            @RequestParam(
                    required = false
            )
            String trackingKey,

            @Valid
            @RequestBody
            SolicitudMotivoPublico r,

            HttpServletRequest req
    ) {

        service.requestReopen(
                code,
                email,
                trackingKey,
                r,
                req
        );

        return Map.of(
                "message",
                "Solicitud de reapertura registrada para revisión."
        );
    }

    // =========================================================
    // CU-03 FA12
    // DESCARGAR EVIDENCIA
    // =========================================================

    @GetMapping(
            "/cases/{code}/evidences/{evidenceId}/download"
    )
    public ResponseEntity<?> download(
            @PathVariable
            String code,

            @PathVariable
            Long evidenceId,

            @RequestParam(
                    required = false
            )
            String email,

            @RequestParam(
                    required = false
            )
            String trackingKey,

            HttpServletRequest req
    ) {

        var download =
                service.download(
                        code,
                        evidenceId,
                        email,
                        trackingKey,
                        req
                );

        return ResponseEntity
                .ok()
                .contentType(
                        MediaType.parseMediaType(
                                download.contentType()
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,

                        ContentDisposition
                                .attachment()
                                .filename(
                                        download.filename(),
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                )
                .body(
                        download.resource()
                );
    }

    // =========================================================
    // SATISFACCIÓN
    // =========================================================

    @PostMapping("/cases/{code}/satisfaction")
    public Map<String, String> satisfaction(
            @PathVariable
            String code,

            @RequestParam(
                    required = false
            )
            String email,

            @RequestParam(
                    required = false
            )
            String trackingKey,

            @Valid
            @RequestBody
            SolicitudSatisfaccion r,

            HttpServletRequest req
    ) {

        service.rate(
                code,
                email,
                trackingKey,
                r,
                req
        );

        return Map.of(
                "message",
                "Gracias por calificar la atención."
        );
    }
}