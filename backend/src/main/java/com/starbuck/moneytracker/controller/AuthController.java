package com.starbuck.moneytracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.starbuck.moneytracker.dto.LoginRequest;
import com.starbuck.moneytracker.dto.LoginResponse;
import com.starbuck.moneytracker.dto.RegisterRequest;
import com.starbuck.moneytracker.dto.UsernameEmailRequest;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.service.UserService;
import jakarta.validation.Valid;

@RestController
public class AuthController {
    
    /**
     * Felhasználó autentikációs szolgáltatás
     */
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
    @PostMapping(path = "/auth/register")
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

    /**
     * Felhasználó beléptetése
     * 
     * @param username
     * @param password
     * @return LoginResponse, amiben a jwt token van
     */
    @PostMapping(path = "/auth/login")
    public LoginResponse loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            String jwtToken = userService.login(loginRequest.username(), loginRequest.password());
            return new LoginResponse(jwtToken);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("User login failed: " + e.getMessage());
        }
    }

    /**
     * Ellenőrzi, hogy a felhasználónév, vagy email cím foglalt-e már
     * 
     * @param username
     * @param email
     * @return Boolean
     */
    @PostMapping(path = "/auth/isUsernameOrEmailExists")
    public boolean usernameOrEmailExists(@RequestBody UsernameEmailRequest request) {
        return userService.usernameOrEmailExists(request.username(), request.email());
    }
}
