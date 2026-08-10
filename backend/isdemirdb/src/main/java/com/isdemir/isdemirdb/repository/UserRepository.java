package com.isdemir.isdemirdb.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.isdemir.isdemirdb.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Finds the row by kullanici_adi. The password is compared with BCrypt in AuthService;
    // it can no longer be matched with a WHERE clause because every hash carries its own salt.
    Optional<User> findByUsername(String username);
}
