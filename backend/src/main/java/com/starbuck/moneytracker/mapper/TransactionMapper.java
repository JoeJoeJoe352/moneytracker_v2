package com.starbuck.moneytracker.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.commands.TransactionUpdateCommand;
import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.dto.TransactionDetailCreateDto;
import com.starbuck.moneytracker.dto.TransactionDetailEditResponseDto;
import com.starbuck.moneytracker.dto.TransactionDetailResponseDto;
import com.starbuck.moneytracker.dto.TransactionEditResponseDto;
import com.starbuck.moneytracker.dto.TransactionResponseDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

@Component
public class TransactionMapper {

    private final WalletMapper walletMapper;

    public TransactionMapper(WalletMapper walletMapper) {
        this.walletMapper = walletMapper;
    }

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
                .map(detail -> {
                    List<String> categoryNames = detail.getCategoryLinks().stream()
                            .map(link -> link.getCategory().getName())
                            .collect(Collectors.toList());
                    return new TransactionDetailResponseDto(
                            detail.getName(),
                            detail.getPrice(),
                            detail.getWeight(),
                            detail.getUnitPrice(),
                            detail.isComplexPriceMode(),
                            categoryNames);
                })
                .collect(Collectors.toList());

        var walletDto = walletMapper.toDto(entity.getWallet());
        
        TransactionResponseDto dto = new TransactionResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getPriceSum(),
                entity.getTransactionDate(),
                entity.getTransactionType(),
                entity.isComplexTransaction(),
                detailDto,
                walletDto);

        return dto;
    }

    /**
     * Tranzakció adatok szekresztő felületekhez
     * 
     * @param entity
     * @return
     */
    public TransactionEditResponseDto toEditDto(Transaction entity) {
        if (entity == null)
            return null;

        List<TransactionDetailEditResponseDto> detailDto = entity.getTransactionDetails().stream()
                .map(detail -> {
                    return new TransactionDetailEditResponseDto(
                            detail.getName(),
                            detail.getPrice(),
                            detail.getWeight(),
                            detail.getUnitPrice(),
                            detail.isComplexPriceMode(),
                            categoryIdsOf(detail));
                })
                .collect(Collectors.toList());

        TransactionEditResponseDto dto = new TransactionEditResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getPriceSum(),
                entity.getTransactionDate(),
                entity.getTransactionType(),
                entity.isComplexTransaction(),
                detailDto,
                entity.getWallet().getId());

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
     * Átalakít egy tranzakció DTO-t egy tranzakció frissítő commandra
     * 
     * @param request
     * @return
     */
    public TransactionSaveCommand fromTransactionSaveRequest(@NonNull TransactionCreateRequest request) {
        List<TransactionDetailSaveCommand> detailCommands = this.fromDetailCreateRequest(
                request.transactionDetails(),
                request.transactionType());

        TransactionUpdateCommand command = new TransactionUpdateCommand(
                request.name(),
                request.globalPrice(),
                request.transactionDate(),
                request.transactionType(),
                detailCommands,
                request.globalCategories(),
                request.walletId());

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
     * @param transaction
     * @return
     */
    public TransactionUpdateCommand entityToCommand(Transaction transaction) {
        List<TransactionDetailSaveCommand> details = transaction.getTransactionDetails().stream()
                .map((detail) -> {
                    if (detail.isComplexPriceMode()) {
                        return new TransactionDetailSaveCommand(
                                detail.getName(),
                                detail.getWeight(),
                                detail.getUnitPrice(),
                                categoryIdsOf(detail));
                    }
                    return new TransactionDetailSaveCommand(
                            detail.getName(),
                            detail.getPrice(),
                            categoryIdsOf(detail),
                            transaction.getTransactionType());
                }).collect(Collectors.toList());

        return new TransactionUpdateCommand(
                transaction.getName(),
                details.size() > 0 ? null : transaction.getPriceSum(),
                transaction.getTransactionDate(),
                transaction.getTransactionType(),
                details,
                List.of(), // van detail, ezért nem kell kategóriát átadni magán a tranzakción
                transaction.getWallet().getId()); // TODO lehet, hogy nincs betöltve a wallet
    }

    /**
     * Tételhez kapcsolt kategória azonosítók listájával tér vissza.
     *
     * @param detail -> betöltött getCategoryLinks-el
     * @return
     */
    private List<Long> categoryIdsOf(TransactionDetail detail) {
        if (detail.getCategoryLinks() == null) {
            return List.of();
        }
        return detail.getCategoryLinks().stream()
                .map(categoryLink -> categoryLink.getCategory().getId())
                .collect(Collectors.toList());
    }
}
