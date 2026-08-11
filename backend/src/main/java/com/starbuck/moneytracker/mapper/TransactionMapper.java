package com.starbuck.moneytracker.mapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.dto.TransactionDetailCreateDto;
import com.starbuck.moneytracker.dto.TransactionDetailResponseDto;
import com.starbuck.moneytracker.dto.TransactionResponseDto;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionDetailCategory;

@Component
public class TransactionMapper {

    /**
     * Átalakít egy tranzakciót DTO-ra
     * 
     * @param entity
     * @return
     */
    public TransactionResponseDto toDto(Transaction entity) {
        if (entity == null)
            return null;

        Set<TransactionDetailResponseDto> detailDto = entity.getTransactionDetails().stream()
                .map(detail -> new TransactionDetailResponseDto(
                        detail.getName(),
                        detail.getPrice(),
                        detail.getWeight(),
                        detail.getUnitPrice(),
                        detail.isComplexPriceMode(),
                        null // TODO ezt megcsinálni
                ))
                .collect(Collectors.toSet());

        TransactionResponseDto dto = new TransactionResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getPriceSum(),
                entity.getTransactionDate(),
                entity.getTransactionType(),
                entity.isComplexTransaction(),
                detailDto);

        return dto;
    }

    /**
     * Átalakít több tranzakciót DTO-ra
     * 
     * @param entities
     * @return
     */
    public List<TransactionResponseDto> toDtoList(List<Transaction> entities) {
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Átalakít egy DTO-t egy tranzakcióra
     * 
     * @param request
     * @return
     */
    public Transaction fromTransactionCreateRequest(@NonNull TransactionCreateRequest request) {
        Transaction entity = new Transaction();
        entity.setName(request.name());
        entity.setTransactionDate(request.transactionDate());
        entity.setTransactionType(request.transactionType());

        return entity;
    }

    /**
     * Átalakít egy DTO-t egy Detail-é
     * 
     * @param request
     * @return
     */
    public TransactionDetail fromDetailCreateRequest(@NonNull TransactionDetailCreateDto request) {
        TransactionDetail entity = new TransactionDetail();
        entity.setName(request.name());
        entity.setPrice(request.price());
        entity.setWeight(request.weight());
        entity.setUnitPrice(request.unitPrice());

        // Kategória dummy objectek létrehozása, összepárosítása a hozzá tartozó detaillal
        List<TransactionDetailCategory> categories = request.categories().stream().map((category) -> {
            Category tempCategoryObject = new Category();
            tempCategoryObject.setId(category);
            var transactionDetailCategoryObject = new TransactionDetailCategory();
            transactionDetailCategoryObject.setCategory(tempCategoryObject);
            return transactionDetailCategoryObject;
        }).collect(Collectors.toList());

        entity.setCategoryLinks(categories);

        return entity;
    }

    /**
     * Átalakítja a kapott DTO-kat detail-ekké
     *
     * @param requests
     * @return
     */
    public List<TransactionDetail> fromDetailCreateRequestList(@NonNull List<TransactionDetailCreateDto> requests) {
        return requests.stream()
                .map(this::fromDetailCreateRequest)
                .collect(Collectors.toList());
    }
}
