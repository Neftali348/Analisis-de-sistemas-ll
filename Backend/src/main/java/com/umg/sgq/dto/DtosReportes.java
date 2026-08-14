package com.umg.sgq.dto;

import com.umg.sgq.enumeracion.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.*;

public final class DtosReportes {
    private DtosReportes() {
    }

    public record FiltroReporte(@NotNull TipoReporte reportType, @NotNull LocalDate from, @NotNull LocalDate to,
                               TipoCaso type, EstadoCaso status, Prioridad priority, Long branchId, Long responsibleId,
                               CategoriaCaso category) {
    }

    public record VistaPreviaReporte(TipoReporte reportType, String generatedBy, Map<String, Object> summary,
                                List<Map<String, Object>> rows) {
    }
}
