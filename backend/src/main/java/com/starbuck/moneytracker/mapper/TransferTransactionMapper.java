package com.starbuck.moneytracker.mapper;

import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.commands.TransferTransactionCreateCommand;
import com.starbuck.moneytracker.commands.TransferTransactionUpdateCommand;
import com.starbuck.moneytracker.dto.TransferTransactionCreateDto;
import com.starbuck.moneytracker.dto.TransferTransactionResponseDto;
import com.starbuck.moneytracker.dto.TransferTransactionUpdateDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransferTransactionSide;

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
                request.dstTransactionName(),
                request.transactionDate(),
                request.priceSum());
    }

    /**
     * Átvezetésből válasz dto-t készít. Az összeg a cél (bevétel) oldal összege, ami pozitív
     *
     * @param side
     * @return
     */
    public TransferTransactionResponseDto toResponseDto(TransferTransactionSide side) {
        Transaction src = side.getSrcTransaction();
        Transaction dst = side.getDstTransaction();
        return new TransferTransactionResponseDto(
                side.getId(),
                src.getName(),
                src.getWallet().getId(),
                dst.getName(),
                dst.getWallet().getId(),
                dst.getTransactionDate(),
                dst.getPriceSum());
    }
}
