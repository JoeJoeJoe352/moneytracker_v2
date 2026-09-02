package com.starbuck.moneytracker.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.starbuck.moneytracker.dto.LoginRequestDto;
import com.starbuck.moneytracker.dto.RegisterRequestDto;
import com.starbuck.moneytracker.dto.WalletCreateDto;
import com.starbuck.moneytracker.dto.WalletResponseDto;
import com.starbuck.moneytracker.dto.WalletUpdateDto;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.GeneralStatusEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.testsupport.MySqlContainerTest;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class WalletE2ETest extends MySqlContainerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    private String authCookie;
    private User user;
    private HttpHeaders headers;

    @BeforeEach
    void registerAndLoginRealUser() {
        RegisterRequestDto registerRequest = new RegisterRequestDto("e2eWalletUser", "password123",
                "e2ewallet@email.com");
        restTemplate.postForEntity("/auth/register", registerRequest, Void.class);

        LoginRequestDto loginRequest = new LoginRequestDto("e2eWalletUser", "password123");
        ResponseEntity<Map<String, String>> loginResponse = restTemplate.exchange("/auth/login", HttpMethod.POST,
                new HttpEntity<>(loginRequest), new ParameterizedTypeReference<Map<String, String>>() {
                });
        String setCookieHeader = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        this.authCookie = setCookieHeader.split(";")[0];

        this.user = userRepository.findByUsername("e2eWalletUser");

        this.headers = new HttpHeaders();
        this.headers.setContentType(MediaType.APPLICATION_JSON);
        this.headers.add(HttpHeaders.COOKIE, this.authCookie);
    }

    @AfterEach
    void cleanupCreatedData() {
        walletRepository.findAll().stream()
                .filter(wallet -> wallet.getUser().getId().equals(this.user.getId()))
                .forEach(walletRepository::delete);
        userRepository.delete(this.user);
    }

    // ---- POST /wallet ----

    /**
     * Sikeres létrehozás esetén a walletet a bejelentkezett userhez menti el
     */
    @Test
    void createWallet_createsWalletForAuthenticatedUser() {
        WalletCreateDto request = new WalletCreateDto("Savings", CurrencyEnum.USD, WalletTypeEnum.SAVINGS);

        ResponseEntity<Void> response = restTemplate.postForEntity("/wallet", new HttpEntity<>(request, headers),
                Void.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        List<Wallet> wallets = walletRepository.findByUserId(this.user.getId());
        assertTrue(wallets.stream().anyMatch(w -> w.getName().equals("Savings")
                && w.getType() == WalletTypeEnum.SAVINGS && w.getCurrencyCode() == CurrencyEnum.USD));
    }

    /**
     * Hiányzó név esetén a command dobja az IllegalArgumentException-t, amit
     * a GlobalExceptionHandler 400-ra fordít, és nem jön létre wallet
     */
    @Test
    void createWallet_returnsBadRequestWhenNameIsMissing() {
        WalletCreateDto request = new WalletCreateDto(null, CurrencyEnum.USD, WalletTypeEnum.SAVINGS);

        ResponseEntity<Void> response = restTemplate.postForEntity("/wallet", new HttpEntity<>(request, headers),
                Void.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ---- GET /wallet ----

    /**
     * A regisztrációkor létrejött alapértelmezett wallet mellett a saját
     * létrehozott walleteket is visszaadja, más user walletjeit viszont nem
     */
    @Test
    void listWallets_returnsOnlyAuthenticatedUsersWallets() {
        WalletCreateDto createRequest = new WalletCreateDto("Savings", CurrencyEnum.USD, WalletTypeEnum.SAVINGS);
        restTemplate.postForEntity("/wallet", new HttpEntity<>(createRequest, headers), Void.class);

        User otherUser = new User("e2eOtherWalletUser", "irrelevantEncodedPassword", "otherwallet@email.com");
        otherUser.generateUuid();
        otherUser = userRepository.save(otherUser);
        Wallet otherUsersWallet = walletRepository
                .save(new Wallet("OtherUsersWallet", otherUser, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT));

        try {
            ResponseEntity<WalletResponseDto[]> response = restTemplate.exchange("/wallet", HttpMethod.GET,
                    new HttpEntity<>(headers), WalletResponseDto[].class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            List<WalletResponseDto> wallets = List.of(response.getBody());

            assertEquals(2, wallets.size());
            assertTrue(wallets.stream().anyMatch(w -> w.name().equals("Savings")));
            assertFalse(wallets.stream().anyMatch(w -> w.name().equals("OtherUsersWallet")));
        } finally {
            walletRepository.delete(otherUsersWallet);
            userRepository.delete(otherUser);
        }
    }

    // ---- GET /wallet/{id} ----

    /**
     * Saját wallet lekérdezése id alapján a wallet adatait adja vissza
     */
    @Test
    void getWalletById_returnsWalletForAuthenticatedUser() {
        Wallet wallet = walletRepository.save(new Wallet("Savings", this.user, CurrencyEnum.USD, WalletTypeEnum.SAVINGS));

        ResponseEntity<WalletResponseDto> response = restTemplate.exchange("/wallet/" + wallet.getId(),
                HttpMethod.GET, new HttpEntity<>(headers), WalletResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        WalletResponseDto body = response.getBody();
        assertEquals(wallet.getId(), body.id());
        assertEquals("Savings", body.name());
        assertEquals(CurrencyEnum.USD, body.currencyCode());
        assertEquals(WalletTypeEnum.SAVINGS, body.type());
    }

    /**
     * Másik user walletjét nem lehet lekérdezni, a service
     * EntityNotFoundException-t dob, amit a GlobalExceptionHandler 404-re fordít
     */
    @Test
    void getWalletById_returnsNotFoundForOtherUsersWallet() {
        User otherUser = new User("e2eOtherGetWalletUser", "irrelevantEncodedPassword", "othergetwallet@email.com");
        otherUser.generateUuid();
        otherUser = userRepository.save(otherUser);
        Wallet otherUsersWallet = walletRepository
                .save(new Wallet("OtherUsersWallet", otherUser, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT));

        try {
            ResponseEntity<Void> response = restTemplate.exchange("/wallet/" + otherUsersWallet.getId(),
                    HttpMethod.GET, new HttpEntity<>(headers), Void.class);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        } finally {
            walletRepository.delete(otherUsersWallet);
            userRepository.delete(otherUser);
        }
    }

    /**
     * Nem létező id-ra 404-et ad vissza
     */
    @Test
    void getWalletById_returnsNotFoundForNonExistentId() {
        ResponseEntity<Void> response = restTemplate.exchange("/wallet/999999999", HttpMethod.GET,
                new HttpEntity<>(headers), Void.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ---- PUT /wallet/{id} ----

    /**
     * Sikeres update esetén a wallet neve és típusa frissül az adatbázisban
     */
    @Test
    void updateWallet_updatesNameAndType() {
        Wallet wallet = walletRepository.save(new Wallet("Savings", this.user, CurrencyEnum.USD, WalletTypeEnum.SAVINGS));
        WalletUpdateDto updateRequest = new WalletUpdateDto("UpdatedSavings", WalletTypeEnum.DEFAULT);

        ResponseEntity<Void> response = restTemplate.exchange("/wallet/" + wallet.getId(), HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers), Void.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

        Wallet updatedWallet = walletRepository.findById(wallet.getId()).orElseThrow();
        assertEquals("UpdatedSavings", updatedWallet.getName());
        assertEquals(WalletTypeEnum.DEFAULT, updatedWallet.getType());
    }

    /**
     * Másik user walletjét nem lehet módosítani, a service
     * EntityNotFoundException-t dob, amit a GlobalExceptionHandler 404-re fordít
     */
    @Test
    void updateWallet_returnsNotFoundForOtherUsersWallet() {
        User otherUser = new User("e2eOtherUpdateWalletUser", "irrelevantEncodedPassword",
                "otherupdatewallet@email.com");
        otherUser.generateUuid();
        otherUser = userRepository.save(otherUser);
        Wallet otherUsersWallet = walletRepository
                .save(new Wallet("OtherUsersWallet", otherUser, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT));
        WalletUpdateDto updateRequest = new WalletUpdateDto("Hacked", WalletTypeEnum.DEFAULT);

        try {
            ResponseEntity<Void> response = restTemplate.exchange("/wallet/" + otherUsersWallet.getId(),
                    HttpMethod.PUT, new HttpEntity<>(updateRequest, headers), Void.class);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            Wallet unchangedWallet = walletRepository.findById(otherUsersWallet.getId()).orElseThrow();
            assertEquals("OtherUsersWallet", unchangedWallet.getName());
        } finally {
            walletRepository.delete(otherUsersWallet);
            userRepository.delete(otherUser);
        }
    }

    // ---- DELETE /wallet/{id} ----

    /**
     * Sikeres soft delete esetén a wallet DISABLED státuszú lesz, és eltűnik a
     * listázásból és az id alapú lekérdezésből is
     */
    @Test
    void softDeleteWallet_disablesWalletAndHidesItFromQueries() {
        Wallet wallet = walletRepository.save(new Wallet("Savings", this.user, CurrencyEnum.USD, WalletTypeEnum.SAVINGS));

        ResponseEntity<Void> response = restTemplate.exchange("/wallet/" + wallet.getId(), HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        Wallet deletedWallet = walletRepository.findById(wallet.getId()).orElseThrow();
        assertEquals(GeneralStatusEnum.DISABLED, deletedWallet.getStatus());

        ResponseEntity<Void> getResponse = restTemplate.exchange("/wallet/" + wallet.getId(), HttpMethod.GET,
                new HttpEntity<>(headers), Void.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    /**
     * Másik user walletjét nem lehet soft delete-elni, a service
     * EntityNotFoundException-t dob, amit a GlobalExceptionHandler 404-re fordít
     */
    @Test
    void softDeleteWallet_returnsNotFoundForOtherUsersWallet() {
        User otherUser = new User("e2eOtherDeleteWalletUser", "irrelevantEncodedPassword",
                "otherdeletewallet@email.com");
        otherUser.generateUuid();
        otherUser = userRepository.save(otherUser);
        Wallet otherUsersWallet = walletRepository
                .save(new Wallet("OtherUsersWallet", otherUser, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT));

        try {
            ResponseEntity<Void> response = restTemplate.exchange("/wallet/" + otherUsersWallet.getId(),
                    HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            Wallet unchangedWallet = walletRepository.findById(otherUsersWallet.getId()).orElseThrow();
            assertEquals(GeneralStatusEnum.ACTIVE, unchangedWallet.getStatus());
        } finally {
            walletRepository.delete(otherUsersWallet);
            userRepository.delete(otherUser);
        }
    }

    // ---- Security határ ----

    /**
     * Auth cookie nélkül a /wallet végpontok is elutasításra kerülnek,
     * mielőtt a controller lefutna
     */
    @ParameterizedTest(name = "{0} {1} auth cookie nélkül elutasítva")
    @MethodSource("walletEndpoints")
    void walletEndpoints_areRejectedWithoutAuthCookie(HttpMethod method, String path) {
        ResponseEntity<Void> response = restTemplate.exchange(path, method, null, Void.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    private static Stream<Arguments> walletEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/wallet"),
                Arguments.of(HttpMethod.GET, "/wallet"),
                Arguments.of(HttpMethod.GET, "/wallet/1"),
                Arguments.of(HttpMethod.PUT, "/wallet/1"),
                Arguments.of(HttpMethod.DELETE, "/wallet/1"));
    }
}
