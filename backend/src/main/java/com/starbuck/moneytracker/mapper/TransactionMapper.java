package com.starbuck.moneytracker.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.commands.TransactionCreateCommand;
import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionUpdateCommand;
import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.dto.TransactionDetailCreateDto;
import com.starbuck.moneytracker.dto.TransactionDetailResponseDto;
import com.starbuck.moneytracker.dto.TransactionResponseDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

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

        List<TransactionDetailResponseDto> detailDto = entity.getTransactionDetails().stream()
                .map(detail -> new TransactionDetailResponseDto(
                        detail.getName(),
                        detail.getPrice(),
                        detail.getWeight(),
                        detail.getUnitPrice(),
                        detail.isComplexPriceMode(),
                        detail.getCategoryIds()))
                .collect(Collectors.toList());

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
     * Átalakít egy tranzakció DTO-t egy tranzakció létrehozó commandra
     * 
     * @param request
     * @return
     */
    public TransactionCreateCommand fromTransactionCreateRequest(@NonNull TransactionCreateRequest request) {
        List<TransactionDetailSaveCommand> detailCommands = this.fromDetailCreateRequest(
                request.transactionDetails(),
                request.transactionType());

        TransactionCreateCommand command = new TransactionCreateCommand(
                request.name(),
                request.globalPrice(),
                request.transactionDate(),
                request.transactionType(),
                detailCommands,
                request.globalCategories());

        return command;
    }

    /**
     * Átalakít egy tranzakció DTO-t egy tranzakció frissítő commandra
     * 
     * @param request
     * @return
     */
    public TransactionUpdateCommand fromTransactionUpdateRequest(@NonNull TransactionCreateRequest request) {
        List<TransactionDetailSaveCommand> detailCommands = this.fromDetailCreateRequest(
                request.transactionDetails(),
                request.transactionType());

        TransactionUpdateCommand command = new TransactionUpdateCommand(
                request.name(),
                request.globalPrice(),
                request.transactionDate(),
                request.transactionType(),
                detailCommands,
                request.globalCategories());

        return command;
    }

    /**
     * Detail Dto-ból Detail létrehozó commandot állít elő
     * 
     * @param detailDtos
     * @param type
     * @return
     */
    public List<TransactionDetailSaveCommand> fromDetailCreateRequest(
            @NonNull List<TransactionDetailCreateDto> detailDtos, TransactionTypeEnum type) {
        return detailDtos.stream().map((detail) -> {
            System.out.println(
                    detail.name() + ": up: " + detail.unitPrice() + ", weight: " + detail.weight());

            if (detail.unitPrice() != null || detail.weight() != null) {
                return new TransactionDetailSaveCommand(
                        detail.name(),
                        detail.weight(),
                        detail.unitPrice(),
                        detail.categories());
            } else {
                return new TransactionDetailSaveCommand(
                        detail.name(),
                        detail.price(),
                        detail.categories(),
                        type);
            }

        }).collect(Collectors.toList());
    }

    /**
     * Db-ből származó entitást átalakít egy update command-á (az szigorúbb
     * szabályozású, kötelező a detail lista)
     * 
     * @param t
     * @return
     */
    public TransactionUpdateCommand entityToCommand(Transaction t) {
        List<TransactionDetailSaveCommand> details = t.getTransactionDetails().stream().map((detail) -> {
            if (detail.getWeight() != null || detail.getUnitPrice() != null) {
                return new TransactionDetailSaveCommand(detail.getName(), detail.getWeight(),
                        detail.getUnitPrice(),
                        detail.getCategoryIds());
            }
            return new TransactionDetailSaveCommand(detail.getName(), detail.getPrice(), List.of(),
                    t.getTransactionType());
        }).collect(Collectors.toList());

        return new TransactionUpdateCommand(t.getName(), t.getPriceSum(), t.getTransactionDate(),
                t.getTransactionType(), details, List.of());
    }
}
