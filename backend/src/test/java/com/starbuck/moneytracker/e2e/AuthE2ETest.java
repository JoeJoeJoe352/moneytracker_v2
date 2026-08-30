package com.starbuck.moneytracker.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.starbuck.moneytracker.dto.LoginRequestDto;
import com.starbuck.moneytracker.dto.RegisterRequestDto;
import com.starbuck.moneytracker.dto.UserDataResponseDto;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.repository.WalletRepository;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class AuthE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    // A tesztek során regisztrált usernevek, hogy minden teszt végén automatikusan
    // ki tudjuk törölni
    private final List<String> createdUsernames = new ArrayList<>();

    @AfterEach
    void cleanupCreatedUsers() {
        createdUsernames.forEach(username -> {
            User user = userRepository.findByUsername(username);
            if (user != null) {
                walletRepository.deleteAll(walletRepository.findByUserId(user.getId()));
                userRepository.delete(user);
            }
        });
        createdUsernames.clear();
    }

    /**
     * Regisztrál egy usert a valódi /auth/register végponton keresztül
     */
    private ResponseEntity<Void> register(String username, String password, String email) {
        createdUsernames.add(username);
        RegisterRequestDto request = new RegisterRequestDto(username, password, email);
        return restTemplate.postForEntity("/auth/register", request, Void.class);
    }

    /**
     * Bejelentkezik a valódi /auth/login végponton keresztül, és a szerver
     * által ténylegesen kiállított Set-Cookie header-ből olvassa vissza az
     * auth cookie-t
     */
    private String login(String username, String password) {
        LoginRequestDto loginRequest = new LoginRequestDto(username, password);
        ResponseEntity<Map<String, String>> response = restTemplate.exchange("/auth/login", HttpMethod.POST,
                new HttpEntity<>(loginRequest), new ParameterizedTypeReference<Map<String, String>>() {
                });
        String setCookieHeader = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        return setCookieHeader.split(";")[0];
    }

    private HttpHeaders authHeaders(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);
        return headers;
    }

    // ---- /auth/register ----

    /**
     * Sikeres regisztráció esetén a user titkosított jelszóval jön létre az
     * adatbázisban és egy wallet automatikusan létrejön neki
     */
    @Test
    void register_createsUserWithEncodedPassword() {
        ResponseEntity<Void> response = register("e2eRegisterUser", "password123", "e2eregister@email.com");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        User savedUser = userRepository.findByUsername("e2eRegisterUser");
        assertNotNull(savedUser);
        assertNotEquals("password123", savedUser.getPassword());
        assertNotNull(savedUser.getUuid());

        List<Wallet> wallets = walletRepository.findAll();
        assertEquals(1, wallets.size());
        assertEquals(savedUser.getId(), wallets.get(0).getUser().getId());
    }

    /**
     * Ha a felhasználónév már foglalt, a service rétegben eldobott
     * IllegalArgumentException-t a GlobalExceptionHandler 400-ra fordítja, és
     * nem jön létre második user
     */
    @Test
    void register_returnsBadRequestWhenUsernameAlreadyTaken() {
        register("e2eDuplicateUser", "password123", "first@email.com");

        RegisterRequestDto duplicateRequest = new RegisterRequestDto("e2eDuplicateUser", "password123",
                "second@email.com");
        ResponseEntity<Void> response = restTemplate.postForEntity("/auth/register", duplicateRequest, Void.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(userRepository.existsByEmail("second@email.com"));
    }

    // ---- /auth/login ----

    /**
     * Helyes belépési adatokra 200-at ad vissza, és a válaszban ott a JWT-t
     * tartalmazó auth cookie
     */
    @Test
    void login_returnsOkWithAuthCookieForValidCredentials() {
        register("e2eLoginUser", "password123", "e2elogin@email.com");

        String cookie = login("e2eLoginUser", "password123");

        assertTrue(cookie.startsWith("AUTH_TOKEN="));
    }

    /**
     * Hibás jelszóra 401-et ad vissza, cookie nélkül
     */
    @Test
    void login_returnsUnauthorizedForInvalidCredentials() {
        register("e2eWrongPassUser", "password123", "e2ewrongpass@email.com");

        LoginRequestDto loginRequest = new LoginRequestDto("e2eWrongPassUser", "notThePassword");
        ResponseEntity<Map<String, String>> response = restTemplate.exchange("/auth/login", HttpMethod.POST,
                new HttpEntity<>(loginRequest), new ParameterizedTypeReference<Map<String, String>>() {
                });

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    }

    // ---- /auth/logout ----

    /**
     * Bejelentkezett userre a logout 200-at ad vissza, és egy azonnal lejáró
     * (maxAge=0) cookie-t küld, ami a böngészőből törli a sütit
     */
    @Test
    void logout_returnsOkWithImmediatelyExpiredCookie() {
        register("e2eLogoutUser", "password123", "e2elogout@email.com");
        String cookie = login("e2eLogoutUser", "password123");

        ResponseEntity<Map<String, String>> response = restTemplate.exchange("/auth/logout", HttpMethod.POST,
                new HttpEntity<>(authHeaders(cookie)), new ParameterizedTypeReference<Map<String, String>>() {
                });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String setCookieHeader = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("Max-Age=0"));
    }

    /**
     * Auth cookie nélkül a logout is védett végpontnak számít, a security
     * filter chain elutasítja
     */
    @Test
    void logout_isRejectedWithoutAuthCookie() {
        ResponseEntity<Void> response = restTemplate.postForEntity("/auth/logout", null, Void.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ---- /auth/isUsernameExists, /auth/isEmailExists ----

    /**
     * Publikus végpont, autentikáció nélkül is elérhető: helyesen jelzi,
     * hogy foglalt-e már a felhasználónév
     */
    @Test
    void isUsernameExists_reflectsPersistedState() {
        register("e2eCheckUsernameUser", "password123", "e2echeckusername@email.com");

        Boolean existingResult = restTemplate.postForObject("/auth/isUsernameExists",
                Map.of("username", "e2eCheckUsernameUser"), Boolean.class);
        Boolean missingResult = restTemplate.postForObject("/auth/isUsernameExists",
                Map.of("username", "definitelyNotRegisteredUsername"), Boolean.class);

        assertTrue(existingResult);
        assertFalse(missingResult);
    }

    /**
     * Publikus végpont, autentikáció nélkül is elérhető: helyesen jelzi,
     * hogy foglalt-e már az email cím
     */
    @Test
    void isEmailExists_reflectsPersistedState() {
        register("e2eCheckEmailUser", "password123", "e2echeckemail@email.com");

        Boolean existingResult = restTemplate.postForObject("/auth/isEmailExists",
                Map.of("email", "e2echeckemail@email.com"), Boolean.class);
        Boolean missingResult = restTemplate.postForObject("/auth/isEmailExists",
                Map.of("email", "definitelynotregistered@email.com"), Boolean.class);

        assertTrue(existingResult);
        assertFalse(missingResult);
    }

    // ---- /auth/authenticateUser ----

    /**
     * Érvényes auth cookie-val a bejelentkezett user nevét adja vissza -
     * ezen keresztül dönti el a frontend, hogy be van-e jelentkezve a user
     */
    @Test
    void authenticateUser_returnsUsernameForAuthenticatedUser() {
        register("e2eAuthenticateUser", "password123", "e2eauthenticate@email.com");
        String cookie = login("e2eAuthenticateUser", "password123");

        ResponseEntity<UserDataResponseDto> response = restTemplate.postForEntity("/auth/authenticateUser",
                new HttpEntity<>(authHeaders(cookie)), UserDataResponseDto.class);

        var userData = response.getBody();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("e2eAuthenticateUser", userData.username());

        assertEquals(1, userData.wallets().size());
    }

    /**
     * Auth cookie nélkül a security filter chain elutasítja a kérést, a
     * controller le sem fut
     */
    @Test
    void authenticateUser_isRejectedWithoutAuthCookie() {
        ResponseEntity<Void> response = restTemplate.postForEntity("/auth/authenticateUser", null, Void.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
