package com.bcp.workshop.auth;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {
    private static final String TEST_SECRET =
            "unit-test-secret-for-HS256-signing-and-verification-with-sufficient-length";

    private final JwtTokenService tokenService = new JwtTokenService(TEST_SECRET);

    @Test
    void createsAndVerifiesJwtWithRequiredClaimsAndExpiry() {
        String token = tokenService.create("student", "STUDENT");

        JwtTokenService.VerifiedToken claims = tokenService.verify(token);

        assertEquals("student", claims.username());
        assertEquals("STUDENT", claims.role());
        assertEquals(3600, Duration.between(claims.issuedAt(), claims.expiresAt()).getSeconds());
    }

    @Test
    void rejectsTamperedPayload() {
        String token = tokenService.create("student", "STUDENT");
        String[] parts = token.split("\\.");
        parts[1] = (parts[1].charAt(0) == 'A' ? "B" : "A") + parts[1].substring(1);

        assertThrows(JwtTokenService.InvalidTokenException.class,
                () -> tokenService.verify(String.join(".", parts)));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        Instant issuedAt = Instant.now().minusSeconds(7200);
        String token = signedToken(new JWTClaimsSet.Builder()
                .subject("student")
                .claim("role", "STUDENT")
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plusSeconds(AuthConstants.TOKEN_EXPIRY_SECONDS)))
                .build());

        assertThrows(JwtTokenService.InvalidTokenException.class, () -> tokenService.verify(token));
    }

    @Test
    void rejectsTokenMissingRequiredClaim() throws Exception {
        Instant issuedAt = Instant.now();
        String token = signedToken(new JWTClaimsSet.Builder()
                .subject("student")
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plusSeconds(AuthConstants.TOKEN_EXPIRY_SECONDS)))
                .build());

        assertThrows(JwtTokenService.InvalidTokenException.class, () -> tokenService.verify(token));
    }

    @Test
    void rejectsTokenWithBlankRoleClaim() throws Exception {
        Instant issuedAt = Instant.now();
        String token = signedToken(new JWTClaimsSet.Builder()
                .subject("student")
                .claim("role", " ")
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plusSeconds(AuthConstants.TOKEN_EXPIRY_SECONDS)))
                .build());

        assertThrows(JwtTokenService.InvalidTokenException.class, () -> tokenService.verify(token));
    }

    @Test
    void rejectsTokenIssuedInTheFuture() throws Exception {
        Instant issuedAt = Instant.now().plusSeconds(60);
        String token = signedToken(new JWTClaimsSet.Builder()
                .subject("student")
                .claim("role", "STUDENT")
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plusSeconds(AuthConstants.TOKEN_EXPIRY_SECONDS)))
                .build());

        assertThrows(JwtTokenService.InvalidTokenException.class, () -> tokenService.verify(token));
    }

    @Test
    void rejectsTokenLifetimeLongerThanConfiguredLimit() throws Exception {
        Instant issuedAt = Instant.now();
        String token = signedToken(new JWTClaimsSet.Builder()
                .subject("student")
                .claim("role", "STUDENT")
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plusSeconds(AuthConstants.TOKEN_EXPIRY_SECONDS + 1L)))
                .build());

        assertThrows(JwtTokenService.InvalidTokenException.class, () -> tokenService.verify(token));
    }

    @Test
    void rejectsTokenWithUnsupportedAlgorithm() throws Exception {
        Instant issuedAt = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("student")
                .claim("role", "STUDENT")
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plusSeconds(AuthConstants.TOKEN_EXPIRY_SECONDS)))
                .build();

        assertThrows(JwtTokenService.InvalidTokenException.class,
                () -> tokenService.verify(signedToken(JWSAlgorithm.HS384, claims)));
    }

    @Test
    void rejectsBlankOrMalformedToken() {
        assertThrows(JwtTokenService.InvalidTokenException.class, () -> tokenService.verify(" "));
        assertThrows(JwtTokenService.InvalidTokenException.class, () -> tokenService.verify("not-a-jwt"));
    }

    @Test
    void rejectsSigningSecretShorterThanHs256Minimum() {
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenService("short-secret"));
    }

    private String signedToken(JWTClaimsSet claims) throws Exception {
        return signedToken(JWSAlgorithm.HS256, claims);
    }

    private String signedToken(JWSAlgorithm algorithm, JWTClaimsSet claims) throws Exception {
        SignedJWT signedToken = new SignedJWT(
                new JWSHeader.Builder(algorithm).type(JOSEObjectType.JWT).build(),
                claims
        );
        signedToken.sign(new MACSigner(TEST_SECRET.getBytes(StandardCharsets.UTF_8)));
        return signedToken.serialize();
    }
}
