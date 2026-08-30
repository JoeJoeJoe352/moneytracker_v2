package com.starbuck.moneytracker.service;

import java.util.List;

import org.hibernate.NonUniqueObjectException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.commands.CategoryCreateCommand;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.enum_entites.LangEnum;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.util.CurrentUserUtil;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CurrentUserUtil currentUser;
    private final ConversionService conversionService;

    public CategoryService(CategoryRepository categoryRepository, CurrentUserUtil currentUser,
            ConversionService conversionService) {
        this.categoryRepository = categoryRepository;
        this.currentUser = currentUser;
        this.conversionService = conversionService;
    }

    /**
     * Létrehoz egy új kategóriát a user számára
     * 
     * @param command
     * @return
     */
    @Transactional
    public Category createCategory(CategoryCreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Category is null");
        }
        LangEnum currentLang = conversionService.convert(LocaleContextHolder.getLocale(), LangEnum.class);
        User user = currentUser.getUser();

        if (this.categoryRepository.isUserHaveThisCategoryName(command.getName(), user.getId(), currentLang)) {
            throw new NonUniqueObjectException("Category with this name already exists for the user", null,
                    command.getName());
        }
        Category categoryModel = new Category(
                command.getName(),
                user,
                currentLang);

        return this.categoryRepository.save(categoryModel);
    }

    /**
     * Kilistázza a közös és a user saját kategóriáit
     * 
     * @return
     */
    public List<Category> listCategoriesForUser() {
        return categoryRepository.findAllForUser(
                currentUser.getUser().getId(),
                conversionService.convert(LocaleContextHolder.getLocale(), LangEnum.class));
    }
}
