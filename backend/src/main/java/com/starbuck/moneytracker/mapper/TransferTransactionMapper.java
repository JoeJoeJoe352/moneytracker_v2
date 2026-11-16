package com.starbuck.moneytracker.mapper;

import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.commands.TransferTransactionCreateCommand;
import com.starbuck.moneytracker.commands.TransferTransactionUpdateCommand;
import com.starbuck.moneytracker.dto.TransferTransactionCreateDto;
import com.starbuck.moneytracker.dto.TransferTransactionUpdateDto;

@Component
public class TransferTransactionMapper {

    /**
     * Átvezetés létrehozási kérésből command-ot készít
     *
     * @param request
     * @return
     */
    public TransferTransactionCreateCommand fromCreateRequest(TransferTransactionCreateDto request) {
        return new TransferTransactionCreateCommand(
                request.srcTransactionName(),
                request.srcWalletId(),
                request.dstTransactionName(),
                request.dstWalletId(),
                request.transactionDate(),
                request.priceSum());
    }

    /**
     * Átvezetés módosítási kérésből command-ot készít
     *
     * @param request
     * @return
     */
    public TransferTransactionUpdateCommand fromUpdateRequest(TransferTransactionUpdateDto request) {
        return new TransferTransactionUpdateCommand(
                request.srcTransactionName(),
                request.srcTransactionId(),
                request.dstTransactionName(),
                request.dstTransactionId(),
                request.transactionDate(),
                request.priceSum());
    }
}
