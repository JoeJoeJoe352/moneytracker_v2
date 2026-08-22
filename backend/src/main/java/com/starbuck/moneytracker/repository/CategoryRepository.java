package com.starbuck.moneytracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.enum_entites.LangEnum;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Megnézi, hogy a usernek van-e már ilyen néven kategóriája (saját, vagy közös)
     * TODO a nyelvi kulcsok miatt fel lehet venni ugyanazzal a névvel egy közös meg
     * egy saját kategóriát (elvileg, mert a frontend nem engedi)
     * 
     * @param name
     * @param userId
     * @return
     */
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = ?1 AND (c.user.id = ?2 OR (c.user.id IS NULL AND c.lang = ?2)) AND c.status = 0")
    boolean isUserHaveThisCategoryName(String name, long userId, LangEnum lang);

    /**
     * Kilistázza a user saját kategóriáit és a közös kategóriákat (null used_id)
     * 
     * @param userId
     * @return
     */
    @Query("SELECT c FROM Category c WHERE (c.user.id = ?1 OR (c.user.id IS NULL AND c.lang = ?2)) AND c.status = 0")
    List<Category> findAllForUser(long userId, LangEnum lang);

    /**
     * Kilistázza a user saját kategóriáit és a közös kategóriákat (null used_id)
     * 
     * @param userId
     * @return
     */
    @Query("SELECT c FROM Category c WHERE (c.user.id = ?2 OR c.user.id IS NULL) AND c.status = 0 AND c.id in (?1)")
    List<Category> findAllById(List<Long> ids, long userId);
}
