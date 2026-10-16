package com.starbuck.moneytracker.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.service.UserLookupService;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserLookupService userLookupService;

    @BeforeEach
    void setUp() {
        userLookupService = new UserLookupService(userRepository);
    }

    /**
     * Ha létezik user az email címmel, azt adja vissza
     */
    @Test
    void loadUserByEmail_returnsUserWhenFound() {
        User userInDB = new User("testuser", "encodedPassword", "teszt@email.com");
        Mockito.when(userRepository.findByEmail("teszt@email.com")).thenReturn(userInDB);

        User result = userLookupService.loadUserByEmail("teszt@email.com");

        assertEquals(userInDB, result);
    }

    /**
     * Ha nem található az email címmel user, UsernameNotFoundException-t
     * dob
     */
    @Test
    void loadUserByEmail_throwsWhenUserNotFound() {
        Mockito.when(userRepository.findByEmail("missing@email.com")).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> {
            userLookupService.loadUserByEmail("missing@email.com");
        });
    }
}
