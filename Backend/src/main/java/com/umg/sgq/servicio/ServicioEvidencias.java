package com.umg.sgq.servicio;

import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.*;
import com.umg.sgq.repositorio.*;
import com.umg.sgq.seguridad.ServicioUsuarioActual;
import com.umg.sgq.utilidad.UtilidadHash;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class ServicioEvidencias {
    private final RepositorioEvidencia repo;
    private final ServicioSeguridadArchivos security;
    private final ServicioAuditoria audit;
    private final ServicioUsuarioActual current;
    private final Path root;

    public ServicioEvidencias(RepositorioEvidencia repo, ServicioSeguridadArchivos security, ServicioAuditoria audit, ServicioUsuarioActual current, @Value("${app.uploads.directory}") String directory) {
        this.repo = repo;
        this.security = security;
        this.audit = audit;
        this.current = current;
        this.root = Paths.get(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible preparar el directorio de evidencias", e);
        }
    }

    public void validateCount(Caso c, Seguimiento follow, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return;
        if (files.size() > 5)
            throw new IllegalArgumentException("Se alcanzó la cantidad máxima de archivos permitidos.");
        long existing = follow == null ? repo.countByComplaintCaseIdAndFollowUpIsNullAndStatus(c.getId(), EstadoEvidencia.ACTIVA) : repo.countByFollowUpIdAndStatus(follow.getId(), EstadoEvidencia.ACTIVA);
        if (existing + files.size() > 5)
            throw new IllegalArgumentException("Se alcanzó la cantidad máxima de archivos permitidos.");
        files.forEach(security::validate);
    }

    @Transactional
    public List<Evidencia> store(Caso c, Seguimiento follow, List<MultipartFile> files, HttpServletRequest req) {
        Usuario uploader = current.orNull();
        boolean visible = follow != null ? follow.isVisibleToClient() : uploader == null;
        return store(c, follow, files, visible, null, req);
    }

    @Transactional
    public List<Evidencia> store(Caso c, Seguimiento follow, List<MultipartFile> files, boolean visibleToClient, String description, HttpServletRequest req) {
        if (files == null || files.isEmpty()) return List.of();
        if (description != null && description.length() > 500)
            throw new IllegalArgumentException("La descripción del archivo supera el límite permitido.");
        validateCount(c, follow, files);
        List<Path> created = new ArrayList<>();
        List<Evidencia> saved = new ArrayList<>();
        Set<String> batchHashes = new HashSet<>();
        try {
            Path caseDir = root.resolve(c.getCode());
            Files.createDirectories(caseDir);
            for (MultipartFile f : files) {
                byte[] bytes = f.getBytes();
                String checksum = UtilidadHash.sha256Bytes(bytes);
                if (!batchHashes.add(checksum) || repo.existsByComplaintCaseIdAndSha256AndStatus(c.getId(), checksum, EstadoEvidencia.ACTIVA))
                    throw new IllegalArgumentException("La evidencia ya fue adjuntada al caso.");
                String original = Optional.ofNullable(f.getOriginalFilename()).orElse("archivo");
                String ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
                String stored = UUID.randomUUID() + "." + ext;
                Path dest = caseDir.resolve(stored).normalize();
                if (!dest.startsWith(caseDir)) throw new IllegalArgumentException("Nombre de archivo inválido.");
                Files.write(dest, bytes, StandardOpenOption.CREATE_NEW);
                created.add(dest);
                Evidencia e = new Evidencia();
                e.setComplaintCase(c);
                e.setFollowUp(follow);
                e.setUploadedBy(current.orNull());
                e.setOriginalName(original);
                e.setStoredName(stored);
                e.setContentType(f.getContentType());
                e.setSizeBytes(f.getSize());
                e.setStoragePath(dest.toString());
                e.setSha256(checksum);
                e.setDescription(description == null ? null : description.trim());
                e.setVisibleToClient(visibleToClient);
                saved.add(repo.save(e));
                audit.log(req, "DOCUMENTOS", "CARGA", "CASO", c.getCode(), "Carga de evidencia " + original, ResultadoAuditoria.EXITOSO, null, Map.of("archivo", original, "tamano", f.getSize(), "visibleCliente", visibleToClient));
            }
            return saved;
        } catch (Exception ex) {
            created.forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
            if (ex instanceof RuntimeException) throw (RuntimeException) ex;
            throw new IllegalStateException("No fue posible almacenar el archivo.", ex);
        }
    }

    @Transactional
    public Download download(Long id, HttpServletRequest req) {
        Evidencia e = require(id);
        Path p = Paths.get(e.getStoragePath());
        if (!Files.exists(p)) throw new IllegalStateException("No fue posible localizar el archivo.");
        audit.log(req, "DOCUMENTOS", "DESCARGA", "CASO", e.getComplaintCase().getCode(), "Descarga de evidencia " + e.getOriginalName(), ResultadoAuditoria.EXITOSO, null, null);
        return new Download(new FileSystemResource(p), e.getOriginalName(), e.getContentType());
    }

    @Transactional(readOnly = true)
    public Evidencia require(Long id) {
        Evidencia e = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Evidencia no encontrada."));
        if (e.getStatus() != EstadoEvidencia.ACTIVA)
            throw new IllegalArgumentException("La evidencia se encuentra anulada.");
        return e;
    }

    @Transactional
    public void annul(Long id, String reason, HttpServletRequest req) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Debe ingresar una justificación.");
        Evidencia e = require(id);
        e.setStatus(EstadoEvidencia.ANULADA);
        e.setAnnulReason(reason.trim());
        e.setAnnulledAt(java.time.LocalDateTime.now());
        repo.save(e);
        audit.log(req, "DOCUMENTOS", "ANULACION", "CASO", e.getComplaintCase().getCode(), "Evidencia anulada con justificación", ResultadoAuditoria.EXITOSO, null, Map.of("motivo", reason));
    }

    public record Download(Resource resource, String filename, String contentType) {
    }
}
