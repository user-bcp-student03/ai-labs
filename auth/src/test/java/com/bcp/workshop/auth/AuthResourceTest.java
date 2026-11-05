package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class AuthResourceTest {
    // Spec "Successful login preserves the login response contract".
    @Test
    void logsInWithWorkshopUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("token", notNullValue());
    }

    // Spec "Invalid credentials cannot authenticate a user": ordinary wrong password is 401 without token.
    @Test
    void rejectsWrongPasswordWithoutToken() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"WrongPassword\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("token", nullValue());
    }

    // Spec "Invalid credentials cannot authenticate a user": ordinary unknown user is 401 without token.
    @Test
    void rejectsUnknownUserWithoutToken() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"nobody\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("token", nullValue());
    }

    // Spec "Injection-like username is rejected" (Issue #23).
    @Test
    void rejectsInjectionLikeUsernameWithoutToken() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"' OR '1'='1\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("token", nullValue());
    }

    // Spec "Injection-like password is rejected" (Issue #23).
    @Test
    void rejectsInjectionLikePasswordWithoutToken() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"' OR '1'='1\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("token", nullValue());
    }

    // Spec "Missing or blank credentials are rejected": empty and whitespace-only username give 400.
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankUsername(String blankUsername) {
        given()
                .contentType("application/json")
                .body("{\"username\":\"" + blankUsername + "\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("token", nullValue());
    }

    // Spec "Missing or blank credentials are rejected": empty and whitespace-only password give 400.
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankPassword(String blankPassword) {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"" + blankPassword + "\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("token", nullValue());
    }
}
