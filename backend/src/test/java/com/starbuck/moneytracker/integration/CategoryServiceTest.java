package com.starbuck.moneytracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.hibernate.NonUniqueObjectException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.service.CategoryService;
import com.starbuck.moneytracker.service.UserService;
import com.starbuck.moneytracker.util.CurrentUserUtil;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CategoryServiceTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService categoryService;

    private User user = null;

    @Autowired
    private UserRepository userRepo;

    @MockitoBean
    private CurrentUserUtil currentUser;

    @Autowired
    private UserService userService;

    @BeforeAll
    void beforeAll() {
        User user = new User("testuser", "password", "teszt@email.com");
        this.user = userService.createUser(user, "password");
    }

    @AfterAll
    void afterAll() {
        userRepo.delete(this.user);
    }

    @BeforeEach
    void mockUserUtil() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);
    }

    @Test
    @DisplayName("createCategory - Null bemenet esetén exception")
    void testCreateCategoryWithNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(null);
        });
    }

    @Test
    @DisplayName("createCategory - Hiányzó név esetén exception")
    void testCreateCategoryMissingData_throws() {
        var category = new Category();
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(category);
        });
    }

    @Test
    @DisplayName("createCategory - Sikeres létrehozás")
    void testCreateCategory_success() {
        var category = new Category();
        category.setName("tesztCategory");
        var savedCategory = categoryService.createCategory(category);

        assertEquals("tesztCategory", savedCategory.getName());
        assertEquals(this.user.getUsername(), savedCategory.getUser().getUsername());

        categoryRepository.delete(savedCategory);
    }

    @Test
    @DisplayName("createCategory - Nem unique kategória a usernél")
    void testCreateCategoryUserHasCategoryWithSameName_throws() {
        // Given
        var category = new Category();
        category.setName("tesztCategory");
        var savedCategory = categoryService.createCategory(category);

        assertEquals("tesztCategory", savedCategory.getName());

        // when
        assertThrows(NonUniqueObjectException.class, () -> {
            categoryService.createCategory(category);
        });

        // then
        categoryRepository.delete(savedCategory);
    }

    @Test
    @DisplayName("createCategory - Engedi felvenni ugyanazt a nevű kategóriát, ha másik usernél van csak")
    void testCreateCategoryCategoryExistsAnotherUser_throws() {
        // Given
        var savedCategoryCommon = categoryRepository.save(new Category("tesztCategory", null));

        var category = new Category();
        category.setName("tesztCategory");
        var savedCategory = categoryService.createCategory(category);

        assertEquals("tesztCategory", savedCategory.getName());

        // then
        categoryRepository.delete(savedCategory);
        categoryRepository.delete(savedCategoryCommon);
    }

    @Test
    @DisplayName("ListCategories - Kilistázza a user összes saját és a közös kategóriákat")
    void testListCategories() {
        // given
        User user = new User("testuser2", "password2", "teszt2@email.com");
        var savedUser2 = userService.createUser(user, "password2");

        var ownCategory = new Category();
        ownCategory.setName("ownCategory");
        var savedCategoryOwn = categoryService.createCategory(ownCategory);

        var savedCategoryNotOwned = categoryRepository.save(new Category("AnotherUserCategory", savedUser2));

        var savedCategoryCommon = categoryRepository.save(new Category("commonCategory", null));

        // when
        List<Category> categories = categoryService.listCategories();

        // then
        assertEquals(2, categories.size());
        assertEquals("ownCategory", categories.get(0).getName());
        assertEquals(this.user.getUsername(), categories.get(0).getUser().getUsername());
        assertEquals("commonCategory", categories.get(1).getName());
        assertNull(categories.get(1).getUser());

        categoryRepository.delete(savedCategoryOwn);
        categoryRepository.delete(savedCategoryNotOwned);
        categoryRepository.delete(savedCategoryCommon);
        userRepo.delete(savedUser2);
    }
}
