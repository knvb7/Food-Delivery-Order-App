package com.dmg.fooddelivery.repository;

import com.dmg.fooddelivery.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query(
            value = "SELECT COUNT(*) > 0 FROM app_users WHERE username = :username",
            nativeQuery = true)
    boolean existsByUsername(@Param("username") String username);

    @Query(value = "SELECT * FROM app_users WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<User> findLockedById(@Param("id") long id);
}
