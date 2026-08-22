package com.starbuck.moneytracker.service;

import java.util.List;

import org.hibernate.NonUniqueObjectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.starbuck.moneytracker.commands.CategoryCreateCommand;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.enum_entites.LangEnum;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.util.CurrentUserUtil;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    CurrentUserUtil currentUser;

    public static final String HU_LANG = "hu";

    /**
     * Létrehoz egy új kategóriát a user számára
     * 
     * @param command
     * @return
     */
    public Category createCategory(CategoryCreateCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Category is null");
        }

        User user = currentUser.getUser();

        if (this.categoryRepository.isUserHaveThisCategoryName(command.getName(), user.getId(), LangEnum.HU)) {
            throw new NonUniqueObjectException("Category with this name already exists for the user", null,
                    command.getName());
        }

        Category categoryModel = new Category(
                command.getName(),
                user,
                LangEnum.HU // TODO ez dinamikussá tenni később
        );

        return this.categoryRepository.save(categoryModel);
    }

    /**
     * Kilistázza a közös és a user saját kategóriáit
     * 
     * @return
     */
    public List<Category> listCategories() {
        return categoryRepository.findAllForUser(
                currentUser.getUser().getId(),
                LangEnum.HU // TODO ezt majd dinamikussá tenni
        );
    }
}
