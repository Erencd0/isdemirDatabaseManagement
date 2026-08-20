package com.isdemir.isdemirdb.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.isdemir.isdemirdb.entity.User;
import com.isdemir.isdemirdb.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// There is no registration endpoint: users are added by hand with an INSERT, so their password
// lands in the table as plain text. On every boot this converts any such row into a BCrypt hash.
// Rows that are already hashed are skipped, so running it repeatedly is harmless.
// Kapali on the supabase profile: that database is shared with the mobile app, which reads
// the passwords as plain text. Hashing them here would lock the mobile app out.
@Profile("!supabase")
@Component
@Slf4j
@RequiredArgsConstructor
public class PasswordHashInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        for (User user : userRepository.findAll()) {
            if (isHashed(user.getPassword())) {
                continue;
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepository.save(user);
            log.warn("Duz metin parola hashlendi: {}", user.getUsername());
        }
    }

    // A BCrypt hash always starts with $2a$ / $2b$ / $2y$ and is 60 characters long.
    static boolean isHashed(String password) {
        return password != null && password.startsWith("$2") && password.length() == 60;
    }
}
