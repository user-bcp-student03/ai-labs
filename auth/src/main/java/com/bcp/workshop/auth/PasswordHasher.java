package com.bcp.workshop.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String ENCODING_PREFIX = "pbkdf2-sha256";
    private static final int ITERATION_COUNT = 120_000;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;

    private final SecureRandom secureRandom;

    PasswordHasher() {
        this(new SecureRandom());
    }

    PasswordHasher(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    String hash(String password) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        byte[] derivedKey = deriveKey(password, salt, ITERATION_COUNT);
        return ENCODING_PREFIX + "$" + ITERATION_COUNT + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derivedKey);
    }

    boolean isEncodedHash(String storedPassword) {
        return storedPassword != null && storedPassword.startsWith(ENCODING_PREFIX + "$");
    }

    boolean matches(String password, String encodedHash) throws GeneralSecurityException {
        String[] parts = encodedHash.split("\\$", -1);
        if (parts.length != 4 || !ENCODING_PREFIX.equals(parts[0])) {
            return false;
        }

        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException malformedIterationCount) {
            return false;
        }
        if (iterations < 1 || iterations > ITERATION_COUNT) {
            return false;
        }

        byte[] salt;
        byte[] expectedKey;
        try {
            salt = Base64.getDecoder().decode(parts[2]);
            expectedKey = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException malformedHash) {
            return false;
        }
        byte[] actualKey = deriveKey(password, salt, iterations);
        return MessageDigest.isEqual(expectedKey, actualKey);
    }

    boolean matchesLegacyPassword(String password, String storedPassword) {
        return MessageDigest.isEqual(
                password.getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8)
        );
    }

    private byte[] deriveKey(String password, byte[] salt, int iterations) throws GeneralSecurityException {
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(keySpec).getEncoded();
        } finally {
            keySpec.clearPassword();
        }
    }
}
