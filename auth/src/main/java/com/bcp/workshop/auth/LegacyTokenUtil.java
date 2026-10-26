package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@ApplicationScoped
public class LegacyTokenUtil {
    @ConfigProperty(name = "auth.token.secret")
    String secret;

    public String create(String username, String role) {
        long expiry = Instant.now().plusSeconds(AuthConstants.TOKEN_EXPIRY_SECONDS).getEpochSecond();
        String value = username + "|" + role + "|" + expiry + "|" + secret;
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
