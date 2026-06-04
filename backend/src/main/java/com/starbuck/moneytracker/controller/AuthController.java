package com.starbuck.moneytracker.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.starbuck.moneytracker.dto.LoginRequest;
import com.starbuck.moneytracker.dto.RegisterRequest;
import com.starbuck.moneytracker.dto.UsernameEmailRequest;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
public class AuthController {

    // Authentikációs cookie neve
    private static final String AUTH_COOKIE_NAME = "AUTH_TOKEN";

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
    public void authRegisterUser(@Valid @RequestBody RegisterRequest user) {
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
     * @return ['message': 'Login successful'] + JWT token egy HttpOnly cookie-ban
     */
    @PostMapping(path = "/auth/login")
    public ResponseEntity<Map<String,String>> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            String jwtToken = userService.login(loginRequest.username(), loginRequest.password());
            ResponseCookie cookie = ResponseCookie.from(AUTH_COOKIE_NAME, jwtToken)
                .httpOnly(true)
                .secure(true) // csak HTTPS-en keresztül küldhető
                .path("/") // Az egész oldal a szkópja a sütinek
                .maxAge(7 * 24 * 60 * 60) // 7 nap
                .sameSite("Strict") // CSRF védelem
                .build();
            HttpHeaders headers = new HttpHeaders();
            headers.add("Set-Cookie", cookie.toString());
            return new ResponseEntity<Map<String,String>>(Map.of("message", "Login successful"), headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<Map<String,String>>(Map.of("message", "Invalid username or password"), HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Felhasználó kiejlentkeztetése (jwt token törlése a böngészőből)
     * 
     * @param HttpServletResponse response
     * @return ['message': 'Logged out'] + JWT token egy lejárt HttpOnly cookie-ban, ami a böngészőből törli a sütit
     */
    @PostMapping(path = "/auth/logout")
    public ResponseEntity<Map<String,String>> logoutUser(HttpServletResponse response) {
        ResponseCookie expiredCookie = ResponseCookie.from(AUTH_COOKIE_NAME, "")
            .path("/")
            .httpOnly(true)
            .secure(true)
            .maxAge(0)
            .sameSite("Strict")
            .build();

        response.setHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
        return ResponseEntity.ok(Map.of("message", "Logged out"));
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
