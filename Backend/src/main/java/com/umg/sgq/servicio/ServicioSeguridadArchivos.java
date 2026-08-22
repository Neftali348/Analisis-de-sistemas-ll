package com.umg.sgq.servicio;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class ServicioSeguridadArchivos {

    // Máximo permitido: 2 MB
    private static final long MAX =
            2L * 1024 * 1024;

    private static final Set<String> EXTENSIONES_PERMITIDAS =
            Set.of(
                    "jpg",
                    "jpeg",
                    "png",
                    "pdf"
            );

    private static final Set<String> MIME_PERMITIDOS =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "application/pdf"
            );

    public void validate(MultipartFile archivo) {

        // =====================================================
        // ARCHIVO VACÍO
        // =====================================================

        if (
                archivo == null ||
                        archivo.isEmpty()
        ) {

            throw new IllegalArgumentException(
                    "El archivo está vacío."
            );
        }

        // =====================================================
        // TAMAÑO MÁXIMO 2 MB
        // =====================================================

        if (archivo.getSize() > MAX) {

            throw new IllegalArgumentException(
                    "El archivo supera el tamaño máximo permitido de 2 MB."
            );
        }

        // =====================================================
        // NOMBRE Y EXTENSIÓN
        // =====================================================

        String nombreOriginal =
                Optional.ofNullable(
                        archivo.getOriginalFilename()
                ).orElse("");

        String extension =
                obtenerExtension(
                        nombreOriginal
                );

        if (
                !EXTENSIONES_PERMITIDAS.contains(
                        extension
                )
        ) {

            throw new IllegalArgumentException(
                    "El formato del archivo no está permitido."
            );
        }

        // =====================================================
        // MIME
        // =====================================================

        String mime =
                Optional.ofNullable(
                                archivo.getContentType()
                        )
                        .orElse("")
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (
                !MIME_PERMITIDOS.contains(
                        mime
                )
        ) {

            throw new IllegalArgumentException(
                    "El tipo MIME del archivo no está permitido."
            );
        }

        /*
         * También comprobamos que extensión y MIME
         * sean compatibles.
         */
        validarMimeExtension(
                extension,
                mime
        );

        // =====================================================
        // CONTENIDO REAL
        // =====================================================

        try {

            byte[] contenido =
                    archivo.getBytes();

            if (
                    !firmaValida(
                            extension,
                            contenido
                    )
            ) {

                throw new IllegalArgumentException(
                        "El contenido del archivo no coincide con su formato."
                );
            }

            // =================================================
            // PDF - CONTENIDO POTENCIALMENTE PELIGROSO
            // =================================================

            if (
                    extension.equals("pdf")
            ) {

                validarPdfSeguro(
                        contenido
                );
            }

        } catch (IOException e) {

            throw new IllegalArgumentException(
                    "No fue posible validar el archivo."
            );
        }
    }


    // =========================================================
    // EXTENSIÓN
    // =========================================================

    private String obtenerExtension(
            String nombre
    ) {

        if (
                nombre == null ||
                        nombre.isBlank()
        ) {

            return "";
        }

        int posicion =
                nombre.lastIndexOf('.');

        if (
                posicion < 0 ||
                        posicion ==
                                nombre.length() - 1
        ) {

            return "";
        }

        return nombre
                .substring(
                        posicion + 1
                )
                .toLowerCase(
                        Locale.ROOT
                );
    }


    // =========================================================
    // EXTENSIÓN ↔ MIME
    // =========================================================

    private void validarMimeExtension(
            String extension,
            String mime
    ) {

        boolean valido =
                switch (extension) {

                    case "jpg", "jpeg" ->
                            mime.equals(
                                    "image/jpeg"
                            );

                    case "png" ->
                            mime.equals(
                                    "image/png"
                            );

                    case "pdf" ->
                            mime.equals(
                                    "application/pdf"
                            );

                    default ->
                            false;
                };

        if (!valido) {

            throw new IllegalArgumentException(
                    "La extensión del archivo no coincide con su tipo MIME."
            );
        }
    }


    // =========================================================
    // FIRMA REAL DEL ARCHIVO
    // =========================================================

    private boolean firmaValida(
            String extension,
            byte[] contenido
    ) {

        return switch (extension) {

            // JPEG:
            // FF D8 FF
            case "jpg", "jpeg" ->

                    contenido.length >= 3 &&

                            (contenido[0] & 0xff)
                                    == 0xff &&

                            (contenido[1] & 0xff)
                                    == 0xd8 &&

                            (contenido[2] & 0xff)
                                    == 0xff;


            // PNG:
            // 89 50 4E 47 0D 0A 1A 0A
            case "png" ->

                    contenido.length >= 8 &&

                            (contenido[0] & 0xff)
                                    == 0x89 &&

                            (contenido[1] & 0xff)
                                    == 0x50 &&

                            (contenido[2] & 0xff)
                                    == 0x4e &&

                            (contenido[3] & 0xff)
                                    == 0x47 &&

                            (contenido[4] & 0xff)
                                    == 0x0d &&

                            (contenido[5] & 0xff)
                                    == 0x0a &&

                            (contenido[6] & 0xff)
                                    == 0x1a &&

                            (contenido[7] & 0xff)
                                    == 0x0a;


            // PDF:
            // %PDF-
            case "pdf" ->

                    contenido.length >= 5 &&

                            new String(
                                    contenido,
                                    0,
                                    5,
                                    StandardCharsets.US_ASCII
                            ).equals("%PDF-");


            default ->
                    false;
        };
    }


    // =========================================================
    // SEGURIDAD BÁSICA PDF
    // =========================================================

    private void validarPdfSeguro(
            byte[] contenido
    ) {

        String texto =
                new String(
                        contenido,
                        StandardCharsets.ISO_8859_1
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        /*
         * Elementos que podrían provocar
         * ejecución de acciones o incorporar
         * otros archivos.
         */
        String[] patronesPeligrosos = {

                "/javascript",
                "/js",
                "/launch",
                "/embeddedfile",
                "/embeddedfiles",
                "/richmedia",
                "/openaction"
        };

        for (
                String patron :
                patronesPeligrosos
        ) {

            if (
                    texto.contains(
                            patron
                    )
            ) {

                throw new IllegalArgumentException(
                        "El archivo contiene contenido potencialmente inseguro."
                );
            }
        }
    }
}