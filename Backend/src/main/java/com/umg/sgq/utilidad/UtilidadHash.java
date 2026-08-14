package com.umg.sgq.utilidad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class UtilidadHash {
    private UtilidadHash() {
    }

    public static String sha256(String value) {
        return sha256Bytes(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Bytes(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible calcular hash", e);
        }
    }
}
