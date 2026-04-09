package com.hotel.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public final class PasswordUtil {
    private static final SecureRandom RNG = new SecureRandom();

    private PasswordUtil() {}

    public static String generateSaltBase64(int bytes) {
        byte[] salt = new byte[bytes];
        RNG.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String pbkdf2HashBase64(char[] password, String saltBase64, int iterations, int keyLengthBits) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        try {
            SecretKeyFactory skf = getFactory();
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("Invalid key spec", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean verify(char[] password, String saltBase64, int iterations, int keyLengthBits, String expectedHashBase64) {
        String actual = pbkdf2HashBase64(password, saltBase64, iterations, keyLengthBits);
        return constantTimeEquals(actual, expectedHashBase64);
    }

    private static SecretKeyFactory getFactory() {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            try {
                return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException("PBKDF2 not available", ex);
            }
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] ba = a.getBytes();
        byte[] bb = b.getBytes();
        if (ba.length != bb.length) return false;
        int result = 0;
        for (int i = 0; i < ba.length; i++) {
            result |= ba[i] ^ bb[i];
        }
        return result == 0;
    }
}
