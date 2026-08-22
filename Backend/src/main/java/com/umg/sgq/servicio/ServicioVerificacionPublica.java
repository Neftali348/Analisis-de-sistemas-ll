package com.umg.sgq.servicio;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServicioVerificacionPublica {

    // =========================================================
    // CONFIGURACIÓN
    // =========================================================

    private static final int MINUTOS_VALIDEZ = 5;

    private final SecureRandom random =
            new SecureRandom();

    private final Map<String, DesafioSeguridad> desafios =
            new ConcurrentHashMap<>();

    // =========================================================
    // CREAR DESAFÍO
    // =========================================================

    public Map<String, String> crearDesafio() {

        limpiarExpirados();

        int numero1 =
                random.nextInt(9) + 1;

        int numero2 =
                random.nextInt(9) + 1;

        int respuesta =
                numero1 + numero2;

        String id =
                UUID.randomUUID()
                        .toString();

        LocalDateTime vence =
                LocalDateTime
                        .now()
                        .plusMinutes(
                                MINUTOS_VALIDEZ
                        );

        desafios.put(
                id,
                new DesafioSeguridad(
                        respuesta,
                        vence
                )
        );

        return Map.of(
                "id",
                id,

                "question",
                "¿Cuánto es "
                        + numero1
                        + " + "
                        + numero2
                        + "?"
        );
    }

    // =========================================================
    // VALIDAR DESAFÍO
    // =========================================================

    public boolean validar(
            String id,
            String respuestaIngresada
    ) {

        if (
                id == null
                        || id.isBlank()
                        || respuestaIngresada == null
                        || respuestaIngresada.isBlank()
        ) {

            return false;
        }

        /*
         * remove() hace que la verificación sea de un solo uso.
         *
         * Una vez utilizada, correcta o incorrectamente,
         * deberá solicitarse un nuevo desafío.
         */
        DesafioSeguridad desafio =
                desafios.remove(
                        id.trim()
                );

        if (
                desafio == null
        ) {

            return false;
        }

        // =====================================================
        // VERIFICAR EXPIRACIÓN
        // =====================================================

        if (
                desafio.vence()
                        .isBefore(
                                LocalDateTime.now()
                        )
        ) {

            return false;
        }

        int respuesta;

        try {

            respuesta =
                    Integer.parseInt(
                            respuestaIngresada.trim()
                    );

        } catch (NumberFormatException ex) {

            return false;
        }

        return respuesta
                == desafio.respuesta();
    }

    // =========================================================
    // LIMPIAR DESAFÍOS EXPIRADOS
    // =========================================================

    private void limpiarExpirados() {

        LocalDateTime ahora =
                LocalDateTime.now();

        desafios
                .entrySet()
                .removeIf(
                        entry ->
                                entry
                                        .getValue()
                                        .vence()
                                        .isBefore(
                                                ahora
                                        )
                );
    }

    // =========================================================
    // ESTRUCTURA INTERNA
    // =========================================================

    private record DesafioSeguridad(
            int respuesta,
            LocalDateTime vence
    ) {
    }
}