package com.starbuck.moneytracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    /**
     * Jelszó kódoló bean
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Új felhasználó regisztrációja. Siker esetén 201-es választ ad vissza, hiba esetén 400-as választ ad vissza a validációs hibák miatt.
     * 
     * @param RegisterRequest user
     */
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void authRegistUser(@Valid @RequestBody RegisterRequest user) {
        User newUser = new User();
        newUser.setUsername(user.username());
        newUser.setPassword(this.passwordEncoder.encode(user.password()));
        newUser.setEmail(user.email());
        newUser.setUuid(java.util.UUID.randomUUID().toString());
        try {
            userService.createUser(newUser);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("User registration failed: " + e.getMessage());
        }
    }
}
