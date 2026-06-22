package com.starbuck.moneytracker.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.dto.TransactionDto;
import com.starbuck.moneytracker.entity.Transaction;

@Component
public class TransactionMapper {

    public TransactionDto toDto(Transaction entity) {
        if (entity == null) return null;

        TransactionDto dto = new TransactionDto(
            entity.getId(), 
            entity.getName(), 
            entity.getPriceSum(), 
            entity.getTransactionDate(),
            entity.getTransactionType()
        );

        return dto;
    }

    public List<TransactionDto> toDtoList(List<Transaction> entities) {
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
