package com.starbuck.moneytracker.service;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.repository.UserRepository;

/**
 * User betöltése a jwt tokenben lévő email cím alapján az authentikációs
 * filterben
 */
@Service
public class UserLookupService {

    private final UserRepository userRepository;

    public UserLookupService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * User betöltése email cím alapján
     *
     * @param String email
     * @return User
     * @throws UsernameNotFoundException ha nincs ilyen email címmel user
     */
    public User loadUserByEmail(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        return user;
    }

}
