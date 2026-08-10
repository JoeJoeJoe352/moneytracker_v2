package com.starbuck.moneytracker.util;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.dto.TransactionDetailCreateDto;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.mapper.TransactionMapper;

import jakarta.annotation.Nonnull;

@Component
public class TransactionDetailFactory {

    @Autowired
    TransactionMapper mapper;

    /**
     * Előállítja a tranzakcióhoz tartozó detail listát a kérésből.
     * - Ha a kérésben explicit detail lista van, azt alakítja át
     * - Ha nincs, de van globalPrice, abból egy alapértelmezett detailt hoz létre
     * - Ha egyik sincs, hibát dob
     *
     * @param TransactionCreateRequest request
     * @return
     */
    public List<TransactionDetail> factoreDetail(@Nonnull TransactionCreateRequest request) {
        if (!request.transactionDetails().isEmpty()) {
            return mapper.fromDetailCreateRequestList(request.transactionDetails());
        }

        // default detail létrehozása
        // TODO globalCategories
        if (request.globalPrice() != null) {
            return mapper.fromDetailCreateRequestList(List.of(new TransactionDetailCreateDto(
                    request.globalPrice(),
                    TransactionDetail.DEFAULT_DETAIL_NAME,
                    null,
                    null,
                    request.globalCategories())));
        }

        throw new IllegalArgumentException("No detail in list and price is also null. Error!");
    }
}
