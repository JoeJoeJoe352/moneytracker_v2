package com.starbuck.moneytracker.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.starbuck.moneytracker.commands.UserCreateCommand;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.service.JwtService;
import com.starbuck.moneytracker.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    /**
     * Sikeres létrehozás esetén a jelszót titkosítva menti, uuid-t generál és
     * elmenti a usert
     */
    @Test
    void createUser_savesEncodedPasswordAndUuid() {
        // GIVEN
        UserCreateCommand UserCreateCommand = new UserCreateCommand("testuser", "teszt@email.com", "password");
        Mockito.when(userRepository.existsByEmail("teszt@email.com")).thenReturn(false);
        Mockito.when(userRepository.existsByUsername("testuser")).thenReturn(false);
        Mockito.when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        // WHEN
        User result = userService.createUser(UserCreateCommand);

        // THEN
        assertEquals("encodedPassword", result.getPassword());
        assertNotNull(result.getUuid());
    }

    /**
     * Ha már foglalt az email cím, hibát dob és nem menti el a usert
     */
    @Test
    void createUser_throwsWhenEmailAlreadyExists() {
        UserCreateCommand UserCreateCommand = new UserCreateCommand("testuser", "teszt@email.com", "password");
        Mockito.when(userRepository.existsByEmail("teszt@email.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(UserCreateCommand);
        });

        Mockito.verify(userRepository, Mockito.never()).save(any(User.class));
    }

    /**
     * Ha már foglalt a felhasználónév, hibát dob és nem menti el a usert
     */
    @Test
    void createUser_throwsWhenUsernameAlreadyExists() {
        UserCreateCommand UserCreateCommand = new UserCreateCommand("testuser", "teszt@email.com", "password");

        Mockito.when(userRepository.existsByEmail("teszt@email.com")).thenReturn(false);
        Mockito.when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(UserCreateCommand);
        });

        Mockito.verify(userRepository, Mockito.never()).save(any(User.class));
    }

    /**
     * Sikeres belépés esetén a generált tokennel tér vissza
     */
    @Test
    void login_returnsGeneratedTokenOnSuccess() {
        User userInDB = new User("testuser", "encodedPassword", "teszt@email.com");

        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(userInDB);
        Mockito.when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        Mockito.when(jwtService.generateToken("testuser")).thenReturn("generatedToken");

        String token = userService.login("testuser", "password");

        assertEquals("generatedToken", token);
    }

    /**
     * Ha nem létezik a felhasználónév, hibát dob
     */
    @Test
    void login_throwsWhenUsernameNotFound() {
        Mockito.when(userRepository.findByUsername("missing")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.login("missing", "password");
        });

        Mockito.verify(jwtService, Mockito.never()).generateToken(anyString());
    }

    /**
     * Ha rossz a jelszó, hibát dob
     */
    @Test
    void login_throwsWhenPasswordDoesNotMatch() {
        User userInDB = new User("testuser", "encodedPassword", "teszt@email.com");

        Mockito.when(userRepository.findByUsername("testuser")).thenReturn(userInDB);
        Mockito.when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.login("testuser", "wrongPassword");
        });

        Mockito.verify(jwtService, Mockito.never()).generateToken(anyString());
    }

    /**
     * isUsernameExists visszaadja a repository válaszát
     */
    @Test
    void isUsernameExists_returnsTrueWhenTaken() {
        Mockito.when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertTrue(userService.isUsernameExists("testuser"));
    }

    @Test
    void isUsernameExists_returnsFalseWhenAvailable() {
        Mockito.when(userRepository.existsByUsername("freeUsername")).thenReturn(false);

        assertFalse(userService.isUsernameExists("freeUsername"));
    }

    /**
     * isEmailExists visszaadja a repository válaszát
     */
    @Test
    void isEmailExists_returnsTrueWhenTaken() {
        Mockito.when(userRepository.existsByEmail("teszt@email.com")).thenReturn(true);

        assertTrue(userService.isEmailExists("teszt@email.com"));
    }

    @Test
    void isEmailExists_returnsFalseWhenAvailable() {
        Mockito.when(userRepository.existsByEmail("free@email.com")).thenReturn(false);

        assertFalse(userService.isEmailExists("free@email.com"));
    }
}
