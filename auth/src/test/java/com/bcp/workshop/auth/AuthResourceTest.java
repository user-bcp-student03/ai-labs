package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
class AuthResourceTest {
    private static final String TEST_SECRET = "unit-test-secret-for-HS256-signing-and-verification";

    @Inject
    JwtTokenService tokenService;

    @Test
    void logsInWithWorkshopUser() {
        String token = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("token", notNullValue())
                .body("expiresIn", is(3600))
                .body("debug", is("authenticated-with-legacy-flow"))
                .extract().path("token");

        JwtTokenService.VerifiedToken claims = tokenService.verify(token);
        assertEquals("student", claims.username());
        assertEquals("STUDENT", claims.role());
        assertEquals(3600, Duration.between(claims.issuedAt(), claims.expiresAt()).getSeconds());
    }

    @Test
    void rejectsInvalidCredentials() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"incorrect\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user student or password"));
    }

    @Test
    void rejectsMissingUsername() {
        given()
                .contentType("application/json")
                .body("{\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsBlankUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"  \",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsMissingPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsBlankPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"  \"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void mapsNullRequestToBadRequest() {
        given()
                .contentType("application/json")
                .body("null")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Request is null"));
    }

    @Test
    void treatsQuoteInUsernameAsLiteralInput() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student'\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user student' or password"));
    }

    @Test
    void returnsGenericServerErrorWithoutInternalDetails() {
        AuthResource resource = new AuthResource();
        resource.loginService = new LoginService(
                new LegacyDatabase(),
                new JwtTokenService(TEST_SECRET)
        ) {
            @Override
            public LoginOutcome login(LoginRequest loginRequest) {
                throw new IllegalStateException("simulated-database-detail");
            }
        };
        LoginRequest request = new LoginRequest();
        request.username = "student";
        request.password = "not-a-real-password";

        Response response = resource.login(request);
        Map<?, ?> body = (Map<?, ?>) response.getEntity();

        assertEquals(500, response.getStatus());
        assertEquals("Authentication failed", body.get("message"));
        assertFalse(body.containsKey("exception"));
    }

    @Test
    void allowsConfiguredCorsOrigin() {
        given()
                .header("Origin", "http://localhost:4200")
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"incorrect\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .header("Access-Control-Allow-Origin", is("http://localhost:4200"));
    }

    @Test
    void rejectsUnconfiguredCorsOrigin() {
        given()
                .header("Origin", "https://untrusted.example")
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"incorrect\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue());
    }

    @Test
    void reportsHealthyStatusWithoutAuthentication() {
        given()
                .accept("application/json")
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("status", is("UP"))
                .body("checks.size()", is(0));
    }

    @Test
    void healthEndpointDoesNotRequireAuthorizationHeader() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }
}
