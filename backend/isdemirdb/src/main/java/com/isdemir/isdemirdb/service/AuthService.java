package com.isdemir.isdemirdb.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.isdemir.isdemirdb.dto.LoginRequest;
import com.isdemir.isdemirdb.dto.LoginResponse;
import com.isdemir.isdemirdb.dto.RefreshRequest;
import com.isdemir.isdemirdb.dto.RefreshResponse;
import com.isdemir.isdemirdb.entity.RefreshToken;
import com.isdemir.isdemirdb.entity.User;
import com.isdemir.isdemirdb.exception.ForbiddenLoginException;
import com.isdemir.isdemirdb.exception.InvalidCredentialsException;
import com.isdemir.isdemirdb.repository.UserRepository;
import com.isdemir.isdemirdb.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// The whole login logic lives in this layer. The controller only calls this service.
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {

        log.info("Login denemesi: {}", request.getUsername());

        // Fetch the user by name, then compare the plain password against the stored BCrypt
        // hash. Unknown user and wrong password give the exact same 401 on purpose, so the
        // response does not reveal which usernames exist.
        User user = userRepository
                .findByUsername(request.getUsername())
                .filter(u -> passwordEncoder.matches(request.getPassword(), u.getPassword()))
                .orElseThrow(() -> new InvalidCredentialsException("Kullanıcı adı veya parola hatalı"));

        // There is a match but rol_adi is NULL/empty -> 403
        if (user.getRoleName() == null || user.getRoleName().trim().isEmpty()) {
            throw new ForbiddenLoginException("Bu kullanıcının giriş yetkisi yok");
        }

        // rol_adi is filled -> parse text like "kv1,kv3" into a list
        List<String> roles = parseRoles(user);

        // Stage 5: successful login -> produce a JWT access token.
        String accessToken = jwtService.generateAccessToken(user, roles);

        // Stage 7: produce a refresh token and store it in the refresh_tokens table.
        RefreshToken refreshToken = refreshTokenService.create(user.getId());

        log.info("Login basarili: {} (id={}, roller={})", user.getUsername(), user.getId(), roles);

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                roles,
                accessToken,
                refreshToken.getToken());
    }

    // Stage 8: produces a new access token when the refresh token is valid.
    // The refresh token itself is not rotated; only a new access token is returned.
    public RefreshResponse refresh(RefreshRequest request) {

        // Present in the DB + active + not expired? Otherwise 401.
        RefreshToken refreshToken = refreshTokenService.validateAndGet(request.getRefreshToken());

        // Load the owner of the token (the roles have to be embedded into the new access token).
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidCredentialsException("Kullanıcı bulunamadı."));

        List<String> roles = parseRoles(user);
        String accessToken = jwtService.generateAccessToken(user, roles);

        log.info("Yeni access token uretildi: {}", user.getUsername());

        return new RefreshResponse(accessToken);
    }

    // Stage 9: logout -> revoke the refresh token (aktif=false).
    public void logout(RefreshRequest request) {
        refreshTokenService.logout(request.getRefreshToken());
        log.info("Logout: refresh token pasiflestirildi");
    }

    // Turns the rol_adi text ("kv1,kv3") into a role list. NULL/empty gives an empty list.
    private List<String> parseRoles(User user) {
        if (user.getRoleName() == null) {
            return List.of();
        }
        return Arrays.stream(user.getRoleName().split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .collect(Collectors.toList());
    }
}
