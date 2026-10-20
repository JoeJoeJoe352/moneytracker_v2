package com.starbuck.moneytracker.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.starbuck.moneytracker.config.TimeZoneAwareLocaleResolver;
import com.starbuck.moneytracker.dto.LoginRequestDto;
import com.starbuck.moneytracker.dto.RegisterRequestDto;
import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.testsupport.MySqlContainerTest;

/**
 * A transactionDate @PastOrPresentInZone validációját teszteli végig a valós HTTP útvonalon:
 * a "ma" a frontend által küldött x-time-zone header szerinti nap.
 * Külön osztály, mert a Clock mockolása miatt a többi teszt LocalDate.now()-ja jövőbeli dátum lenne
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class TransactionDateTimeZoneE2ETest extends MySqlContainerTest {

    /** UTC szerint 2026-10-01 22:30 - Budapesten (UTC+2) már 10-02, New Yorkban (UTC-4) még 10-01 */
    private static final Instant NOW = Instant.parse("2026-10-01T22:30:00Z");

    @MockitoBean
    private Clock clock;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionDetailRepository transactionDetailRepository;

    private String authCookie;
    private User user;
    private Wallet wallet;

    @BeforeEach
    void registerAndLoginRealUser() {
        // A regisztráció (alapértelmezett tárca) is használhatja a Clockot, ezért előtte kell
        Mockito.when(clock.instant()).thenReturn(NOW);

        RegisterRequestDto registerRequest = new RegisterRequestDto("e2eTimeZoneUser", "password123",
                "e2etimezone@email.com");
        ResponseEntity<Void> registerResponse = restTemplate.postForEntity("/auth/register", registerRequest,
                Void.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        LoginRequestDto loginRequest = new LoginRequestDto("e2etimezone@email.com", "password123");
        ResponseEntity<Map<String, String>> loginResponse = restTemplate.exchange("/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest), new ParameterizedTypeReference<Map<String, String>>() {
                });
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());

        String setCookieHeader = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        this.authCookie = setCookieHeader.split(";")[0];

        this.user = userRepository.findByEmail("e2etimezone@email.com");
        this.wallet = walletRepository.findByUserId(this.user.getId()).get(0);
    }

    @AfterEach
    void cleanupCreatedData() {
        transactionDetailRepository.deleteAllInBatch();
        transactionRepository.findAll()
                .forEach(transaction -> transactionRepository
                        .hardDeleteTransaction(transaction.getId()));
        walletRepository.delete(this.wallet);
        userRepository.delete(this.user);
    }

    /**
     * Ugyanaz a dátum az egyik zónában már ma, a másikban még holnap
     */
    @ParameterizedTest(name = "{0}, {1} -> {2}")
    @CsvSource({
            "Europe/Budapest, 2026-10-02, CREATED", // Budapesten ma
            "Europe/Budapest, 2026-10-03, BAD_REQUEST",
            "America/New_York, 2026-10-01, CREATED", // New Yorkban ma
            "America/New_York, 2026-10-02, BAD_REQUEST", // New Yorkban még holnap
            "Pacific/Kiritimati, 2026-10-02, CREATED", // UTC+14
            "Pacific/Pago_Pago, 2026-10-02, BAD_REQUEST", // UTC-11
            "Europe/Budapest, 2026-09-15, CREATED" // múltbeli dátum
    })
    void createTransaction_validatesDateInUserTimeZone(String zone, LocalDate transactionDate,
            HttpStatus expectedStatus) {
        HttpHeaders headers = this.getHeaderWithCookie();
        headers.add(TimeZoneAwareLocaleResolver.TIME_ZONE_HEADER, zone);

        ResponseEntity<Map<String, String>> response = this.postTransaction(transactionDate, headers);

        assertEquals(expectedStatus, response.getStatusCode());
        if (expectedStatus == HttpStatus.CREATED) {
            assertEquals(1, transactionRepository.count());
        } else {
            assertTrue(response.getBody().containsKey("transactionDate"));
            assertEquals(0, transactionRepository.count());
        }
    }

    /**
     * Header nélkül (pl.: postman) UTC szerinti a mai nap, ahol még 10-01 van
     */
    @Test
    void createTransaction_withoutTimeZoneHeaderUsesUtc() {
        HttpHeaders headers = this.getHeaderWithCookie();

        assertEquals(HttpStatus.BAD_REQUEST,
                this.postTransaction(LocalDate.of(2026, 10, 2), headers).getStatusCode());
        assertEquals(HttpStatus.CREATED,
                this.postTransaction(LocalDate.of(2026, 10, 1), headers).getStatusCode());
        assertEquals(1, transactionRepository.count());
    }

    private ResponseEntity<Map<String, String>> postTransaction(LocalDate transactionDate, HttpHeaders headers) {
        TransactionCreateRequest request = new TransactionCreateRequest("Időzóna teszt", new BigDecimal("-1500"),
                TransactionTypeEnum.OUTCOME, transactionDate, List.of(), List.of(), this.wallet.getId());

        return restTemplate.exchange("/transaction", HttpMethod.POST, new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<Map<String, String>>() {
                });
    }

    private HttpHeaders getHeaderWithCookie() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, this.authCookie);
        return headers;
    }
}
