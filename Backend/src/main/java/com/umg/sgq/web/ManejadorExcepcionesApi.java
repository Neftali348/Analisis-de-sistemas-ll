package com.umg.sgq.web;

import com.umg.sgq.enumeracion.ResultadoAuditoria;
import com.umg.sgq.servicio.ServicioAuditoria;

import jakarta.persistence.OptimisticLockException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ManejadorExcepcionesApi {

    private final ServicioAuditoria auditoria;

    public ManejadorExcepcionesApi(
            ServicioAuditoria auditoria
    ) {
        this.auditoria = auditoria;
    }

    // =========================================================
    // ERRORES DE VALIDACIÓN
    // Ejemplo: campos @NotBlank, @Email, etc.
    // =========================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(
            MethodArgumentNotValidException e
    ) {

        Map<String, String> errors =
                new LinkedHashMap<>();

        e.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "message",
                                "Revise los campos ingresados",
                                "errors",
                                errors
                        )
                );
    }

    // =========================================================
    // ERRORES CONTROLADOS DE NEGOCIO
    //
    // Ejemplos:
    // - Usuario o contraseña incorrectos.
    // - Usuario bloqueado.
    // - Usuario sin permisos.
    // =========================================================
    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public ResponseEntity<?> business(
            RuntimeException e
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "message",
                                e.getMessage()
                        )
                );
    }

    // =========================================================
    // CONCURRENCIA
    // =========================================================
    @ExceptionHandler({
            OptimisticLockException.class,
            OptimisticLockingFailureException.class
    })
    public ResponseEntity<?> concurrency(
            Exception e
    ) {

        return ResponseEntity
                .status(409)
                .body(
                        Map.of(
                                "message",
                                "La operación fue rechazada porque la información cambió durante el proceso."
                        )
                );
    }

    // =========================================================
    // ARCHIVOS DEMASIADO GRANDES
    // =========================================================
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> upload(
            MaxUploadSizeExceededException e
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "message",
                                "El archivo supera el tamaño máximo permitido de 2 MB."
                        )
                );
    }

    // =========================================================
    // FA12 - ERROR DE CONEXIÓN CON BASE DE DATOS
    // =========================================================
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> database(
            DataAccessException e
    ) {

        /*
         * Intentamos registrar el fallo.
         *
         * Si PostgreSQL está caído, guardar la auditoría
         * también puede fallar. Por eso usamos try/catch.
         */
        try {

            auditoria.logSystem(
                    "SISTEMA",
                    "ERROR_CONEXION",
                    "BASE_DATOS",
                    e.getClass().getSimpleName(),
                    "Error de acceso a la base de datos",
                    ResultadoAuditoria.FALLIDO
            );

        } catch (Exception ignored) {
            /*
             * Si la base está caída, es imposible
             * guardar el evento dentro de esa misma BD.
             */
        }

        return ResponseEntity
                .status(503)
                .body(
                        Map.of(
                                "message",
                                "No fue posible conectar con el servidor o la base de datos."
                        )
                );
    }

    // =========================================================
    // FA13 - ERROR INTERNO NO CONTROLADO
    //
    // IMPORTANTE:
    // ESTE MANEJADOR DEBE QUEDAR AL FINAL.
    // =========================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> generic(
            Exception e
    ) {

        try {

            auditoria.logSystem(
                    "SISTEMA",
                    "ERROR_INTERNO",
                    "EXCEPCION",
                    e.getClass().getSimpleName(),
                    "Error interno no controlado",
                    ResultadoAuditoria.FALLIDO
            );

        } catch (Exception ignored) {
        }

        /*
         * Durante desarrollo sí conviene imprimir
         * el error completo en la consola de Spring.
         */
        e.printStackTrace();

        return ResponseEntity
                .status(500)
                .body(
                        Map.of(
                                "message",
                                "Error interno del sistema. Intente nuevamente."
                        )
                );
    }
}