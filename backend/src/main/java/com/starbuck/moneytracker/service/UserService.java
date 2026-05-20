package com.starbuck.moneytracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.repository.UserRepository;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;

    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setUuid(java.util.UUID.randomUUID().toString());
        userRepository.save(user);
        return user;
    }
}
