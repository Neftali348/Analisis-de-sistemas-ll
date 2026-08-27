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

    // =========================================================
    // RN07
    // Máximo permitido por archivo: 2 MB
    // =========================================================

    private static final long MAX_BYTES =
            2L * 1024 * 1024;

    private static final Set<String>
            EXTENSIONES_PERMITIDAS =
            Set.of(
                    "jpg",
                    "jpeg",
                    "png",
                    "pdf"
            );

    private static final Set<String>
            MIME_PERMITIDOS =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "application/pdf"
            );


    // =========================================================
    // VALIDACIÓN PRINCIPAL
    // =========================================================

    public void validate(
            MultipartFile archivo
    ) {

        // =====================================================
        // FA13
        // Archivo vacío, dañado o ilegible
        // =====================================================

        if (
                archivo == null ||
                        archivo.isEmpty() ||
                        archivo.getSize() <= 0
        ) {

            throw new IllegalArgumentException(
                    "El archivo está vacío, dañado o no puede ser procesado"
            );
        }


        // =====================================================
        // FA10 / AN02 No. 12
        // Tamaño máximo 2 MB
        // =====================================================

        if (
                archivo.getSize() >
                        MAX_BYTES
        ) {

            throw new IllegalArgumentException(
                    "El archivo supera el tamaño máximo permitido de 2 MB."
            );
        }


        // =====================================================
        // FA09 / AN02 No. 11
        // Extensión
        // =====================================================

        String nombreOriginal =
                Optional
                        .ofNullable(
                                archivo
                                        .getOriginalFilename()
                        )
                        .orElse("")
                        .trim();

        String extension =
                obtenerExtension(
                        nombreOriginal
                );

        if (
                !EXTENSIONES_PERMITIDAS
                        .contains(extension)
        ) {

            throw new IllegalArgumentException(
                    "El formato del archivo no está permitido."
            );
        }


        // =====================================================
        // FA09 / RN07
        // MIME
        // =====================================================

        String mime =
                Optional
                        .ofNullable(
                                archivo.getContentType()
                        )
                        .orElse("")
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (
                !MIME_PERMITIDOS
                        .contains(mime)
        ) {

            throw new IllegalArgumentException(
                    "El formato del archivo no está permitido."
            );
        }


        // =====================================================
        // EXTENSIÓN ↔ MIME
        // =====================================================

        if (
                !mimeCoincideConExtension(
                        extension,
                        mime
                )
        ) {

            throw new IllegalArgumentException(
                    "El formato del archivo no está permitido."
            );
        }


        try {

            byte[] contenido =
                    archivo.getBytes();


            // =================================================
            // FA13
            // Segunda protección contra archivo vacío
            // =================================================

            if (
                    contenido.length == 0
            ) {

                throw new IllegalArgumentException(
                        "El archivo está vacío, dañado o no puede ser procesado"
                );
            }


            // =================================================
            // FA09
            // La firma debe corresponder al formato declarado.
            // =================================================

            if (
                    !firmaInicialValida(
                            extension,
                            contenido
                    )
            ) {

                throw new IllegalArgumentException(
                        "El formato del archivo no está permitido."
                );
            }


            // =================================================
            // FA13
            // Comprobar que el archivo no parezca truncado.
            // =================================================

            if (
                    !estructuraBasicaValida(
                            extension,
                            contenido
                    )
            ) {

                throw new IllegalArgumentException(
                        "El archivo está vacío, dañado o no puede ser procesado"
                );
            }


            // =================================================
            // FA12
            // Validación básica de contenido peligroso
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
                    "El archivo está vacío, dañado o no puede ser procesado"
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
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }


    // =========================================================
    // EXTENSIÓN ↔ MIME
    // =========================================================

    private boolean mimeCoincideConExtension(
            String extension,
            String mime
    ) {

        return switch (extension) {

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
    }


    // =========================================================
    // FIRMA REAL DEL ARCHIVO
    // =========================================================

    private boolean firmaInicialValida(
            String extension,
            byte[] contenido
    ) {

        return switch (extension) {

            // =================================================
            // JPEG
            // FF D8 FF
            // =================================================

            case "jpg", "jpeg" ->

                    contenido.length >= 3 &&

                            (contenido[0] & 0xff)
                                    == 0xff &&

                            (contenido[1] & 0xff)
                                    == 0xd8 &&

                            (contenido[2] & 0xff)
                                    == 0xff;


            // =================================================
            // PNG
            // 89 50 4E 47 0D 0A 1A 0A
            // =================================================

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


            // =================================================
            // PDF
            // %PDF-
            // =================================================

            case "pdf" ->

                    contenido.length >= 5 &&

                            new String(
                                    contenido,
                                    0,
                                    5,
                                    StandardCharsets.US_ASCII
                            ).equals(
                                    "%PDF-"
                            );


            default ->
                    false;
        };
    }


    // =========================================================
    // COMPROBACIÓN BÁSICA DE ARCHIVO DAÑADO
    // =========================================================

    private boolean estructuraBasicaValida(
            String extension,
            byte[] contenido
    ) {

        return switch (extension) {

            // =================================================
            // JPEG
            // Debe terminar FF D9
            // =================================================

            case "jpg", "jpeg" ->

                    contenido.length >= 4 &&

                            (
                                    contenido[
                                            contenido.length - 2
                                            ] & 0xff
                            ) == 0xff &&

                            (
                                    contenido[
                                            contenido.length - 1
                                            ] & 0xff
                            ) == 0xd9;


            // =================================================
            // PNG
            // Debe contener el chunk IEND.
            // =================================================

            case "png" ->
                    contieneSecuencia(
                            contenido,
                            new byte[]{
                                    0x49,
                                    0x45,
                                    0x4e,
                                    0x44
                            }
                    );


            // =================================================
            // PDF
            // Debe contener marca de cierre %%EOF.
            // =================================================

            case "pdf" -> {

                String texto =
                        new String(
                                contenido,
                                StandardCharsets.ISO_8859_1
                        );

                yield texto.contains(
                        "%%EOF"
                );
            }


            default ->
                    false;
        };
    }


    // =========================================================
    // BUSCAR SECUENCIA BINARIA
    // =========================================================

    private boolean contieneSecuencia(
            byte[] contenido,
            byte[] patron
    ) {

        if (
                contenido == null ||
                        patron == null ||
                        contenido.length <
                                patron.length
        ) {

            return false;
        }

        for (
                int i = 0;
                i <=
                        contenido.length -
                                patron.length;
                i++
        ) {

            boolean coincide =
                    true;

            for (
                    int j = 0;
                    j < patron.length;
                    j++
            ) {

                if (
                        contenido[i + j] !=
                                patron[j]
                ) {

                    coincide =
                            false;

                    break;
                }
            }

            if (coincide) {
                return true;
            }
        }

        return false;
    }


    // =========================================================
    // FA12
    // SEGURIDAD BÁSICA DE PDF
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
         * Acciones o elementos que no necesitamos aceptar
         * como evidencia y que podrían producir comportamiento
         * activo dentro de un PDF.
         */
        String[] patronesPeligrosos = {

                "/javascript",
                "/js",
                "/launch",

                "/embeddedfile",
                "/embeddedfiles",

                "/richmedia",

                "/openaction",

                "/additionalactions",

                "/submitform",

                "/importdata"
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
                        "El archivo no superó la validación de seguridad"
                );
            }
        }
    }
}