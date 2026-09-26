package com.umg.sgq.controlador;
import com.umg.sgq.dto.DtosCasos.*;
import com.umg.sgq.enumeracion.*;
import com.umg.sgq.servicio.ServicioCasos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
@RestController
@RequestMapping("/api/cases")
public class ControladorCasos {
    private final ServicioCasos service;
    public ControladorCasos(ServicioCasos service) { this.service = service; }
    @GetMapping
    @PreAuthorize("@authz.has('CASE_VIEW')")
    public PaginaCasos list(@RequestParam(required=false) List<EstadoCaso> status,@RequestParam(required=false) String code,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(required=false) TipoCaso type,@RequestParam(required=false) Prioridad priority,@RequestParam(required=false) Long branchId,@RequestParam(required=false) CategoriaCaso category,@RequestParam(required=false) Long responsibleId,@RequestParam(required=false) IndicadorSla sla,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return service.search(status,code,from,to,type,priority,branchId,category,responsibleId,sla,page,size);
    }
    @GetMapping("/filters/responsibles")
    @PreAuthorize("@authz.has('CASE_VIEW')")
    public List<Map<String,Object>> responsablesFiltro() { return service.responsablesDisponiblesFiltro(); }
    @GetMapping("/filters/branches")
    @PreAuthorize("@authz.has('CASE_VIEW')")
    public List<Map<String,Object>> sucursalesFiltro() { return service.sucursalesDisponiblesFiltro(); }
    @GetMapping("/{id}")
    @PreAuthorize("@authz.has('CASE_VIEW')")
    public VistaCasoInterno detail(@PathVariable Long id,HttpServletRequest req) { return service.detail(id,req); }
    @GetMapping("/{id}/agents")
    @PreAuthorize("@authz.has('CASE_ASSIGN')")
    public List<VistaAgenteAsignacion> agents(@PathVariable Long id,@RequestParam(required=false) String q,@RequestParam(required=false) Long branchId,@RequestParam(required=false) CategoriaCaso category,@RequestParam(required=false) Boolean available,@RequestParam(required=false) Integer maxOpenCases) {
        return service.availableAgents(id,q,branchId,category,available,maxOpenCases);
    }
    @PutMapping("/{id}")
    @PreAuthorize("@authz.has('CASE_UPDATE')")
    public VistaCasoInterno edit(@PathVariable Long id,@Valid @RequestBody SolicitudEdicionCaso r,HttpServletRequest req) { return service.edit(id,r,req); }
    @PostMapping("/{id}/assign")
    @PreAuthorize("@authz.has('CASE_ASSIGN')")
    public VistaCasoInterno assign(@PathVariable Long id,@Valid @RequestBody SolicitudAsignacion r,HttpServletRequest req) { return service.assign(id,r,req); }
    @PostMapping(value="/{id}/follow-ups",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.has('CASE_FOLLOWUP')")
    public VistaCasoInterno follow(@PathVariable Long id,@Valid @RequestPart("data") SolicitudSeguimiento r,@RequestPart(value="files",required=false) List<MultipartFile> files,HttpServletRequest req) {
        return service.followUp(id,r,files,req);
    }
    @GetMapping("/{id}/evidences/availability")
    @PreAuthorize("@authz.has('EVIDENCE_UPLOAD')")
    public Map<String,Object> evidenceAvailability(@PathVariable Long id,@RequestParam TipoAsociacionEvidencia associationType,@RequestParam(required=false) Long followUpId) {
        return service.evidenceAvailability(id,associationType,followUpId);
    }
    @PostMapping(value="/{id}/evidences",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.has('EVIDENCE_UPLOAD')")
    public VistaEvidencia evidence(@PathVariable Long id,@RequestParam TipoAsociacionEvidencia associationType,@RequestParam(required=false) Long followUpId,@RequestParam String description,@RequestParam(defaultValue="false") boolean visibleToClient,@RequestParam long version,@RequestPart("file") MultipartFile file,HttpServletRequest req) {
        return service.addEvidence(id,associationType,followUpId,description,visibleToClient,version,file,req);
    }
    @PostMapping("/{id}/resolve")
    @PreAuthorize("@authz.has('CASE_RESOLVE')")
    public VistaCasoInterno resolve(@PathVariable Long id,@Valid @RequestBody SolicitudResolucion r,HttpServletRequest req) { return service.resolve(id,r,req); }
    @PostMapping("/{id}/close")
    @PreAuthorize("@authz.has('CASE_CLOSE')")
    public VistaCasoInterno close(@PathVariable Long id,@Valid @RequestBody SolicitudCierre r,HttpServletRequest req) { return service.close(id,r,req); }
    @GetMapping("/{id}/closure-certificate")
    @PreAuthorize("@authz.has('CASE_VIEW')")
    public ResponseEntity<byte[]> closureCertificate(@PathVariable Long id,HttpServletRequest req) {
        byte[] pdf=service.closureCertificate(id,req);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment()
                        .filename("constancia-cierre-"+id+".pdf",StandardCharsets.UTF_8).build().toString())
                .body(pdf);
    }
    @PostMapping("/{id}/reopen")
    @PreAuthorize("@authz.has('CASE_REOPEN')")
    public VistaCasoInterno reopen(@PathVariable Long id,@Valid @RequestBody SolicitudReapertura r,HttpServletRequest req) { return service.reopen(id,r,req); }
    @PatchMapping("/{id}/priority")
    @PreAuthorize("@authz.has('CASE_PRIORITY')")
    public VistaCasoInterno priority(@PathVariable Long id,@Valid @RequestBody SolicitudPrioridad r,HttpServletRequest req) { return service.priority(id,r,req); }
    @PatchMapping("/{id}/status")
    @PreAuthorize("@authz.has('CASE_UPDATE')")
    public VistaCasoInterno status(@PathVariable Long id,@Valid @RequestBody SolicitudEstado r,HttpServletRequest req) { return service.status(id,r,req); }
}
