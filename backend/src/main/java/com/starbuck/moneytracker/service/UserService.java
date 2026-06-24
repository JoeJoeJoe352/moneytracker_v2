package com.starbuck.moneytracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
    
    /**
     * Felhasználó létrehozása a megadott adatokkal. Username és email cím egyediség ellenőrzés
     * 
     * @param User user
     * @return User
     */
    public User createUser(User user, String password) {
        if (userRepository.existsByEmail(user.getEmail()) || userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username or email already exists");
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setUuid();
        userRepository.save(user);
        return user;
    }

    /**
     * Felhasználó bejelentkezése. Siker esetén visszaadja a felhasználó adatait, hiba esetén IllegalArgumentException-t dob.
     * 
     * @param loginRequest
     * @return User
     */
    public String login(String username, String password) {
        User user = this.userRepository.findByUsername(username);
        if (user == null || !this.passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        return this.jwtService.generateToken(username);
    }

    /**
     * Megnézi, hogy felhasználónév foglalt-e már
     * 
     * @param username
     * @return Boolean
     */
    public Boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Megnézi, hogy az email cím foglalt-e már
     * 
     * @param email
     * @return Boolean
     */
    public Boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Usernév alapján megszerzi a teljes user modellt
     * 
     * @param username
     * @return
     */
    public User getUserFromName(String username) {
        return this.userRepository.findByUsername(username);
    }
}
