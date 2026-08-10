package com.isdemir.isdemirdb.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// The password hashing contract: what goes into kullanici_parola and what login compares against.
class PasswordHashTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void hashIsSaltedAndVerifiable() {
        String hash = encoder.encode("isdemir123");

        // The hash is not the password, and two hashes of the same password differ (random salt)
        assertFalse(hash.equals("isdemir123"));
        assertFalse(hash.equals(encoder.encode("isdemir123")));

        // ...yet both verify, because the salt is stored inside the hash
        assertTrue(encoder.matches("isdemir123", hash));
        assertFalse(encoder.matches("isdemir124", hash));
    }

    @Test
    void hashFitsTheColumn() {
        // kullanici_parola is varchar(72); BCrypt is always 60 characters
        assertEquals(60, encoder.encode("1234").length());
    }

    @Test
    void initializerOnlyHashesPlainText() {
        assertFalse(PasswordHashInitializer.isHashed("1234"));
        assertFalse(PasswordHashInitializer.isHashed(null));
        assertTrue(PasswordHashInitializer.isHashed(encoder.encode("1234")));
    }
}
