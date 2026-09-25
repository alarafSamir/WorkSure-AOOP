package com.worksure.util;

import java.security.SecureRandom;
import java.time.LocalDate;

public final class Invoices {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Invoices() {}

    public static String generate(String prefix) {
        byte[] buf = new byte[4];
        RANDOM.nextBytes(buf);
        StringBuilder hex = new StringBuilder();
        for (byte b : buf) {
            hex.append(String.format("%02X", b));
        }
        LocalDate d = LocalDate.now();
        return prefix + "-" + d.getYear()
                + String.format("%02d", d.getMonthValue())
                + String.format("%02d", d.getDayOfMonth())
                + "-" + hex;
    }

    public static String generate() {
        return generate("WS");
    }
}
