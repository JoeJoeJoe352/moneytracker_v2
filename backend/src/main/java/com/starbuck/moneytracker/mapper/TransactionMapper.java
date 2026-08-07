package com.starbuck.moneytracker.mapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.dto.TransactionDetailCreateDto;
import com.starbuck.moneytracker.dto.TransactionDetailDto;
import com.starbuck.moneytracker.dto.TransactionDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;

@Component
public class TransactionMapper {

    /**
     * Átalakít egy tranzakciót DTO-ra
     * 
     * @param entity
     * @return
     */
    public TransactionDto toDto(Transaction entity) {
        if (entity == null)
            return null;

        Set<TransactionDetailDto> detailDto = entity.getTransactionDetails().stream()
                .map(detail -> new TransactionDetailDto(
                        detail.getName(),
                        detail.getPrice(),
                        detail.getWeight(),
                        detail.getUnitPrice(),
                        detail.isComplexPriceMode()))
                .collect(Collectors.toSet());

        TransactionDto dto = new TransactionDto(
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
    public List<TransactionDto> toDtoList(List<Transaction> entities) {
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

    /**
     * Előállítja a tranzakcióhoz tartozó detail listát a kérésből.
     * - Ha a kérésben explicit detail lista van, azt alakítja át
     * - Ha nincs, de van globalPrice, abból egy alapértelmezett detailt hoz létre
     * - Ha egyik sincs, hibát dob
     *
     * @param request
     * @return
     */
    public List<TransactionDetail> resolveDetails(@NonNull TransactionCreateRequest request) {
        if (!request.transactionDetails().isEmpty()) {
            return fromDetailCreateRequestList(request.transactionDetails());
        }

        // default detail létrehozása
        if (request.globalPrice() != null) {
            return fromDetailCreateRequestList(List.of(new TransactionDetailCreateDto(
                    request.globalPrice(),
                    TransactionDetail.DEFAULT_DETAIL_NAME,
                    null,
                    null)));
        }

        throw new IllegalArgumentException("No detail in list and price is also null. Error!");
    }
}
