package com.starbuck.moneytracker.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

import com.starbuck.moneytracker.dto.LoginRequestDto;
import com.starbuck.moneytracker.dto.RegisterRequestDto;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.testsupport.MySqlContainerTest;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class GlobalExceptionHandlerE2ETest extends MySqlContainerTest {

    private static final ParameterizedTypeReference<Map<String, String>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    private User user;
    private HttpHeaders headers;

    @BeforeEach
    void registerAndLoginRealUser() {
        RegisterRequestDto registerRequest =
                new RegisterRequestDto("e2eErrorUser", "password123", "e2eerror@email.com");
        restTemplate.postForEntity("/auth/register", registerRequest, Void.class);

        LoginRequestDto loginRequest = new LoginRequestDto("e2eerror@email.com", "password123");
        ResponseEntity<Map<String, String>> loginResponse =
                restTemplate.exchange("/auth/login", HttpMethod.POST, new HttpEntity<>(loginRequest), MAP_TYPE);
        String setCookieHeader = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);

        this.user = userRepository.findByEmail("e2eerror@email.com").orElseThrow();

        this.headers = new HttpHeaders();
        this.headers.setContentType(MediaType.APPLICATION_JSON);
        this.headers.add(HttpHeaders.COOKIE, setCookieHeader.split(";")[0]);
    }

    @AfterEach
    void cleanupCreatedData() {
        walletRepository.deleteAll(walletRepository.findByUserId(this.user.getId()));
        userRepository.delete(this.user);
    }

    /**
     * Nem létező végpontra 404-et ad vissza (nem 500-at), a kért útvonallal az
     * üzenetben
     */
    @Test
    void unknownEndpoint_returnsNotFound() {
        ResponseEntity<Map<String, String>> response =
                restTemplate.exchange("/nonexistent", HttpMethod.GET, new HttpEntity<>(headers), MAP_TYPE);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Endpoint not found: /nonexistent", response.getBody().get("message"));
    }

    /**
     * Létező végpont nem támogatott methoddal 405-öt ad vissza (ErrorResponse
     * ágon keresztül)
     */
    @Test
    void unsupportedMethod_returnsMethodNotAllowed() {
        ResponseEntity<Map<String, String>> response =
                restTemplate.exchange("/wallet", HttpMethod.DELETE, new HttpEntity<>(headers), MAP_TYPE);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody().get("message"));
    }

    /**
     * Nem szám id esetén 400-at ad vissza a paraméter nevével
     */
    @Test
    void invalidPathVariableType_returnsBadRequest() {
        ResponseEntity<Map<String, String>> response =
                restTemplate.exchange("/transaction/abc", HttpMethod.GET, new HttpEntity<>(headers), MAP_TYPE);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid value for parameter 'id'", response.getBody().get("message"));
    }

    /**
     * Hibás JSON body esetén 400-at ad vissza, a Jackson részletei nélkül
     */
    @Test
    void malformedJsonBody_returnsBadRequest() {
        ResponseEntity<Map<String, String>> response =
                restTemplate.exchange("/category", HttpMethod.POST, new HttpEntity<>("{not json", headers), MAP_TYPE);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed request body", response.getBody().get("message"));
    }

    /**
     * Érvénytelen enum érték esetén 400-at ad vissza az elfogadott értékek
     * listájával, de a Jackson belső részletei (pl. osztálynév) nélkül
     */
    @Test
    void invalidEnumValue_returnsBadRequestWithAcceptedValues() {
        String body = "{\"name\":\"Wallet\",\"currencyCode\":\"XYZ\",\"walletType\":\"DEFAULT\"}";

        ResponseEntity<Map<String, String>> response =
                restTemplate.exchange("/wallet", HttpMethod.POST, new HttpEntity<>(body, headers), MAP_TYPE);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                "not one of the values accepted for Enum class: [HUF, EUR, USD]",
                response.getBody().get("message"));
    }

    /**
     * Nem támogatott Content-Type esetén 415-öt ad vissza (ErrorResponse ágon
     * keresztül)
     */
    @Test
    void unsupportedMediaType_returnsUnsupportedMediaType() {
        HttpHeaders textHeaders = new HttpHeaders();
        textHeaders.addAll(headers);
        textHeaders.setContentType(MediaType.TEXT_PLAIN);

        ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                "/category", HttpMethod.POST, new HttpEntity<>("Groceries", textHeaders), MAP_TYPE);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertNotNull(response.getBody().get("message"));
    }
}
