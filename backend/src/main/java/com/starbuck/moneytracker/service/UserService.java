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
     * @param username
     * @param password
     * @return User
     */
    public User login(String username, String password) {
        User user = this.userRepository.findByUsername(username);
        if (user != null && this.passwordEncoder.matches(password, user.getPassword())) {
            return user;
        } else {
            throw new IllegalArgumentException("Invalid username or password");
        }
    }
}
