package com.fraudshield.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Salted SHA-256 password hashing. Stored format: salt$hexSha256(salt + password).
 * (Academic simulation: a production system would use bcrypt / PBKDF2 / Argon2.)
 */
public final class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String password) {
        byte[] saltBytes = new byte[8];
        RANDOM.nextBytes(saltBytes);
        String salt = toHex(saltBytes);
        return salt + "$" + sha256(salt + password);
    }

    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) return false;
        int idx = stored.indexOf('$');
        if (idx < 1) return false;
        String salt = stored.substring(0, idx);
        String expected = stored.substring(idx + 1);
        return MessageDigest.isEqual(
                sha256(salt + password).getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));   // constant-time comparison
    }

    private static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return toHex(md.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
