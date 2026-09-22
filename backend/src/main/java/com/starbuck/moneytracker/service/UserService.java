package com.starbuck.moneytracker.service;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.commands.UserCreateCommand;
import com.starbuck.moneytracker.commands.UserLoginCommand;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final WalletService walletService;
    private final MessageSource messageSource;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            WalletService walletService, MessageSource messageSource) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.walletService = walletService;
        this.messageSource = messageSource;
    }

    /**
     * Felhasználó létrehozása a megadott adatokkal. Username és email cím egyediség
     * ellenőrzés
     * 
     * @param User user
     * @return User
     */
    @Transactional
    public User createUser(UserCreateCommand command) {
        if (userRepository.existsByUsername(command.getUsername())) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("usernameExists", null, LocaleContextHolder.getLocale()));
        }
        if (userRepository.existsByEmail(command.getEmail())) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("emailExists", null, LocaleContextHolder.getLocale()));
        }

        User user = new User(command.getUsername(), passwordEncoder.encode(command.getPassword()), command.getEmail());
        user.generateUuid();
        User savedUser = userRepository.save(user);

        walletService.createDefaultWallet(savedUser);

        return savedUser;
    }

    /**
     * Felhasználó bejelentkezése. Siker esetén visszaadja a felhasználó adatait
     *
     * @param loginRequest
     * @return User
     */
    public String login(UserLoginCommand command) {
        User user = this.userRepository.findByUsername(command.getUsername());
        if (user == null || !this.passwordEncoder.matches(command.getPassword(), user.getPassword())) {
            var errorMsg = messageSource.getMessage("invalidCredentials", null, LocaleContextHolder.getLocale());
            throw new BadCredentialsException(errorMsg);
        }
        return this.jwtService.generateToken(command.getUsername());
    }

    /**
     * Megnézi, hogy felhasználónév foglalt-e már
     * 
     * @param username
     * @return boolean
     */
    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Megnézi, hogy az email cím foglalt-e már
     * 
     * @param email
     * @return boolean
     */
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
