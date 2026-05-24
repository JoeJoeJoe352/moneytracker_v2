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
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail()) || userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username or email already exists");
        }

        user.setUuid(java.util.UUID.randomUUID().toString());
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
     * Megnézi, hogy felhasználónév vagy jelszó foglalt-e már
     * 
     * @param username
     * @param email
     * @return
     */
    public Boolean usernameOrEmailExists(String username, String email) {
        return userRepository.existsByEmailOrUsername(email, username);
    }
}
