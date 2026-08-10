package com.isdemir.isdemirdb.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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

    // Passwords are stored as BCrypt hashes in kullanici.kullanici_parola.
    // BCrypt generates its own random salt and embeds it in the hash, so the same password
    // produces a different string every time -> the hash cannot be looked up with a WHERE clause.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Tek CORS ayari. Daha once her controller'da ayri @CrossOrigin listesi vardi; yeni bir
    // origin eklemek 4 dosyayi duzenlemek demekti ve Docker'a gecince (localhost:3000)
    // hepsi unutuldu -> tarayicidan gelen her istek 403 "Invalid CORS request" aldi.
    //
    // 3000 = Docker'daki nginx, 5173/5174 = vite dev sunucusu.
    // Not: nginx uzerinden giden istekler zaten ayni origin'de, bu liste dogrudan
    // backend'e (8080) baglanan tarayicilar icin de gerekli.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:5174"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Yukaridaki corsConfigurationSource bean'ini kullanir.
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
