package com.starbuck.moneytracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    /**
     * Felhasználó létrehozása a megadott adatokkal. Username és email cím egyediség
     * ellenőrzés
     * 
     * @param User user
     * @return User
     */
    public User createUser(UserCreateCommand command) {
        if (userRepository.existsByEmail(command.getEmail()) || userRepository.existsByUsername(command.getUsername())) {
            throw new IllegalArgumentException("Username or email already exists");
        }

        User user = new User(command.getUsername(), passwordEncoder.encode(command.getPassword()), command.getEmail());
        user.setUuid();
        userRepository.save(user);
        
        // TODO létrehozni default walletet. Transactional legyen a függvény

        return user;
    }

    /**
     * Felhasználó bejelentkezése. Siker esetén visszaadja a felhasználó adatait,
     * hiba esetén IllegalArgumentException-t dob.
     * 
     * @param loginRequest
     * @return User
     */
    public String login(UserLoginCommand command) {
        User user = this.userRepository.findByUsername(command.getUsername());
        if (user == null || !this.passwordEncoder.matches(command.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        return this.jwtService.generateToken(command.getUsername());
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
}
