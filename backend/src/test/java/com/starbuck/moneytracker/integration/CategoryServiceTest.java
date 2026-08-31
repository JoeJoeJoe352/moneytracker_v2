package com.starbuck.moneytracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Locale;

import org.hibernate.NonUniqueObjectException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.starbuck.moneytracker.commands.CategoryCreateCommand;
import com.starbuck.moneytracker.commands.UserCreateCommand;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.enum_entites.LangEnum;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.service.CategoryService;
import com.starbuck.moneytracker.service.UserService;
import com.starbuck.moneytracker.testsupport.MySqlContainerTest;
import com.starbuck.moneytracker.util.CurrentUserUtil;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CategoryServiceTest extends MySqlContainerTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService categoryService;

    private User user = null;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private WalletRepository walletRepo;

    @MockitoBean
    private CurrentUserUtil currentUser;

    @Autowired
    private UserService userService;

    @BeforeAll
    void beforeAll() {
        UserCreateCommand user = new UserCreateCommand("testuser", "teszt@email.com", "password");
        this.user = userService.createUser(user);
    }

    @AfterAll
    void afterAll() {
        var wallet = walletRepo.findAll();
        assertEquals(1, wallet.size());
        walletRepo.delete(wallet.get(0));
        userRepo.delete(this.user);
    }

    @BeforeEach
    void beforeEach() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);
        // LocaleContextHolder.getLocale() a JVM alapértelmezett locale-jára esik
        // vissza, ha nincs HTTP request kontextus, ezért van inkább beállítva itt
        LocaleContextHolder.setLocale(Locale.forLanguageTag("hu"));
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("createCategory - Null bemenet esetén exception")
    void testCreateCategoryWithNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(null);
        });
    }

    @Test
    @DisplayName("createCategory - Sikeres létrehozás")
    void testCreateCategory_success() {
        var categoryCommand = new CategoryCreateCommand("tesztCategory");
        var savedCategory = categoryService.createCategory(categoryCommand);

        assertEquals("tesztCategory", savedCategory.getName());
        assertEquals(this.user.getUsername(), savedCategory.getUser().getUsername());

        categoryRepository.delete(savedCategory);
    }

    @Test
    @DisplayName("createCategory - Nem unique kategória a usernél")
    void testCreateCategoryUserHasCategoryWithSameName_throws() {
        // Given
        var categoryCommand = new CategoryCreateCommand("tesztCategory");
        var savedCategory = categoryService.createCategory(categoryCommand);

        assertEquals("tesztCategory", savedCategory.getName());

        // when
        assertThrows(NonUniqueObjectException.class, () -> {
            categoryService.createCategory(categoryCommand);
        });

        // then
        categoryRepository.delete(savedCategory);
    }

    @Test
    @DisplayName("createCategory - Nem engedi felvenni ugyanazt a kategóriát, ha már van közösben olyan nevű")
    void testCreateCategoryCategoryExistsAnotherUser_throws() {
        // Given
        // közvetlenül repo-ba mentjük, mert a service-ben muszáj lenne usernek lennie
        var savedCategoryCommon = categoryRepository.save(new Category("tesztCategory", null, LangEnum.HU));

        var categoryCommand = new CategoryCreateCommand("tesztCategory");

        // WHEN
        assertThrows(NonUniqueObjectException.class, () -> {
            categoryService.createCategory(categoryCommand);
        });

        // THEN
        categoryRepository.delete(savedCategoryCommon);
    }

    @Test
    @DisplayName("ListCategories - Kilistázza a user összes saját és a közös kategóriákat")
    void testListCategories() {
        // given
        UserCreateCommand user = new UserCreateCommand("testuser2", "teszt2@email.com", "password2");
        var savedUser2 = userService.createUser(user);

        var categoryCommand = new CategoryCreateCommand("ownCategory");

        var savedCategoryOwn = categoryService.createCategory(categoryCommand);

        var savedCategoryNotOwned = categoryRepository
                .save(new Category("AnotherUserCategory", savedUser2, LangEnum.HU));

        var savedCategoryCommon = categoryRepository.save(new Category("commonCategory", null, LangEnum.HU));

        // when
        List<Category> categories = categoryService.listCategoriesForUser();

        // then
        // migráció felvesz 16 közöset + 2 van, amit használhat
        assertEquals(18, categories.size());
        assertEquals("ownCategory", categories.get(16).getName());
        assertEquals(this.user.getUsername(), categories.get(16).getUser().getUsername());
        assertEquals("commonCategory", categories.get(17).getName());
        assertNull(categories.get(17).getUser());

        categoryRepository.delete(savedCategoryOwn);
        categoryRepository.delete(savedCategoryNotOwned);
        categoryRepository.delete(savedCategoryCommon);

        walletRepo.delete(walletRepo.findByUserId(savedUser2.getId()).get(0));
        userRepo.delete(savedUser2);
    }
}
