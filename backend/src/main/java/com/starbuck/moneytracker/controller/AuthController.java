package com.starbuck.moneytracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.starbuck.moneytracker.dto.RegisterRequest;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.service.UserService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;

@RestController
public class AuthController {
    
    @Autowired
    private UserService userService;

    @PostMapping("/auth/register")
    public User authRegistUser(@Valid @RequestBody RegisterRequest user) {
        User userFromRequest = new User();
        userFromRequest.setUsername(user.username());
        userFromRequest.setPassword(user.password());
            // todo kódolni a jelszót
        userFromRequest.setEmail(user.email());
        userFromRequest.setUuid(java.util.UUID.randomUUID().toString());
        return userService.createUser(userFromRequest);
    }
}
