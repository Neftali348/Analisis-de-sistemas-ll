package com.umg.sgq.utilidad;

import java.security.SecureRandom;

public final class UtilidadClaveAleatoria {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom R = new SecureRandom();

    private UtilidadClaveAleatoria() {
    }

    public static String trackingKey() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 10; i++) s.append(CHARS.charAt(R.nextInt(CHARS.length())));
        return s.toString();
    }

    public static String tempPassword() {
        return "Sgq!" + trackingKey() + "9a";
    }
}
