package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class AuthResourceTest {
    @Test
    void logsInWithWorkshopUserAndReturnsSignedToken() throws Exception {
        String encodedToken = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("expiresIn", is(3600))
                .body("token", notNullValue())
                .extract().path("token");
        String[] parts = new String(Base64.getDecoder().decode(encodedToken), StandardCharsets.UTF_8)
                .split("\\|", -1);
        assertEquals(4, parts.length);
        assertNotEquals("WORKSHOP_SECRET_2026", parts[3]);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                "workshop-test-only-secret-not-for-production".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        String expectedSignature = HexFormat.of().formatHex(
                mac.doFinal((parts[0] + "|" + parts[1] + "|" + parts[2]).getBytes(StandardCharsets.UTF_8))
        );
        assertEquals(expectedSignature, parts[3]);
    }
}
