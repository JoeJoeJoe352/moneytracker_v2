package com.starbuck.moneytracker.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.commands.RecurringTransactionSaveCommand;
import com.starbuck.moneytracker.dto.CategoryResponseDto;
import com.starbuck.moneytracker.dto.RecurringTransactionResponseDto;
import com.starbuck.moneytracker.dto.RecurringTransactionSaveRequestDto;
import com.starbuck.moneytracker.entity.RecurringTransaction;

@Component
public class RecurringTransactionMapper {

    private final WalletMapper walletMapper;
    private final CategoryMapper categoryMapper;

    public RecurringTransactionMapper(WalletMapper walletMapper, CategoryMapper categoryMapper) {
        this.walletMapper = walletMapper;
        this.categoryMapper = categoryMapper;
    }

    public RecurringTransactionSaveCommand fromSaveRequest(RecurringTransactionSaveRequestDto request) {
        return new RecurringTransactionSaveCommand(
                request.name(),
                request.priceSum(),
                request.startDate(),
                request.recurrenceInterval(),
                request.transactionType(),
                request.categoryIds(),
                request.walletId(),
                request.isRestart());
    }

    public RecurringTransactionResponseDto toDto(RecurringTransaction entity) {
        List<CategoryResponseDto> categories = entity.getCategoryLinks() == null
                ? List.of()
                : entity.getCategoryLinks().stream()
                        .map(link -> categoryMapper.toDto(link.getCategory()))
                        .collect(Collectors.toList());

        return new RecurringTransactionResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getPriceSum(),
                entity.getStartDate(),
                entity.getNextOccurrenceDate(),
                entity.getRecurrenceInterval(),
                entity.getTransactionType(),
                entity.getStatus(),
                categories,
                walletMapper.toDto(entity.getWallet()));
    }

    public List<RecurringTransactionResponseDto> toDtoList(List<RecurringTransaction> entities) {
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
