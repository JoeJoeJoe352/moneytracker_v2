package com.starbuck.moneytracker.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.starbuck.moneytracker.dto.LoginRequest;
import com.starbuck.moneytracker.dto.RegisterRequest;
import com.starbuck.moneytracker.dto.UserDataResponseDto;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.service.UserService;
import com.starbuck.moneytracker.util.CurrentUserUtil;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
public class AuthController {

    // Authentikációs cookie neve
    public static final String AUTH_COOKIE_NAME = "AUTH_TOKEN";

    /**
     * Felhasználó autentikációs szolgáltatás
     */
    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUserUtil userUtil;

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
        
        newUser.setEmail(user.email());
        try {
            userService.createUser(newUser, user.password());
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
     * Ellenőrzi, hogy a felhasználónév foglalt-e már
     * 
     * @param username
     * @return Boolean
     */
    @PostMapping(path = "/auth/isUsernameExists")
    public boolean isUsernameExists(@RequestBody Map<String, String> body) {
        return userService.isUsernameExists(body.get("username"));
    }

    /**
     * Ellenőrzi, hogy az email cím foglalt-e már
     * 
     * @param email
     * @return Boolean
     */
    @PostMapping(path = "/auth/isEmailExists")
    public boolean isEmailExists(@RequestBody Map<String, String> body) {
        return userService.isEmailExists(body.get("email"));
    }

    /**
     * User alapadatokkal tér vissza. Gyakorlatilag a bejelentkezés tényét dönti el
     * Ha be van loginolva a user, akkor visszaadja az adatokat, egyébként el sem éri ezt a végpontot
     * Így pl.: frontend újrabetöltéskor azonnal le tudja ellenőrizni a frontend, hogy be vagyunk-e jelentkezve
     * 
     * @return
     */
    @PostMapping(path = "auth/authenticateUser")
    public UserDataResponseDto authenticateUser() {
        User user = this.userUtil.getUser();

        return new UserDataResponseDto(user.getUsername());
    }
}
