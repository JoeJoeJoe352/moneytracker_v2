package com.starbuck.moneytracker.service;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private WalletService walletService;

    /**
     * Felhasználó létrehozása a megadott adatokkal. Username és email cím egyediség
     * ellenőrzés
     * 
     * @param User user
     * @return User
     */
    @Transactional
    public User createUser(UserCreateCommand command) {
        if (userRepository.existsByEmail(command.getEmail())
                || userRepository.existsByUsername(command.getUsername())) {
            throw new IllegalArgumentException("Username or email already exists");
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
            throw new BadCredentialsException("Invalid username or password");
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
