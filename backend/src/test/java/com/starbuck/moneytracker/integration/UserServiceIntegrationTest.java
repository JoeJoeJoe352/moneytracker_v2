package com.starbuck.moneytracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.service.JwtService;
import com.starbuck.moneytracker.service.UserService;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    /**
     * Sikeres létrehozás esetén a jelszó titkosítva, uuid-vel ellátva
     * mentődik el
     */
    @Test
    void createUser_persistsEncodedPasswordAndUuid() {
        User user = new User("integrationUser", null, "integration@email.com");

        User saved = userService.createUser(user, "password");

        assertNotNull(saved.getId());
        assertNotNull(saved.getUuid());
        assertNotEquals("password", saved.getPassword());
        assertTrue(userRepository.existsByUsername("integrationUser"));

        userRepository.delete(saved);
    }

    /**
     * Ha már foglalt az email cím, hibát dob és nem jön létre új felhasználó
     */
    @Test
    void createUser_throwsWhenEmailAlreadyExists() {
        User existing = userService.createUser(new User("emailOwner", null, "duplicate@email.com"), "password");

        User newUser = new User("brandNewUsername", null, "duplicate@email.com");

        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(newUser, "password");
        });

        assertFalse(userRepository.existsByUsername("brandNewUsername"));

        userRepository.delete(existing);
    }

    /**
     * Ha már foglalt a felhasználónév, hibát dob és nem jön létre új
     * felhasználó
     */
    @Test
    void createUser_throwsWhenUsernameAlreadyExists() {
        User existing = userService.createUser(new User("duplicateUsername", null, "usernameOwner@email.com"),
                "password");

        User newUser = new User("duplicateUsername", null, "brandnew@email.com");

        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(newUser, "password");
        });

        assertFalse(userRepository.existsByEmail("brandnew@email.com"));

        userRepository.delete(existing);
    }

    /**
     * Sikeres belépés esetén érvényes, a felhasználóra kiállított tokennel tér
     * vissza
     */
    @Test
    void login_returnsValidTokenForCorrectCredentials() {
        User saved = userService.createUser(new User("loginUser", null, "login@email.com"), "password");

        String token = userService.login("loginUser", "password");

        assertNotNull(token);
        assertEquals("loginUser", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, saved));

        userRepository.delete(saved);
    }

    /**
     * Hibás jelszóval hibát dob
     */
    @Test
    void login_throwsForWrongPassword() {
        User saved = userService.createUser(new User("wrongPassUser", null, "wrongpass@email.com"), "password");

        assertThrows(IllegalArgumentException.class, () -> {
            userService.login("wrongPassUser", "notThePassword");
        });

        userRepository.delete(saved);
    }

    /**
     * Nem létező felhasználónévvel hibát dob
     */
    @Test
    void login_throwsForUnknownUsername() {
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login("nonExistentUser", "password");
        });
    }

    /**
     * isUsernameExists a ténylegesen elmentett usernek megfelelően válaszol
     */
    @Test
    void isUsernameExists_reflectsPersistedState() {
        assertFalse(userService.isUsernameExists("notYetRegisteredUsername"));

        User saved = userService.createUser(new User("notYetRegisteredUsername", null, "checkusername@email.com"),
                "password");

        assertTrue(userService.isUsernameExists("notYetRegisteredUsername"));

        userRepository.delete(saved);
    }

    /**
     * isEmailExists a ténylegesen elmentett usernek megfelelően válaszol
     */
    @Test
    void isEmailExists_reflectsPersistedState() {
        assertFalse(userService.isEmailExists("notyetregistered@email.com"));

        User saved = userService.createUser(new User("checkEmailUser", null, "notyetregistered@email.com"),
                "password");

        assertTrue(userService.isEmailExists("notyetregistered@email.com"));

        userRepository.delete(saved);
    }
}
