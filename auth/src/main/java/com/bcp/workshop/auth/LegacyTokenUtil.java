package com.bcp.workshop.auth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class LegacyTokenUtil {
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_LIFETIME_SECONDS = 3600;

    private LegacyTokenUtil() {}

    public static String create(String username, String role, String secret) throws GeneralSecurityException {
        long expiry = Instant.now().plusSeconds(TOKEN_LIFETIME_SECONDS).getEpochSecond();
        String payload = username + "|" + role + "|" + expiry;
        String value = payload + "|" + sign(payload, secret);
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String payload, String secret) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM));
        byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(signature);
    }
}
