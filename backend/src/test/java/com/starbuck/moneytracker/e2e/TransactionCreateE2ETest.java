package com.starbuck.moneytracker.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.starbuck.moneytracker.dto.LoginRequest;
import com.starbuck.moneytracker.dto.RegisterRequestDto;
import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.dto.TransactionDetailCreateDto;
import com.starbuck.moneytracker.dto.TransactionResponseDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.repository.UserRepository;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class TransactionCreateE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionDetailRepository transactionDetailRepository;

    private String authCookie;
    private User user;

    /**
     * Minden teszt előtt egy valódi usert regisztrálunk és beléptetünk a
     * publikus végpontokon keresztül, hogy a kapott JWT cookie-t utána a
     * védett /transaction végpontok hívásához használhassuk
     */
    @BeforeEach
    void registerAndLoginRealUser() {
        RegisterRequestDto registerRequest = new RegisterRequestDto("e2eCreateUser", "password123",
                "e2ecreate@email.com");
        ResponseEntity<Void> registerResponse = restTemplate.postForEntity("/auth/register", registerRequest,
                Void.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        LoginRequest loginRequest = new LoginRequest("e2eCreateUser", "password123");
        ResponseEntity<Map<String, String>> loginResponse = restTemplate.exchange("/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest), new ParameterizedTypeReference<Map<String, String>>() {
                });
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());

        // A szerver által ténylegesen kiállított Set-Cookie headert használjuk,
        // nem magunk gyártunk JWT-t - így a teszt a valós autentikációs útvonalat
        // futtatja végig
        String setCookieHeader = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        this.authCookie = setCookieHeader.split(";")[0];

        this.user = userRepository.findByUsername("e2eCreateUser");
    }

    @AfterEach
    void cleanupCreatedData() {
        transactionDetailRepository.deleteAllInBatch();
        transactionRepository.findAll()
                .forEach(transaction -> transactionRepository
                        .hardDeleteTransaction(transaction.getId()));
        userRepository.delete(this.user);
    }

    /**
     * A teljes útvonalat futtatja végig: autentikált POST /transaction ->
     * adatbázisban ellenőrizzük a mentést -> majd egy másik, szintén
     * autentikált végponton (GET /transaction/last) keresztül is
     * visszaolvassuk
     */
    @Test
    void createTransaction_persistsAndIsRetrievableThroughTheApi() {
        // GIVEN
        TransactionDetailCreateDto detail = new TransactionDetailCreateDto(new BigDecimal("100.00"), "teszt", null,
                null, List.of());
        TransactionCreateRequest request = new TransactionCreateRequest("E2E groceries", null,
                TransactionTypeEnum.INCOME, LocalDate.now(), List.of(detail), List.of());

        var headers = this.getHeaderWithCookie();

        // WHEN
        ResponseEntity<Void> response = restTemplate.postForEntity("/transaction",
                new HttpEntity<>(request, headers), Void.class);

        // THEN
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // Adatbázis szintű ellenőrzés
        List<Transaction> transactionsInDb = transactionRepository.findAll();
        assertEquals(1, transactionsInDb.size());
        assertEquals("E2E groceries", transactionsInDb.get(0).getName());
        assertEquals(new BigDecimal("100.00"), transactionsInDb.get(0).getPriceSum());
        assertEquals(this.user.getId(), transactionsInDb.get(0).getUser().getId());

        // API szintű ellenőrzés: egy másik, szintén autentikált végponton keresztül
        // is visszaolvasható-e a frissen létrehozott tranzakció
        ResponseEntity<TransactionResponseDto[]> lastTransactionsResponse = restTemplate.exchange(
                "/transaction/last", HttpMethod.GET, new HttpEntity<>(headers),
                TransactionResponseDto[].class);

        assertEquals(HttpStatus.OK, lastTransactionsResponse.getStatusCode());
        TransactionResponseDto[] lastTransactions = lastTransactionsResponse.getBody();
        assertEquals(1, lastTransactions.length);
        assertEquals("E2E groceries", lastTransactions[0].name());
        assertEquals(new BigDecimal("100.00"), lastTransactions[0].priceSum());
    }

    /**
     * Autentikációs cookie nélkül a security filter chain elutasítja a
     * kérést, a controller le sem fut - így semmi nem kerül az adatbázisba.
     * (Mivel az alkalmazás sem formLogin-t, sem httpBasic-et nem konfigurál,
     * a Spring Security alapértelmezett fallback entry pointja - mivel nincs
     * hova visszairányítani a usert - 403-at ad vissza login oldal helyett.)
     */
    @Test
    void createTransaction_isRejectedWithoutAuthCookie() {
        TransactionDetailCreateDto detail = new TransactionDetailCreateDto(new BigDecimal("100.00"), null, null,
                null, List.of());
        TransactionCreateRequest request = new TransactionCreateRequest("Should not persist", null,
                TransactionTypeEnum.INCOME, LocalDate.now(), List.of(detail), List.of());

        ResponseEntity<Void> response = restTemplate.postForEntity("/transaction", request, Void.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(0, transactionRepository.count());
    }

    /**
     * Validációs hiba esetén (pl.: túl rövid név) a végpont 400-at ad
     * vissza, és nem hoz létre semmit az adatbázisban
     */
    @Test
    void createTransaction_returnsBadRequestForInvalidPayload() {
        // "ab" rövidebb, mint az elvárt minimum 3 karakter
        TransactionCreateRequest invalidRequest = new TransactionCreateRequest("ab", new BigDecimal("10.00"),
                TransactionTypeEnum.INCOME, LocalDate.now(), List.of(), List.of());

        var headers = this.getHeaderWithCookie();

        ResponseEntity<Void> response = restTemplate.postForEntity("/transaction",
                new HttpEntity<>(invalidRequest, headers), Void.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(0, transactionRepository.count());
    }

    private HttpHeaders getHeaderWithCookie() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.COOKIE, this.authCookie);
        return headers;
    }
}
