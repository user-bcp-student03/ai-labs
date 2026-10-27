package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void hashesWithSaltAndVerifiesOnlyMatchingPassword() throws Exception {
        String firstHash = passwordHasher.hash("Workshop123!");
        String secondHash = passwordHasher.hash("Workshop123!");

        assertNotEquals(firstHash, secondHash);
        assertTrue(passwordHasher.matches("Workshop123!", firstHash));
        assertFalse(passwordHasher.matches("wrong-password", firstHash));
    }

    @Test
    void rejectsMalformedHashes() throws Exception {
        assertFalse(passwordHasher.matches("Workshop123!", "pbkdf2-sha256$not-a-number$salt$key"));
        assertFalse(passwordHasher.matches("Workshop123!", "pbkdf2-sha256$999999999$salt$key"));
    }
}
