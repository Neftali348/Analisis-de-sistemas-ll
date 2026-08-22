package com.umg.sgq.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.sgq.entidad.RegistroAuditoria;
import com.umg.sgq.entidad.Usuario;
import com.umg.sgq.enumeracion.ResultadoAuditoria;
import com.umg.sgq.repositorio.RepositorioRegistroAuditoria;
import com.umg.sgq.seguridad.ServicioUsuarioActual;
import com.umg.sgq.utilidad.UtilidadHash;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static com.umg.sgq.utilidad.UtilidadPeticion.ip;

@Service
public class ServicioAuditoria {

    private final RepositorioRegistroAuditoria repo;
    private final ServicioUsuarioActual current;
    private final ObjectMapper mapper;

    public ServicioAuditoria(
            RepositorioRegistroAuditoria repo,
            ServicioUsuarioActual current,
            ObjectMapper mapper
    ) {
        this.repo = repo;
        this.current = current;
        this.mapper = mapper;
    }

    // =========================================================
    // REGISTRO GENERAL DE AUDITORÍA
    // =========================================================

    @Transactional
    public RegistroAuditoria log(
            HttpServletRequest req,
            String module,
            String action,
            String entityType,
            String ref,
            String description,
            ResultadoAuditoria result,
            Object oldV,
            Object newV
    ) {

        Usuario u =
                current.orNull();

        return logAs(
                req,
                u == null
                        ? "ANÓNIMO"
                        : u.getUsername(),

                u == null
                        ? "PUBLICO"
                        : u.getRole().getCode().name(),

                module,
                action,
                entityType,
                ref,
                description,
                result,
                oldV,
                newV
        );
    }

    // =========================================================
    // AUDITORÍA CON ACTOR INDICADO
    // =========================================================

    @Transactional
    public RegistroAuditoria logActor(
            HttpServletRequest req,
            String username,
            String role,
            String module,
            String action,
            String entityType,
            String ref,
            String description,
            ResultadoAuditoria result
    ) {

        return logAs(
                req,
                username,
                role,
                module,
                action,
                entityType,
                ref,
                description,
                result,
                null,
                null
        );
    }

    // =========================================================
    // EVENTOS AUTOMÁTICOS DEL SISTEMA
    // =========================================================

    @Transactional
    public RegistroAuditoria logSystem(
            String module,
            String action,
            String entityType,
            String ref,
            String description,
            ResultadoAuditoria result
    ) {

        return logAs(
                null,
                "SISTEMA",
                "SISTEMA",
                module,
                action,
                entityType,
                ref,
                description,
                result,
                null,
                null
        );
    }

    // =========================================================
    // CU-03 FA05
    // CONSULTA PÚBLICA FALLIDA
    // =========================================================

    /*
     * REQUIRES_NEW hace que este registro tenga su propia
     * transacción.
     *
     * De esta forma, aunque ServicioCasosPublicos lance después
     * una IllegalArgumentException, el intento fallido queda
     * registrado en la bitácora.
     *
     * IMPORTANTE:
     * No guardamos correo electrónico ni clave temporal.
     * Tampoco indicamos cuál dato fue incorrecto.
     */
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void logConsultaPublicaFallida(
            HttpServletRequest req,
            String codigo
    ) {

        String referencia =
                limpiarReferenciaCodigo(
                        codigo
                );

        logAs(
                req,

                // Usuario público no autenticado
                "ANÓNIMO",

                // Rol público
                "PUBLICO",

                // Módulo
                "CASOS",

                // Acción
                "CONSULTA_PUBLICA_FALLIDA",

                // Entidad
                "CASO",

                // Código introducido
                referencia,

                // Descripción genérica
                "Intento fallido de consulta pública del estado de un caso",

                // Resultado
                ResultadoAuditoria.FALLIDO,

                // Sin información sensible
                null,

                // Sin correo, clave ni datos sensibles
                null
        );
    }

    // =========================================================
    // CU-03 FA16
    // ERROR TÉCNICO DURANTE CONSULTA PÚBLICA
    // =========================================================

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void logErrorConsultaPublica(
            HttpServletRequest req,
            String codigo,
            String detalle
    ) {

        String referencia =
                limpiarReferenciaCodigo(
                        codigo
                );

        String detalleSeguro;

        if (
                detalle == null
                        || detalle.isBlank()
        ) {

            detalleSeguro =
                    "Error técnico no especificado";

        } else {

            detalleSeguro =
                    detalle.trim();

            if (
                    detalleSeguro.length()
                            > 500
            ) {

                detalleSeguro =
                        detalleSeguro.substring(
                                0,
                                500
                        );
            }
        }

        logAs(
                req,
                "ANÓNIMO",
                "PUBLICO",
                "CASOS",
                "ERROR_CONSULTA_PUBLICA",
                "CASO",
                referencia,
                "Error técnico durante consulta pública: "
                        + detalleSeguro,
                ResultadoAuditoria.FALLIDO,
                null,
                null
        );
    }

    // =========================================================
    // RECHAZO DE EVIDENCIA
    // =========================================================

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void logRechazoArchivo(
            HttpServletRequest req,
            String codigoCaso,
            String nombreArchivo,
            String motivo
    ) {

        String motivoSeguro =
                motivo == null
                        || motivo.isBlank()
                        ? "Error no especificado"
                        : motivo.trim();

        String archivoSeguro =
                nombreArchivo == null
                        || nombreArchivo.isBlank()
                        ? "ARCHIVO_SIN_NOMBRE"
                        : nombreArchivo.trim();

        if (
                archivoSeguro.length()
                        > 255
        ) {

            archivoSeguro =
                    archivoSeguro.substring(
                            0,
                            255
                    );
        }

        if (
                motivoSeguro.length()
                        > 500
        ) {

            motivoSeguro =
                    motivoSeguro.substring(
                            0,
                            500
                    );
        }

        logAs(
                req,
                "ANÓNIMO",
                "PUBLICO",
                "DOCUMENTOS",
                "CARGA_RECHAZADA",
                "CASO",
                codigoCaso,
                "Carga de evidencia rechazada: "
                        + archivoSeguro
                        + ". Motivo: "
                        + motivoSeguro,
                ResultadoAuditoria.FALLIDO,
                null,
                Map.of(
                        "archivo",
                        archivoSeguro,
                        "motivo",
                        motivoSeguro
                )
        );
    }

    // =========================================================
    // CREAR REGISTRO
    // =========================================================

    /*
     * synchronized protege la secuencia de hashes para evitar
     * que dos registros creados al mismo tiempo lean el mismo
     * previousHash.
     */
    private synchronized RegistroAuditoria logAs(
            HttpServletRequest req,
            String username,
            String role,
            String module,
            String action,
            String entityType,
            String ref,
            String description,
            ResultadoAuditoria result,
            Object oldV,
            Object newV
    ) {

        RegistroAuditoria a =
                new RegistroAuditoria();

        a.setCreatedAt(
                LocalDateTime
                        .now()
                        .truncatedTo(
                                ChronoUnit.MILLIS
                        )
        );

        a.setUsername(
                valorSeguro(
                        username,
                        "ANÓNIMO"
                )
        );

        a.setRole(
                valorSeguro(
                        role,
                        "PUBLICO"
                )
        );

        a.setIpAddress(
                req == null
                        ? "SYSTEM"
                        : ip(req)
        );

        a.setModule(
                valorSeguro(
                        module,
                        "SISTEMA"
                )
        );

        a.setAction(
                valorSeguro(
                        action,
                        "SIN_ACCION"
                )
        );

        a.setEntityType(
                valorSeguro(
                        entityType,
                        "SIN_ENTIDAD"
                )
        );

        a.setEntityReference(
                ref
        );

        a.setDescription(
                description
        );

        a.setResult(
                result
        );

        // =====================================================
        // VALORES ANTERIORES / NUEVOS
        // =====================================================

        try {

            a.setOldValues(
                    oldV == null
                            ? null
                            : mapper.writeValueAsString(
                            oldV
                    )
            );

            a.setNewValues(
                    newV == null
                            ? null
                            : mapper.writeValueAsString(
                            newV
                    )
            );

        } catch (Exception ignored) {

            /*
             * La auditoría principal no debe fallar únicamente
             * porque un objeto no pueda serializarse.
             */

            a.setOldValues(null);
            a.setNewValues(null);
        }

        // =====================================================
        // CADENA DE INTEGRIDAD
        // =====================================================

        String previousHash =
                repo
                        .findTopByOrderByIdDesc()
                        .map(
                                RegistroAuditoria::getIntegrityHash
                        )
                        .orElse(
                                "GENESIS"
                        );

        a.setPreviousHash(
                previousHash
        );

        a.setIntegrityHash(
                hashOf(a)
        );

        return repo.save(a);
    }

    // =========================================================
    // VERIFICAR UN REGISTRO
    // =========================================================

    public boolean verify(
            RegistroAuditoria a
    ) {

        return a != null
                && Objects.equals(
                a.getIntegrityHash(),
                hashOf(a)
        );
    }

    // =========================================================
    // VERIFICAR TODA LA CADENA
    // =========================================================

    @Transactional(
            readOnly = true
    )
    public Map<String, Object> verifyChain() {

        List<RegistroAuditoria> logs =
                repo
                        .findAll()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        RegistroAuditoria::getId
                                )
                        )
                        .toList();

        String expectedPrev =
                "GENESIS";

        Long brokenId =
                null;

        for (
                RegistroAuditoria a : logs
        ) {

            boolean previousCorrect =
                    Objects.equals(
                            expectedPrev,
                            a.getPreviousHash()
                    );

            boolean hashCorrect =
                    verify(a);

            if (
                    !previousCorrect
                            || !hashCorrect
            ) {

                brokenId =
                        a.getId();

                break;
            }

            expectedPrev =
                    a.getIntegrityHash();
        }

        return Map.of(
                "valid",
                brokenId == null,

                "records",
                logs.size(),

                "brokenId",
                brokenId == null
                        ? ""
                        : brokenId.toString()
        );
    }

    // =========================================================
    // CREAR HASH
    // =========================================================

    private String hashOf(
            RegistroAuditoria a
    ) {

        String payload =
                String.join(
                        "|",

                        safe(
                                a.getPreviousHash()
                        ),

                        safe(
                                a.getCreatedAt()
                        ),

                        safe(
                                a.getUsername()
                        ),

                        safe(
                                a.getRole()
                        ),

                        safe(
                                a.getIpAddress()
                        ),

                        safe(
                                a.getModule()
                        ),

                        safe(
                                a.getAction()
                        ),

                        safe(
                                a.getEntityType()
                        ),

                        safe(
                                a.getEntityReference()
                        ),

                        safe(
                                a.getDescription()
                        ),

                        safe(
                                a.getResult()
                        ),

                        safe(
                                a.getOldValues()
                        ),

                        safe(
                                a.getNewValues()
                        )
                );

        return UtilidadHash.sha256(
                payload
        );
    }

    // =========================================================
    // LIMPIAR CÓDIGO PARA AUDITORÍA
    // =========================================================

    private String limpiarReferenciaCodigo(
            String codigo
    ) {

        if (
                codigo == null
                        || codigo.isBlank()
        ) {

            return "NO_INFORMADO";
        }

        String limpio =
                codigo
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        /*
         * Evitamos guardar entradas enormes provenientes
         * directamente del portal público.
         */
        if (
                limpio.length()
                        > 50
        ) {

            limpio =
                    limpio.substring(
                            0,
                            50
                    );
        }

        return limpio;
    }

    // =========================================================
    // VALOR SEGURO
    // =========================================================

    private String valorSeguro(
            String valor,
            String predeterminado
    ) {

        if (
                valor == null
                        || valor.isBlank()
        ) {

            return predeterminado;
        }

        return valor.trim();
    }

    // =========================================================
    // SAFE PARA HASH
    // =========================================================

    private String safe(
            Object value
    ) {

        return value == null
                ? ""
                : String.valueOf(
                value
        );
    }
}