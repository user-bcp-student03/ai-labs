package com.bcp.workshop.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@ApplicationScoped
public class JwtTokenService {
    private static final int HS256_MINIMUM_KEY_BYTES = 32;
    private static final String ROLE_CLAIM = "role";
    private static final String INVALID_TOKEN_MESSAGE = "Invalid authentication token";
    private static final String SIGNING_ERROR_MESSAGE = "Unable to sign authentication token";

    private final byte[] secret;

    @Inject
    public JwtTokenService(@ConfigProperty(name = "auth.token.secret") String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < HS256_MINIMUM_KEY_BYTES) {
            throw new IllegalArgumentException("The authentication signing key must be at least 32 bytes");
        }
        this.secret = secretBytes;
    }

    public String create(String username, String role) {
        Instant issuedAt = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(username)
                .claim(ROLE_CLAIM, role)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plusSeconds(AuthConstants.TOKEN_EXPIRY_SECONDS)))
                .build();
        SignedJWT token = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                claims
        );

        try {
            token.sign(new MACSigner(secret));
            return token.serialize();
        } catch (JOSEException error) {
            throw new IllegalStateException(SIGNING_ERROR_MESSAGE, error);
        }
    }

    public VerifiedToken verify(String token) {
        if (token == null || token.isBlank()) {
            throw invalidToken();
        }

        try {
            SignedJWT signedToken = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(signedToken.getHeader().getAlgorithm())
                    || !JOSEObjectType.JWT.equals(signedToken.getHeader().getType())
                    || !signedToken.verify(new MACVerifier(secret))) {
                throw invalidToken();
            }

            JWTClaimsSet claims = signedToken.getJWTClaimsSet();
            String username = claims.getSubject();
            String role = claims.getStringClaim(ROLE_CLAIM);
            Date issuedAtDate = claims.getIssueTime();
            Date expiresAtDate = claims.getExpirationTime();
            if (username == null || username.isBlank() || role == null || role.isBlank()
                    || issuedAtDate == null || expiresAtDate == null) {
                throw invalidToken();
            }

            Instant issuedAt = issuedAtDate.toInstant();
            Instant expiresAt = expiresAtDate.toInstant();
            Instant now = Instant.now();
            if (issuedAt.isAfter(now) || !expiresAt.isAfter(now) || !expiresAt.isAfter(issuedAt)
                    || Duration.between(issuedAt, expiresAt)
                    .compareTo(Duration.ofSeconds(AuthConstants.TOKEN_EXPIRY_SECONDS)) > 0) {
                throw invalidToken();
            }

            return new VerifiedToken(username, role, issuedAt, expiresAt);
        } catch (ParseException | JOSEException error) {
            throw invalidToken();
        }
    }

    private InvalidTokenException invalidToken() {
        return new InvalidTokenException(INVALID_TOKEN_MESSAGE);
    }

    public record VerifiedToken(String username, String role, Instant issuedAt, Instant expiresAt) {}

    public static final class InvalidTokenException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private InvalidTokenException(String message) {
            super(message);
        }
    }
}
