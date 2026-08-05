package com.isdemir.isdemirdb.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

// Stage 3: the Spring Security setup.
// Stage 6: the JWT filter was added to the chain; sessions are STATELESS and tokens fill the
//          SecurityContext.
// Stage 10: the endpoints were locked down -> /auth/** is open, /api/** (heat, material,
//           event) REQUIRES a valid token. A request without a token -> 401
//           (JwtAuthenticationEntryPoint).
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS stays enabled so the @CrossOrigin settings on the controllers keep working
                .cors(Customizer.withDefaults())
                // CSRF is not needed for a REST API (so POSTs can be sent from Postman)
                .csrf(csrf -> csrf.disable())
                // We use JWT; no session is kept on the server
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Shared 401 JSON response on unauthorized access
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                // Access rules (stage 10):
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight requests (browser) are free
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // The authentication endpoints are open to everyone
                        .requestMatchers("/auth/**").permitAll()
                        // Tomcat forwards failures (400/404/500) to /error as a NEW dispatch where
                        // the Authorization header is no longer read; without this the real error
                        // would be masked by a 401.
                        .requestMatchers("/error").permitAll()
                        // Everything else (heat, material, event...) requires a token
                        .anyRequest().authenticated())
                // Run our own JWT filter before the standard authentication filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
