package com.starbuck.moneytracker.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.starbuck.moneytracker.commands.UserCreateCommand;
import com.starbuck.moneytracker.commands.UserLoginCommand;
import com.starbuck.moneytracker.dto.IsEmailExistsDto;
import com.starbuck.moneytracker.dto.IsUsernameExistsDto;
import com.starbuck.moneytracker.dto.LoginRequestDto;
import com.starbuck.moneytracker.dto.RegisterRequestDto;
import com.starbuck.moneytracker.dto.UserDataResponseDto;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.mapper.UserMapper;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.service.UserService;
import com.starbuck.moneytracker.util.CookieUtil;
import com.starbuck.moneytracker.util.CurrentUserUtil;

import jakarta.validation.Valid;

@RestController
public class AuthController {

    /**
     * Felhasználó autentikációs szolgáltatás
     */
    private final UserService userService;
    private final CurrentUserUtil userUtil;
    private final CookieUtil cookieUtil;
    private final UserMapper mapper;
    private final WalletRepository walletRepository;

    public AuthController(UserService userService, CurrentUserUtil userUtil, CookieUtil cookieUtil, UserMapper mapper,
            WalletRepository walletRepository) {
        this.userService = userService;
        this.userUtil = userUtil;
        this.cookieUtil = cookieUtil;
        this.mapper = mapper;
        this.walletRepository = walletRepository;
    }

    /**
     * Új felhasználó regisztrációja. Siker esetén 201-es választ ad vissza, hiba
     * esetén 400-as választ ad vissza a validációs hibák miatt.
     * 
     * @param RegisterRequestDto user
     */
    @PostMapping(path = "/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void authRegisterUser(@Valid @RequestBody RegisterRequestDto user) {
        UserCreateCommand command = new UserCreateCommand(user.username(), user.email(), user.password());
        userService.createUser(command);
    }

    /**
     * Felhasználó beléptetése
     * 
     * @param username
     * @param password
     * @return üres body + JWT token egy HttpOnly cookie-ban
     */
    @PostMapping(path = "/auth/login")
    public ResponseEntity<Void> loginUser(@Valid @RequestBody LoginRequestDto loginRequest) {
        UserLoginCommand command = new UserLoginCommand(loginRequest.username(), loginRequest.password());

        String jwtToken = userService.login(command);
        ResponseCookie cookie = cookieUtil.getJwtCookie(jwtToken);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Set-Cookie", cookie.toString());
        return ResponseEntity.ok().headers(headers).build();
    }

    /**
     * Felhasználó kiejlentkeztetése (jwt token törlése a böngészőből)
     * 
     * @return ResponseEntity üres body, de tartalmaz headerben egy expiration
     *         cookie-t
     */
    @PostMapping(path = "/auth/logout")
    public ResponseEntity<Void> logoutUser() {
        ResponseCookie expiredCookie = cookieUtil.getJwtSessionDestroyCookie();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Set-Cookie", expiredCookie.toString());
        return ResponseEntity.ok().headers(headers).build();
    }

    /**
     * Ellenőrzi, hogy a felhasználónév foglalt-e már
     * 
     * @param username
     * @return Boolean
     */
    @PostMapping(path = "/auth/isUsernameExists")
    public boolean isUsernameExists(@RequestBody IsUsernameExistsDto dto) {
        return userService.isUsernameExists(dto.username());
    }

    /**
     * Ellenőrzi, hogy az email cím foglalt-e már
     * 
     * @param email
     * @return Boolean
     */
    @PostMapping(path = "/auth/isEmailExists")
    public boolean isEmailExists(@RequestBody IsEmailExistsDto dto) {
        return userService.isEmailExists(dto.email());
    }

    /**
     * User alapadatokkal tér vissza, a bejelentkezés tényét dönti el.
     * Ha be van loginolva a user, akkor visszaadja az adatokat, egyébként el sem
     * éri ezt a végpontot
     * Így pl.: frontend újrabetöltéskor azonnal le tudja ellenőrizni a frontend,
     * hogy be vagyunk-e jelentkezve
     * 
     * @return
     */
    @PostMapping(path = "auth/authenticateUser")
    public UserDataResponseDto authenticateUser() {
        User user = this.userUtil.getUser();
        // user.getWallets() nem használható, mert itt már a security principal user
        // detached
        List<Wallet> wallets = walletRepository.findByUserId(user.getId());

        return mapper.toDto(user, wallets);
    }
}
