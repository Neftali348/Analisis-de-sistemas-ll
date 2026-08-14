package com.umg.sgq.servicio;

import com.umg.sgq.entidad.Caso;
import com.umg.sgq.entidad.Usuario;
import com.umg.sgq.enumeracion.EstadoCaso;
import com.umg.sgq.enumeracion.CodigoRol;
import com.umg.sgq.repositorio.RepositorioCaso;
import com.umg.sgq.seguridad.ServicioUsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ServicioPanel {

    private final RepositorioCaso cases;
    private final ServicioUsuarioActual current;

    public ServicioPanel(
            RepositorioCaso cases,
            ServicioUsuarioActual current
    ) {
        this.cases = cases;
        this.current = current;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {

        Usuario usuario = current.require();

        CodigoRol rol = usuario.getRole().getCode();

        Long branchId = usuario.getBranch() != null
                ? usuario.getBranch().getId()
                : null;

        List<Caso> lista;

        /*
         * RN01:
         *
         * AGENTE_ATENCION:
         * únicamente sus casos asignados.
         *
         * SUPERVISOR:
         * únicamente casos de su sucursal.
         *
         * ADMINISTRADOR:
         * todos los casos.
         *
         * AUDITOR:
         * consulta general.
         */

        if (rol == CodigoRol.AGENTE_ATENCION) {

            if (branchId == null) {
                lista = List.of();
            } else {
                lista =
                        cases.findByBranchIdAndResponsibleIdOrderByCreatedAtDesc(
                                branchId,
                                usuario.getId()
                        );
            }

        } else if (rol == CodigoRol.SUPERVISOR) {

            if (branchId == null) {
                lista = List.of();
            } else {
                lista =
                        cases.findByBranchIdOrderByCreatedAtDesc(branchId);
            }

        } else {

            // ADMINISTRADOR y AUDITOR
            lista = cases.findAllByOrderByCreatedAtDesc();
        }

        Map<String, Long> porEstado =
                lista.stream()
                        .collect(
                                Collectors.groupingBy(
                                        c -> c.getStatus().name(),
                                        LinkedHashMap::new,
                                        Collectors.counting()
                                )
                        );

        Map<String, Long> porPrioridad =
                lista.stream()
                        .collect(
                                Collectors.groupingBy(
                                        c -> c.getPriority().name(),
                                        LinkedHashMap::new,
                                        Collectors.counting()
                                )
                        );

        long abiertos =
                lista.stream()
                        .filter(c ->
                                !Set.of(
                                        EstadoCaso.CERRADO,
                                        EstadoCaso.RECHAZADO,
                                        EstadoCaso.CANCELADO
                                ).contains(c.getStatus())
                        )
                        .count();

        long slaVencidos =
                lista.stream()
                        .filter(Caso::isSlaBreached)
                        .count();

        long sinAsignar =
                lista.stream()
                        .filter(c -> c.getResponsible() == null)
                        .count();

        Map<String, Object> resultado = new LinkedHashMap<>();

        resultado.put("total", lista.size());
        resultado.put("open", abiertos);
        resultado.put("slaBreached", slaVencidos);
        resultado.put("unassigned", sinAsignar);
        resultado.put("byStatus", porEstado);
        resultado.put("byPriority", porPrioridad);

        return resultado;
    }
}