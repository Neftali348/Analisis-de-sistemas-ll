package com.umg.sgq.servicio;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ServicioSeguridadArchivos {
    private static final long MAX = 2L * 1024 * 1024;
    private static final Set<String> EXT = Set.of("jpg", "jpeg", "png", "pdf");
    private static final Set<String> MIME = Set.of("image/jpeg", "image/png", "application/pdf");

    public void validate(MultipartFile f) {
        if (f == null || f.isEmpty()) throw new IllegalArgumentException("El archivo está vacío.");
        if (f.getSize() > MAX)
            throw new IllegalArgumentException("El archivo supera el tamaño máximo permitido de 2 MB.");
        String name = Optional.ofNullable(f.getOriginalFilename()).orElse("archivo");
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!EXT.contains(ext)) throw new IllegalArgumentException("El formato del archivo no está permitido.");
        String mime = Optional.ofNullable(f.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        if (!MIME.contains(mime)) throw new IllegalArgumentException("El tipo MIME del archivo no está permitido.");
        try {
            byte[] b = f.getBytes();
            boolean signature;
            switch (ext) {
                case "jpg":
                case "jpeg":
                    signature = b.length > 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff;
                    break;
                case "png":
                    signature = b.length > 8 && (b[0] & 0xff) == 0x89 && b[1] == 0x50 && b[2] == 0x4e && b[3] == 0x47;
                    break;
                case "pdf":
                    signature = b.length > 5 && new String(b, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
                    break;
                default:
                    signature = false;
            }
            if (!signature) throw new IllegalArgumentException("El contenido del archivo no coincide con su formato.");
            if (ext.equals("pdf")) {
                String text = new String(b, StandardCharsets.ISO_8859_1).toLowerCase(Locale.ROOT);
                if (text.contains("/javascript") || text.contains("/js") || text.contains("/launch") || text.contains("/embeddedfile"))
                    throw new IllegalArgumentException("El archivo contiene contenido potencialmente inseguro.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No fue posible validar el archivo.");
        }
    }
}
