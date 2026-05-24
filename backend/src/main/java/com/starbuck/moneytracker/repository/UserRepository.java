package com.starbuck.moneytracker.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.starbuck.moneytracker.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmailOrUsername(String email, String username);
    User findByUsername(String username);
}
