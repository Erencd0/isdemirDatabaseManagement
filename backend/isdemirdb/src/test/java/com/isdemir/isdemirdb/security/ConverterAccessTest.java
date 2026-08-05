package com.isdemir.isdemirdb.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

// The role -> converter mapping and the write guard built on it.
class ConverterAccessTest {

    private final ConverterAccess converterAccess = new ConverterAccess();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mapsOnlyConverterRoles() {
        assertEquals(List.of(1, 3), ConverterAccess.convertersOf(List.of("kv1", "admin", "kv3")));
        assertEquals(List.of(), ConverterAccess.convertersOf(List.of("admin")));
    }

    @Test
    void readsConverterFromHeatNo() {
        assertEquals(1, ConverterAccess.converterOfHeatNo(6100001));
        assertEquals(3, ConverterAccess.converterOfHeatNo(6300042));
    }

    // The reported bug: a kv3-only user posting to a kv1 heat (6100001).
    @Test
    void rejectsForeignConverter() {
        login("kv3");
        assertDoesNotThrow(() -> converterAccess.require(3));
        assertThrows(AccessDeniedException.class,
                () -> converterAccess.require(ConverterAccess.converterOfHeatNo(6100001)));
    }

    // No token at all -> nothing is allowed (not everything).
    @Test
    void rejectsWhenNotAuthenticated() {
        assertThrows(AccessDeniedException.class, () -> converterAccess.require(1));
    }

    private void login(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test", null,
                        List.of(roles).stream().map(SimpleGrantedAuthority::new).toList()));
    }
}
