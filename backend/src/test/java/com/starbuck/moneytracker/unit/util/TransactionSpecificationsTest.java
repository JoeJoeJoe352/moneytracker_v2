package com.starbuck.moneytracker.unit.util;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.util.TransactionSpecifications;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@ExtendWith(MockitoExtension.class)
class TransactionSpecificationsTest {

    @Mock
    private Root<Transaction> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    /**
     * Ha nincs név megadva, null predikátummal tér vissza, és hozzá sem nyúl a
     * root/criteriaBuilderhez
     */
    @Test
    void hasName_returnsNullWhenNameIsNull() {
        Specification<Transaction> spec = TransactionSpecifications.hasName(null);

        Predicate result = spec.toPredicate(root, query, cb);

        assertNull(result);
        verifyNoInteractions(root, cb);
    }

    /**
     * Ha van megadva név, kis-nagybetű érzéketlen, mindkét oldalról wildcardos
     * LIKE predikátumot épít
     */
    @SuppressWarnings("unchecked")
    @Test
    void hasName_buildsCaseInsensitiveLikePredicateWhenNameProvided() {
        Path<String> namePath = mock(Path.class);
        Expression<String> loweredName = mock(Expression.class);
        Predicate likePredicate = mock(Predicate.class);

        when(root.<String>get("name")).thenReturn(namePath);
        when(cb.lower(namePath)).thenReturn(loweredName);
        when(cb.like(loweredName, "%groceries%")).thenReturn(likePredicate);

        Specification<Transaction> spec = TransactionSpecifications.hasName("Groceries");
        Predicate result = spec.toPredicate(root, query, cb);

        assertSame(likePredicate, result);
    }

    /**
     * Ha nincs dátum megadva, null predikátummal tér vissza, és hozzá sem nyúl
     * a root/criteriaBuilderhez
     */
    @Test
    void hasDate_returnsNullWhenDateIsNull() {
        Specification<Transaction> spec = TransactionSpecifications.hasDate(null);

        Predicate result = spec.toPredicate(root, query, cb);

        assertNull(result);
        verifyNoInteractions(root, cb);
    }

    /**
     * Ha van megadva dátum, egyenlőség predikátumot épít a transactionDate
     * mezőre
     */
    @SuppressWarnings("unchecked")
    @Test
    void hasDate_buildsEqualsPredicateWhenDateProvided() {
        LocalDate date = LocalDate.of(2026, 6, 25);
        Path<LocalDate> datePath = mock(Path.class);
        Predicate equalsPredicate = mock(Predicate.class);

        when(root.<LocalDate>get("transactionDate")).thenReturn(datePath);
        when(cb.equal(datePath, date)).thenReturn(equalsPredicate);

        Specification<Transaction> spec = TransactionSpecifications.hasDate(date);
        Predicate result = spec.toPredicate(root, query, cb);

        assertSame(equalsPredicate, result);
    }

    /**
     * A userId szűrés nem opcionális, mindig egyenlőség predikátumot épít a
     * user.id mezőre
     */
    @SuppressWarnings("unchecked")
    @Test
    void hasUserId_buildsEqualsPredicateOnUserId() {
        Long userId = 42L;
        Path<Object> userPath = mock(Path.class);
        Path<Object> userIdPath = mock(Path.class);
        Predicate equalsPredicate = mock(Predicate.class);

        when(root.<Object>get("user")).thenReturn(userPath);
        when(userPath.<Object>get("id")).thenReturn(userIdPath);
        when(cb.equal(userIdPath, userId)).thenReturn(equalsPredicate);

        Specification<Transaction> spec = TransactionSpecifications.hasUserId(userId);
        Predicate result = spec.toPredicate(root, query, cb);

        assertSame(equalsPredicate, result);
    }
}
