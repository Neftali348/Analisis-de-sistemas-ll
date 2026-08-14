package com.umg.sgq.utilidad;

import jakarta.servlet.http.HttpServletRequest;

public final class UtilidadPeticion {
    private UtilidadPeticion() {
    }

    public static String ip(HttpServletRequest r) {
        String x = r.getHeader("X-Forwarded-For");
        if (x != null && !x.isBlank()) return x.split(",")[0].trim();
        return r.getRemoteAddr() == null ? "UNKNOWN" : r.getRemoteAddr();
    }
}
