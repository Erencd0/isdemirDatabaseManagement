package com.isdemir.isdemirdb.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

// Kicks in when a protected endpoint is reached without a valid token: sends JSON in the
// shared format instead of a bare 401. (The lockdown became active in stage 10.)
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        // Which URL got rejected: the JSON body alone does not say.
        log.warn("401 Unauthorized: {} {} - {}", request.getMethod(), request.getRequestURI(),
                authException.getMessage());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"message\":\"Yetkisiz erişim. Geçerli bir token gerekli.\"}");
    }
}
