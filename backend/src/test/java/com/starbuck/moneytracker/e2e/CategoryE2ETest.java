package com.starbuck.moneytracker.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.starbuck.moneytracker.dto.CategoryCreateDto;
import com.starbuck.moneytracker.dto.CategoryResponseDto;
import com.starbuck.moneytracker.dto.LoginRequest;
import com.starbuck.moneytracker.dto.RegisterRequest;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.repository.UserRepository;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class CategoryE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private String authCookie;
    private User user;
    private HttpHeaders headers;

    // A teszt által létrehozott kategória id-k, hogy pontosan takarítani
    // tudjunk utánuk, függetlenül attól, melyik teszt hozta létre őket
    private final List<Long> createdCategoryIds = new ArrayList<>();

    @BeforeEach
    void registerAndLoginRealUser() {
        RegisterRequest registerRequest = new RegisterRequest("e2eCategoryUser", "password123", "password123",
                "e2ecategory@email.com");
        restTemplate.postForEntity("/auth/register", registerRequest, Void.class);

        LoginRequest loginRequest = new LoginRequest("e2eCategoryUser", "password123");
        ResponseEntity<Map<String, String>> loginResponse = restTemplate.exchange("/auth/login", HttpMethod.POST,
                new HttpEntity<>(loginRequest), new ParameterizedTypeReference<Map<String, String>>() {
                });
        String setCookieHeader = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookieHeader);
        this.authCookie = setCookieHeader.split(";")[0];

        this.user = userRepository.findByUsername("e2eCategoryUser");

        this.headers = new HttpHeaders();
        this.headers.setContentType(MediaType.APPLICATION_JSON);
        this.headers.add(HttpHeaders.COOKIE, this.authCookie);
    }

    @AfterEach
    void cleanupCreatedData() {
        createdCategoryIds.forEach(id -> categoryRepository.findById(id).ifPresent(categoryRepository::delete));
        createdCategoryIds.clear();
        userRepository.delete(this.user);
    }

    // ---- POST /category ----

    /**
     * Sikeres létrehozás esetén a válasz a userhez kötött (nem közös) új
     * kategóriát tartalmazza, és az adatbázisban is megjelenik
     */
    @Test
    void createCategory_createsCategoryForAuthenticatedUser() {
        CategoryCreateDto request = new CategoryCreateDto("Groceries");

        ResponseEntity<CategoryResponseDto> response = restTemplate.postForEntity("/category",
                new HttpEntity<>(request, headers), CategoryResponseDto.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        CategoryResponseDto body = response.getBody();
        assertNotNull(body.id());
        assertEquals("Groceries", body.name());
        assertFalse(body.isLangKey());
        createdCategoryIds.add(body.id());

        Category savedCategory = categoryRepository.findById(body.id()).orElseThrow();
        assertEquals(this.user.getId(), savedCategory.getUser().getId());
    }

    /**
     * Hiányzó név esetén (a service dobja az IllegalArgumentException-t) a
     * GlobalExceptionHandler 400-ra fordítja, és nem jön létre kategória
     */
    @Test
    void createCategory_returnsBadRequestWhenNameIsMissing() {
        CategoryCreateDto request = new CategoryCreateDto(null);

        ResponseEntity<Void> response = restTemplate.postForEntity("/category", new HttpEntity<>(request, headers),
                Void.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * Ha a usernek már van ilyen nevű kategóriája, a service
     * NonUniqueObjectException-t dob, amit a GlobalExceptionHandler 409
     * Conflict-ra fordít, és nem jön létre második kategória
     */
    @Test
    void createCategory_returnsConflictForDuplicateCategoryName() {
        CategoryCreateDto request = new CategoryCreateDto("DuplicateCategory");
        ResponseEntity<CategoryResponseDto> firstResponse = restTemplate.postForEntity("/category",
                new HttpEntity<>(request, headers), CategoryResponseDto.class);
        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());
        createdCategoryIds.add(firstResponse.getBody().id());

        ResponseEntity<Void> secondResponse = restTemplate.postForEntity("/category",
                new HttpEntity<>(request, headers), Void.class);

        assertEquals(HttpStatus.CONFLICT, secondResponse.getStatusCode());
    }

    // ---- GET /category ----

    /**
     * A saját kategóriák mellett a közös (user nélküli) kategóriákat is
     * visszaadja
     */
    @Test
    void getCategories_returnsOwnAndCommonCategories() {
        CategoryCreateDto ownRequest = new CategoryCreateDto("OwnCategory");
        ResponseEntity<CategoryResponseDto> ownResponse = restTemplate.postForEntity("/category",
                new HttpEntity<>(ownRequest, headers), CategoryResponseDto.class);
        createdCategoryIds.add(ownResponse.getBody().id());

        Category commonCategory = categoryRepository.save(new Category("CommonCategory", null));
        createdCategoryIds.add(commonCategory.getId());

        ResponseEntity<CategoryResponseDto[]> response = restTemplate.exchange("/category", HttpMethod.GET,
                new HttpEntity<>(headers), CategoryResponseDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<CategoryResponseDto> categories = List.of(response.getBody());

        assertTrue(categories.stream().anyMatch(c -> c.name().equals("OwnCategory") && !c.isLangKey()));
        assertTrue(categories.stream().anyMatch(c -> c.name().equals("CommonCategory") && c.isLangKey()));
    }

    /**
     * Más user saját kategóriáit nem adja vissza
     */
    @Test
    void getCategories_doesNotReturnOtherUsersCategories() {
        User otherUser = new User("e2eOtherCategoryUser", "irrelevantEncodedPassword", "othercategory@email.com");
        otherUser.setUuid();
        otherUser = userRepository.save(otherUser);

        Category otherUsersCategory = categoryRepository.save(new Category("OtherUsersCategory", otherUser));

        try {
            ResponseEntity<CategoryResponseDto[]> response = restTemplate.exchange("/category", HttpMethod.GET,
                    new HttpEntity<>(headers), CategoryResponseDto[].class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            boolean containsOtherUsersCategory = List.of(response.getBody()).stream()
                    .anyMatch(c -> c.name().equals("OtherUsersCategory"));
            assertFalse(containsOtherUsersCategory);
        } finally {
            categoryRepository.delete(otherUsersCategory);
            userRepository.delete(otherUser);
        }
    }

    // ---- Security határ ----

    /**
     * Auth cookie nélkül a /category végpontok is elutasításra kerülnek,
     * mielőtt a controller lefutna
     */
    @ParameterizedTest(name = "{0} {1} auth cookie nélkül elutasítva")
    @MethodSource("categoryEndpoints")
    void categoryEndpoints_areRejectedWithoutAuthCookie(HttpMethod method, String path) {
        ResponseEntity<Void> response = restTemplate.exchange(path, method, null, Void.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    private static Stream<Arguments> categoryEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/category"),
                Arguments.of(HttpMethod.GET, "/category"));
    }
}
