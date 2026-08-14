package com.umg.sgq.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.ResultadoAuditoria;
import com.umg.sgq.repositorio.RepositorioRegistroAuditoria;
import com.umg.sgq.seguridad.ServicioUsuarioActual;
import com.umg.sgq.utilidad.UtilidadHash;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static com.umg.sgq.utilidad.UtilidadPeticion.ip;

@Service
public class ServicioAuditoria {
    private final RepositorioRegistroAuditoria repo;
    private final ServicioUsuarioActual current;
    private final ObjectMapper mapper;

    public ServicioAuditoria(RepositorioRegistroAuditoria repo, ServicioUsuarioActual current, ObjectMapper mapper) {
        this.repo = repo;
        this.current = current;
        this.mapper = mapper;
    }

    @Transactional
    public RegistroAuditoria log(HttpServletRequest req, String module, String action, String entityType, String ref, String description, ResultadoAuditoria result, Object oldV, Object newV) {
        Usuario u = current.orNull();
        return logAs(req, u == null ? "ANÓNIMO" : u.getUsername(), u == null ? "PUBLICO" : u.getRole().getCode().name(), module, action, entityType, ref, description, result, oldV, newV);
    }

    @Transactional
    public RegistroAuditoria logActor(HttpServletRequest req, String username, String role, String module, String action, String entityType, String ref, String description, ResultadoAuditoria result) {
        return logAs(req, username, role, module, action, entityType, ref, description, result, null, null);
    }

    @Transactional
    public RegistroAuditoria logSystem(String module, String action, String entityType, String ref, String description, ResultadoAuditoria result) {
        return logAs(null, "SISTEMA", "SISTEMA", module, action, entityType, ref, description, result, null, null);
    }

    private synchronized RegistroAuditoria logAs(HttpServletRequest req, String username, String role, String module, String action, String entityType, String ref, String description, ResultadoAuditoria result, Object oldV, Object newV) {
        RegistroAuditoria a = new RegistroAuditoria();
        a.setCreatedAt(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        a.setUsername(username);
        a.setRole(role);
        a.setIpAddress(req == null ? "SYSTEM" : ip(req));
        a.setModule(module);
        a.setAction(action);
        a.setEntityType(entityType);
        a.setEntityReference(ref);
        a.setDescription(description);
        a.setResult(result);
        try {
            a.setOldValues(oldV == null ? null : mapper.writeValueAsString(oldV));
            a.setNewValues(newV == null ? null : mapper.writeValueAsString(newV));
        } catch (Exception ignored) {
        }
        String prev = repo.findTopByOrderByIdDesc().map(RegistroAuditoria::getIntegrityHash).orElse("GENESIS");
        a.setPreviousHash(prev);
        a.setIntegrityHash(hashOf(a));
        return repo.save(a);
    }

    public boolean verify(RegistroAuditoria a) {
        return a != null && Objects.equals(a.getIntegrityHash(), hashOf(a));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verifyChain() {
        List<RegistroAuditoria> logs = repo.findAll().stream().sorted(Comparator.comparing(RegistroAuditoria::getId)).toList();
        String expectedPrev = "GENESIS";
        Long brokenId = null;
        for (RegistroAuditoria a : logs) {
            if (!Objects.equals(expectedPrev, a.getPreviousHash()) || !verify(a)) {
                brokenId = a.getId();
                break;
            }
            expectedPrev = a.getIntegrityHash();
        }
        return Map.of("valid", brokenId == null, "records", logs.size(), "brokenId", brokenId == null ? "" : brokenId.toString());
    }

    private String hashOf(RegistroAuditoria a) {
        String payload = String.join("|", safe(a.getPreviousHash()), safe(a.getCreatedAt()), safe(a.getUsername()), safe(a.getRole()), safe(a.getIpAddress()), safe(a.getModule()), safe(a.getAction()), safe(a.getEntityType()), safe(a.getEntityReference()), safe(a.getDescription()), safe(a.getResult()), safe(a.getOldValues()), safe(a.getNewValues()));
        return UtilidadHash.sha256(payload);
    }

    private String safe(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
