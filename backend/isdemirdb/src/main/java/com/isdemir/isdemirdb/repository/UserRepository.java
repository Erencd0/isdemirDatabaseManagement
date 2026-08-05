package com.isdemir.isdemirdb.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.isdemir.isdemirdb.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Finds the row matching kullanici_adi and kullanici_parola
    Optional<User> findByUsernameAndPassword(String username, String password);
}
