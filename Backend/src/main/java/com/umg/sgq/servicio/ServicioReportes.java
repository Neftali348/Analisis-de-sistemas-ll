package com.umg.sgq.servicio;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.umg.sgq.dto.DtosReportes.*;
import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.*;
import com.umg.sgq.repositorio.*;
import com.umg.sgq.seguridad.ServicioUsuarioActual;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.List;

@Service
public class ServicioReportes {
    private final RepositorioCaso cases;
    private final RepositorioSatisfaccion satisfaction;
    private final RepositorioRegistroAuditoria audits;
    private final ServicioUsuarioActual current;
    private final ServicioAuditoria audit;

    public ServicioReportes(RepositorioCaso cases, RepositorioSatisfaccion satisfaction, RepositorioRegistroAuditoria audits, ServicioUsuarioActual current, ServicioAuditoria audit) {
        this.cases = cases;
        this.satisfaction = satisfaction;
        this.audits = audits;
        this.current = current;
        this.audit = audit;
    }

    @Transactional
    public VistaPreviaReporte preview(FiltroReporte f, HttpServletRequest req) {
        validate(f);
        List<Map<String, Object>> rows = buildRows(f);
        Map<String, Object> summary = summary(rows, f.reportType());
        String user = current.require().getUsername();
        audit.log(req, "REPORTES", "GENERACION", "REPORTE", f.reportType().name(), "Reporte generado en pantalla", ResultadoAuditoria.EXITOSO, null, Map.of("desde", f.from().toString(), "hasta", f.to().toString(), "filas", rows.size()));
        return new VistaPreviaReporte(f.reportType(), user, summary, rows);
    }

    @Transactional
    public Export export(FiltroReporte f, String format, HttpServletRequest req) {
        validate(f);
        List<Map<String, Object>> rows = buildRows(f);
        String user = current.require().getUsername();
        String normalizedFormat = format == null ? "" : format.trim().toUpperCase(Locale.ROOT);
        byte[] bytes;
        String ext;
        String contentType;
        switch (normalizedFormat) {
            case "PDF":
                bytes = pdf(f, rows, user);
                ext = "pdf";
                contentType = "application/pdf";
                break;
            case "XLSX":
                bytes = xlsx(f, rows, user);
                ext = "xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                break;
            default:
                throw new IllegalArgumentException("Formato de reporte no permitido.");
        }
        String name = "reporte_" + f.reportType().name().toLowerCase(Locale.ROOT) + "_" + LocalDate.now() + "." + ext;
        audit.log(req, "REPORTES", "EXPORTACION", "REPORTE", f.reportType().name(), "Reporte exportado a " + ext.toUpperCase(Locale.ROOT), ResultadoAuditoria.EXITOSO, null, Map.of("usuario", user, "filas", rows.size()));
        return new Export(bytes, name, contentType);
    }

    private void validate(FiltroReporte f) {
        if (f.from().isAfter(f.to()))
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        CodigoRol role = current.require().getRole().getCode();
        if (role == CodigoRol.AUDITOR && f.reportType() != TipoReporte.AUDITORIA)
            throw new IllegalArgumentException("El Auditor únicamente puede generar reportes de auditoría.");
        if (role == CodigoRol.SUPERVISOR && f.reportType() == TipoReporte.AUDITORIA)
            throw new IllegalArgumentException("No posee permisos para realizar esta acción.");
    }

    private List<Map<String, Object>> buildRows(FiltroReporte f) {
        LocalDateTime from = f.from().atStartOfDay(), to = f.to().plusDays(1).atStartOfDay().minusNanos(1);
        if (f.reportType() == TipoReporte.AUDITORIA) {
            return audits.findAll().stream().filter(a -> !a.getCreatedAt().isBefore(from) && !a.getCreatedAt().isAfter(to)).map(this::auditRow).limit(10000).toList();
        }
        if (f.reportType() == TipoReporte.SATISFACCION) {
            return satisfaction.findAll().stream().filter(s -> between(s.getCreatedAt(), from, to)).filter(s -> caseMatches(s.getComplaintCase(), f)).map(this::satisfactionRow).toList();
        }
        return cases.findCreatedBetween(from, to).stream().filter(c -> caseMatches(c, f)).map(c -> caseRow(c, f.reportType())).toList();
    }

    private boolean caseMatches(Caso c, FiltroReporte f) {
        Usuario u = current.require();
        if (u.getRole().getCode() == CodigoRol.SUPERVISOR && u.getBranch() != null && !Objects.equals(c.getBranch().getId(), u.getBranch().getId()))
            return false;
        return (f.type() == null || c.getType() == f.type()) && (f.status() == null || c.getStatus() == f.status()) && (f.priority() == null || c.getPriority() == f.priority()) && (f.branchId() == null || Objects.equals(c.getBranch().getId(), f.branchId())) && (f.responsibleId() == null || (c.getResponsible() != null && Objects.equals(c.getResponsible().getId(), f.responsibleId()))) && (f.category() == null || c.getCategory() == f.category());
    }

    private Map<String, Object> caseRow(Caso c, TipoReporte type) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("Código", c.getCode());
        m.put("Fecha", c.getCreatedAt());
        m.put("Tipo", c.getType());
        m.put("Estado", c.getStatus());
        m.put("Prioridad", c.getPriority());
        m.put("Sucursal", c.getBranch().getName());
        if (type == TipoReporte.TIEMPOS_ATENCION) {
            m.put("Responsable", c.getResponsible() == null ? "Sin asignar" : c.getResponsible().getFullName());
            m.put("Primera respuesta", c.getFirstResponseAt());
            m.put("Límite SLA", c.getSlaDeadlineAt());
            m.put("Cumple SLA", c.getFirstResponseAt() != null && !c.getFirstResponseAt().isAfter(c.getSlaDeadlineAt()) ? "Sí" : c.isSlaBreached() ? "No" : "Pendiente");
        }
        if (type == TipoReporte.CASOS_RESPONSABLE) {
            m.put("Responsable", c.getResponsible() == null ? "Sin asignar" : c.getResponsible().getFullName());
        }
        if (type == TipoReporte.CASOS_SUCURSAL) {
            m.put("Categoría", c.getCategory());
        }
        if (type == TipoReporte.CASOS_REGISTRADOS) {
            m.put("Categoría", c.getCategory());
            m.put("Canal", "PORTAL_PUBLICO");
        }
        return m;
    }

    private Map<String, Object> satisfactionRow(Satisfaccion s) {
        Caso c = s.getComplaintCase();
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("Código", c.getCode());
        m.put("Fecha", s.getCreatedAt());
        m.put("Sucursal", c.getBranch().getName());
        m.put("Tipo", c.getType());
        m.put("Calificación", s.getRating());
        m.put("Comentario", s.getComment());
        return m;
    }

    private Map<String, Object> auditRow(RegistroAuditoria a) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        m.put("Fecha", a.getCreatedAt());
        m.put("Usuario", a.getUsername());
        m.put("Rol", a.getRole());
        m.put("Acción", a.getAction());
        m.put("Módulo", a.getModule());
        m.put("IP", a.getIpAddress());
        m.put("Resultado", a.getResult());
        m.put("Referencia", a.getEntityReference());
        return m;
    }

    private Map<String, Object> summary(List<Map<String, Object>> rows, TipoReporte t) {
        LinkedHashMap<String, Object> s = new LinkedHashMap<>();
        s.put("total", rows.size());
        if (t == TipoReporte.SATISFACCION) {
            double avg = rows.stream().map(x -> x.get("Calificación")).filter(Objects::nonNull).mapToDouble(x -> Double.parseDouble(String.valueOf(x))).average().orElse(0);
            s.put("promedioCalificacion", Math.round(avg * 100.0) / 100.0);
        }
        return s;
    }

    private byte[] pdf(FiltroReporte f, List<Map<String, Object>> rows, String user) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 20, 20, 25, 25);
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph("Sistema de Gestión de Quejas - " + f.reportType().name().replace('_', ' ')));
            doc.add(new Paragraph("Generado por: " + user + " | Fecha: " + LocalDateTime.now() + " | Rango: " + f.from() + " a " + f.to()));
            doc.add(Chunk.NEWLINE);
            if (rows.isEmpty()) {
                doc.add(new Paragraph("No existen registros que coincidan con los criterios de búsqueda."));
            } else {
                List<String> cols = new ArrayList<>(rows.get(0).keySet());
                PdfPTable table = new PdfPTable(cols.size());
                table.setWidthPercentage(100);
                for (String c : cols) {
                    PdfPCell cell = new PdfPCell(new Phrase(c));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                }
                for (Map<String, Object> row : rows) {
                    for (String c : cols) table.addCell(text(row.get(c)));
                }
                doc.add(table);
            }
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible generar el reporte.", e);
        }
    }

    private byte[] xlsx(FiltroReporte f, List<Map<String, Object>> rows, String user) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("Reporte");
            int r = 0;
            Row title = sh.createRow(r++);
            title.createCell(0).setCellValue("SGQ - " + f.reportType().name());
            Row meta = sh.createRow(r++);
            meta.createCell(0).setCellValue("Generado por: " + user + " | " + LocalDateTime.now() + " | " + f.from() + " a " + f.to());
            r++;
            if (!rows.isEmpty()) {
                List<String> cols = new ArrayList<>(rows.get(0).keySet());
                Row head = sh.createRow(r++);
                for (int i = 0; i < cols.size(); i++) head.createCell(i).setCellValue(cols.get(i));
                for (Map<String, Object> data : rows) {
                    Row row = sh.createRow(r++);
                    for (int i = 0; i < cols.size(); i++) row.createCell(i).setCellValue(text(data.get(cols.get(i))));
                }
                for (int i = 0; i < cols.size(); i++) sh.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible generar el reporte.", e);
        }
    }

    private boolean between(LocalDateTime d, LocalDateTime f, LocalDateTime t) {
        return d != null && !d.isBefore(f) && !d.isAfter(t);
    }

    private String text(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    public record Export(byte[] bytes, String filename, String contentType) {
    }
}
