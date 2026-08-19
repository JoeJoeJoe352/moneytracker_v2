package com.starbuck.moneytracker.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
import org.springframework.test.context.ActiveProfiles;

import com.starbuck.moneytracker.dto.LoginRequest;
import com.starbuck.moneytracker.dto.MoneySumResponseDto;
import com.starbuck.moneytracker.dto.RegisterRequestDto;
import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.dto.TransactionDetailCreateDto;
import com.starbuck.moneytracker.dto.TransactionResponseDto;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.repository.UserRepository;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class TransactionE2ETest {

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
    private HttpHeaders headers;

    // A teszt által létrehozott tranzakció id-k, hogy pontosan (soft delete-elt
    // állapottól függetlenül) takarítani tudjunk utánuk
    private final List<Long> createdTransactionIds = new ArrayList<>();

    @BeforeEach
    void registerAndLoginRealUser() {
        RegisterRequestDto registerRequest = new RegisterRequestDto("e2eTxUser", "password123", "password123",
                "e2etx@email.com");
        restTemplate.postForEntity("/auth/register", registerRequest, Void.class);

        LoginRequest loginRequest = new LoginRequest("e2eTxUser", "password123");
        ResponseEntity<Map<String, String>> loginResponse = restTemplate.exchange("/auth/login", HttpMethod.POST,
                new HttpEntity<>(loginRequest), new ParameterizedTypeReference<Map<String, String>>() {
                });
        String setCookieHeader = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        this.authCookie = setCookieHeader.split(";")[0];

        this.user = userRepository.findByUsername("e2eTxUser");

        this.headers = new HttpHeaders();
        this.headers.setContentType(MediaType.APPLICATION_JSON);
        this.headers.add(HttpHeaders.COOKIE, this.authCookie);
    }

    @AfterEach
    void cleanupCreatedData() {
        transactionDetailRepository.deleteAllInBatch();
        createdTransactionIds.forEach(id -> transactionRepository.hardDeleteTransaction(id));
        createdTransactionIds.clear();
        userRepository.delete(this.user);
    }

    /**
     * Létrehoz egy egy-detailos tranzakciót a valódi POST /transaction
     * végponton keresztül, és visszaadja az adatbázisban kapott id-jét
     */
    private Long createTransactionViaApi(String name, TransactionTypeEnum type, BigDecimal price, LocalDate date) {
        TransactionDetailCreateDto detail = new TransactionDetailCreateDto(price, null, null, null, List.of());
        TransactionCreateRequest request = new TransactionCreateRequest(name, null, type, date, List.of(detail),
                List.of());

        restTemplate.postForEntity("/transaction", new HttpEntity<>(request, headers), Void.class);

        Long id = transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getName().equals(name))
                .findFirst()
                .orElseThrow()
                .getId();
        createdTransactionIds.add(id);
        return id;
    }

    // ---- PUT /transaction/{id} ----

    /**
     * Meglévő tranzakció frissítése esetén az új adatokkal olvasható vissza
     * a GET /transaction/{id} végponton keresztül
     */
    @Test
    void updateTransaction_updatesExistingTransactionForOwner() {
        Long id = createTransactionViaApi("Original", TransactionTypeEnum.INCOME, new BigDecimal("100.00"),
                LocalDate.now());

        TransactionDetailCreateDto updatedDetail = new TransactionDetailCreateDto(new BigDecimal("-50.00"), null,
                null, null, List.of());
        TransactionCreateRequest updateRequest = new TransactionCreateRequest("Updated", null,
                TransactionTypeEnum.OUTCOME, LocalDate.of(2026, 1, 1), List.of(updatedDetail), List.of());

        ResponseEntity<Void> updateResponse = restTemplate.exchange("/transaction/" + id, HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers), Void.class);

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());

        ResponseEntity<TransactionResponseDto> getResponse = restTemplate.exchange("/transaction/" + id,
                HttpMethod.GET, new HttpEntity<>(headers), TransactionResponseDto.class);

        assertEquals("Updated", getResponse.getBody().name());
        assertEquals(TransactionTypeEnum.OUTCOME, getResponse.getBody().transactionType());
        assertEquals(LocalDate.of(2026, 1, 1), getResponse.getBody().transactionDate());
        assertEquals(new BigDecimal("-50.00"), getResponse.getBody().priceSum());
    }

    /**
     * Nem létező (vagy más userhez tartozó) id-ra 404-et ad vissza
     */
    @Test
    void updateTransaction_returnsNotFoundForNonexistentId() {
        TransactionDetailCreateDto detail = new TransactionDetailCreateDto(new BigDecimal("10.00"), null, null,
                null, List.of());
        TransactionCreateRequest updateRequest = new TransactionCreateRequest("Doesn't matter", null,
                TransactionTypeEnum.INCOME, LocalDate.now(), List.of(detail), List.of());

        ResponseEntity<Void> response = restTemplate.exchange("/transaction/999999", HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers), Void.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ---- GET /transaction/sum ----

    /**
     * A user tranzakciói alapján helyesen összegzi a teljes egyenleget és a
     * jelenlegi havi bevételt/kiadást
     */
    @Test
    void sumAllMoney_returnsAggregatedSumsForUser() {
        createTransactionViaApi("Salary", TransactionTypeEnum.INCOME, new BigDecimal("1000.00"), LocalDate.now());
        createTransactionViaApi("Rent", TransactionTypeEnum.OUTCOME, new BigDecimal("-400.00"), LocalDate.now());

        ResponseEntity<MoneySumResponseDto> response = restTemplate.exchange("/transaction/sum", HttpMethod.GET,
                new HttpEntity<>(headers), MoneySumResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        MoneySumResponseDto sums = response.getBody();
        assertEquals(new BigDecimal("600.00"), sums.moneySum());
        assertEquals(new BigDecimal("1000.00"), sums.incomeSumThisMonth());
        assertEquals(new BigDecimal("400.00"), sums.expenseSumThisMonth());
    }

    // ---- GET /transaction/last ----

    /**
     * Legfeljebb 5 legutóbbi tranzakciót ad vissza, akkor is, ha ennél több
     * van a usernek
     */
    @Test
    void getLastTransactions_limitsResultToFiveNewest() {
        for (int i = 0; i < 6; i++) {
            createTransactionViaApi("last" + i, TransactionTypeEnum.INCOME, new BigDecimal("10.00"),
                    LocalDate.now());
        }

        ResponseEntity<TransactionResponseDto[]> response = restTemplate.exchange("/transaction/last",
                HttpMethod.GET, new HttpEntity<>(headers), TransactionResponseDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody().length);
    }

    // ---- GET /transaction/{id} ----

    /**
     * A tranzakció adatait a hozzá tartozó detailokkal együtt adja vissza
     */
    @Test
    void getTransactionById_returnsTransactionWithDetails() {
        Long id = createTransactionViaApi("Groceries", TransactionTypeEnum.OUTCOME, new BigDecimal("-75.00"),
                LocalDate.now());

        ResponseEntity<TransactionResponseDto> response = restTemplate.exchange("/transaction/" + id,
                HttpMethod.GET, new HttpEntity<>(headers), TransactionResponseDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TransactionResponseDto dto = response.getBody();
        assertEquals("Groceries", dto.name());
        assertEquals(new BigDecimal("-75.00"), dto.priceSum());
        assertEquals(TransactionTypeEnum.OUTCOME, dto.transactionType());
        assertEquals(1, dto.transactionDetails().size());
    }

    /**
     * Nem létező (vagy más userhez tartozó) tranzakció id-ra 404-et ad
     * vissza
     */
    @Test
    void getTransactionById_returnsNotFoundForNonexistentId() {
        ResponseEntity<Void> response = restTemplate.exchange("/transaction/999999", HttpMethod.GET,
                new HttpEntity<>(headers), Void.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ---- DELETE /transaction/{id} ----

    /**
     * Törlés után a tranzakció (soft delete miatt) többé nem érhető el a
     * usernek
     */
    @Test
    void deleteTransactionById_removesTransactionFromFutureQueries() {
        Long id = createTransactionViaApi("ToDelete", TransactionTypeEnum.INCOME, new BigDecimal("20.00"),
                LocalDate.now());

        ResponseEntity<Void> deleteResponse = restTemplate.exchange("/transaction/" + id, HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());

        ResponseEntity<Void> getResponse = restTemplate.exchange("/transaction/" + id, HttpMethod.GET,
                new HttpEntity<>(headers), Void.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    /**
     * Nem létező (vagy más userhez tartozó) tranzakció id-ra 404-et ad
     * vissza, és nem dob 500-at
     */
    @Test
    void deleteTransactionById_returnsNotFoundForNonexistentId() {
        ResponseEntity<Void> response = restTemplate.exchange("/transaction/999999", HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ---- GET /transaction/history ----

    /**
     * A history végpont a name query paraméter alapján szűri a listát
     */
    @Test
    void listTransactionHistory_filtersByName() {
        createTransactionViaApi("Groceries shopping", TransactionTypeEnum.OUTCOME, new BigDecimal("-30.00"),
                LocalDate.now());
        createTransactionViaApi("Salary payment", TransactionTypeEnum.INCOME, new BigDecimal("2000.00"),
                LocalDate.now());

        ResponseEntity<TransactionResponseDto[]> response = restTemplate.exchange(
                "/transaction/history?name=groceries", HttpMethod.GET, new HttpEntity<>(headers),
                TransactionResponseDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().length);
        assertEquals("Groceries shopping", response.getBody()[0].name());
    }

    /**
     * Szűrés nélkül a user összes tranzakcióját visszaadja
     */
    @Test
    void listTransactionHistory_returnsAllTransactionsWithoutFilter() {
        createTransactionViaApi("first", TransactionTypeEnum.INCOME, new BigDecimal("10.00"), LocalDate.now());
        createTransactionViaApi("second", TransactionTypeEnum.INCOME, new BigDecimal("20.00"), LocalDate.now());

        ResponseEntity<TransactionResponseDto[]> response = restTemplate.exchange("/transaction/history",
                HttpMethod.GET, new HttpEntity<>(headers), TransactionResponseDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().length);
    }

    // ---- Security határ - minden végpont auth cookie nélkül ----

    /**
     * Auth cookie nélkül minden /transaction végpont elutasításra kerül,
     * mielőtt a controller lefutna
     */
    @ParameterizedTest(name = "{0} {1} auth cookie nélkül elutasítva")
    @MethodSource("transactionEndpoints")
    void transactionEndpoints_areRejectedWithoutAuthCookie(HttpMethod method, String path) {
        ResponseEntity<Void> response = restTemplate.exchange(path, method, null, Void.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    private static Stream<Arguments> transactionEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/transaction"),
                Arguments.of(HttpMethod.PUT, "/transaction/1"),
                Arguments.of(HttpMethod.GET, "/transaction/sum"),
                Arguments.of(HttpMethod.GET, "/transaction/last"),
                Arguments.of(HttpMethod.GET, "/transaction/1"),
                Arguments.of(HttpMethod.DELETE, "/transaction/1"),
                Arguments.of(HttpMethod.GET, "/transaction/history"));
    }
}
