package com.starbuck.moneytracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.starbuck.moneytracker.commands.UserCreateCommand;
import com.starbuck.moneytracker.commands.UserLoginCommand;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.service.JwtService;
import com.starbuck.moneytracker.service.UserService;
import com.starbuck.moneytracker.util.CurrentUserUtil;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CurrentUserUtil currentUser;

    /**
     * Sikeres létrehozás esetén a jelszó titkosítva, uuid-vel ellátva
     * mentődik el
     */
    @Test
    void createUser_persistsEncodedPasswordAndUuid() {
        // Ez accpet-lang headerből jönne, de integrációs teszteknél nem használjuk,
        // ilyenkor viszont a gép nyelvét használná alapesetben
        LocaleContextHolder.setDefaultLocale(Locale.ENGLISH);

        UserCreateCommand command = new UserCreateCommand("integrationUser", "integration@email.com", "teszt");
        User saved = userService.createUser(command);

        assertNotNull(saved.getId());
        assertNotNull(saved.getUuid());
        assertNotEquals("password", saved.getPassword());
        assertTrue(userRepository.existsByUsername("integrationUser"));

        List<Wallet> wallets = walletRepository.findAll();
        assertEquals(1, wallets.size());

        // default a magyar nyelv, ezért wallet lesz a név
        assertEquals("Wallet", wallets.get(0).getName());

        walletRepository.delete(wallets.get(0));
        userRepository.delete(saved);
    }

    /**
     * Ha már foglalt az email cím, hibát dob és nem jön létre új felhasználó
     */
    @Test
    void createUser_throwsWhenEmailAlreadyExists() {
        UserCreateCommand command = new UserCreateCommand("emailOwner", "duplicate@email.com", "teszt");

        User existing = userService.createUser(command);

        UserCreateCommand anotherUserCommand = new UserCreateCommand("brandNewUsername", "duplicate@email.com",
                "teszt");
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(anotherUserCommand);
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
        UserCreateCommand command = new UserCreateCommand("duplicateUsername", "usernameOwner@email.com", "teszt");
        User existing = userService.createUser(command);

        UserCreateCommand newUserCommand = new UserCreateCommand("duplicateUsername", "brandnew@email.com", "teszt");
        assertThrows(IllegalArgumentException.class, () -> {
            userService.createUser(newUserCommand);
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
        UserCreateCommand createCommand = new UserCreateCommand("loginUser", "login@email.com", "password");
        User saved = userService.createUser(createCommand);

        UserLoginCommand loginCommand = new UserLoginCommand("loginUser", "password");
        String token = userService.login(loginCommand);

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
        UserCreateCommand command = new UserCreateCommand("wrongPassUser", "wrongpass@email.com", "password");
        User saved = userService.createUser(command);

        UserLoginCommand loginCommand = new UserLoginCommand("wrongPassUser", "notThePassword");
        assertThrows(BadCredentialsException.class, () -> {
            userService.login(loginCommand);
        });

        userRepository.delete(saved);
    }

    /**
     * Nem létező felhasználónévvel hibát dob
     */
    @Test
    void login_throwsForUnknownUsername() {
        UserLoginCommand loginCommand = new UserLoginCommand("nonExistentUser", "password");
        assertThrows(BadCredentialsException.class, () -> {
            userService.login(loginCommand);
        });
    }

    /**
     * isUsernameExists a ténylegesen elmentett usernek megfelelően válaszol
     */
    @Test
    void isUsernameExists_reflectsPersistedState() {
        assertFalse(userService.isUsernameExists("notYetRegisteredUsername"));

        UserCreateCommand command = new UserCreateCommand("notYetRegisteredUsername", "checkusername@email.com",
                "password");
        User saved = userService.createUser(command);

        assertTrue(userService.isUsernameExists("notYetRegisteredUsername"));

        userRepository.delete(saved);
    }

    /**
     * isEmailExists a ténylegesen elmentett usernek megfelelően válaszol
     */
    @Test
    void isEmailExists_reflectsPersistedState() {
        assertFalse(userService.isEmailExists("notyetregistered@email.com"));

        UserCreateCommand command = new UserCreateCommand("checkEmailUser", "notyetregistered@email.com",
                "password");
        User saved = userService.createUser(command);

        assertTrue(userService.isEmailExists("notyetregistered@email.com"));

        userRepository.delete(saved);
    }
}
