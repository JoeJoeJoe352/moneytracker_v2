package com.starbuck.moneytracker.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.dto.HistoryResponseDto;
import com.starbuck.moneytracker.dto.PageDataResponseDto;
import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.dto.TransactionDetailCreateRequestDto;
import com.starbuck.moneytracker.dto.TransactionDetailEditResponseDto;
import com.starbuck.moneytracker.dto.TransactionDetailResponseDto;
import com.starbuck.moneytracker.dto.TransactionEditResponseDto;
import com.starbuck.moneytracker.dto.TransactionResponseDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;

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
        if (entity == null) return null;

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
                walletDto,
                entity.getSpecialType());

        return dto;
    }

    /**
     * Tranzakció adatok szekresztő felületekhez
     *
     * @param entity
     * @return
     */
    public TransactionEditResponseDto toEditDto(Transaction entity) {
        if (entity == null) return null;

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
                entity.getWallet().getId(),
                entity.getSpecialType());

        return dto;
    }

    /**
     * Átalakít több tranzakciót DTO-ra
     *
     * @param entities
     * @return
     */
    public HistoryResponseDto toDtoList(List<Transaction> entities, PageDataResponseDto pageData) {
        var transactions = entities.stream().map((entity) -> this.toDto(entity)).collect(Collectors.toList());

        return new HistoryResponseDto(transactions, pageData);
    }

    /**
     * Átalakít egy tranzakció oldalt DTO-ra, a lapozási adatokkal együtt
     *
     * @param page
     * @return
     */
    public HistoryResponseDto toDtoList(Page<Transaction> page) {
        PageDataResponseDto pageData = new PageDataResponseDto(page.getTotalElements(), page.getSize());

        return this.toDtoList(page.getContent(), pageData);
    }

    /**
     * Átalakít egy tranzakció DTO-t egy tranzakció frissítő commandra
     *
     * @param request
     * @return
     */
    public TransactionSaveCommand fromTransactionSaveRequest(@NonNull TransactionCreateRequest request) {
        List<TransactionDetailSaveCommand> detailCommands =
                this.fromDetailCreateRequest(request.transactionDetails(), request.transactionType());

        TransactionSaveCommand command = new TransactionSaveCommand(
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
            @NonNull List<TransactionDetailCreateRequestDto> detailDtos, TransactionTypeEnum type) {
        return detailDtos.stream()
                .map((detail) -> {
                    if (detail.unitPrice() != null || detail.weight() != null) {
                        return new TransactionDetailSaveCommand(
                                detail.name(), detail.weight(), detail.unitPrice(), detail.categories());
                    } else {
                        return new TransactionDetailSaveCommand(
                                detail.name(), detail.price(), detail.categories(), type);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Db-ből származó entitást átalakít egy update command-á (az szigorúbb
     * szabályozású, kötelező a detail lista)
     *
     * @param transaction
     * @return
     */
    public TransactionSaveCommand entityToCommand(Transaction transaction) {
        List<TransactionDetailSaveCommand> details = transaction.getTransactionDetails().stream()
                .map((detail) -> {
                    if (detail.isComplexPriceMode()) {
                        return new TransactionDetailSaveCommand(
                                detail.getName(), detail.getWeight(), detail.getUnitPrice(), categoryIdsOf(detail));
                    }
                    return new TransactionDetailSaveCommand(
                            detail.getName(),
                            detail.getPrice(),
                            categoryIdsOf(detail),
                            transaction.getTransactionType());
                })
                .collect(Collectors.toList());

        return new TransactionSaveCommand(
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
