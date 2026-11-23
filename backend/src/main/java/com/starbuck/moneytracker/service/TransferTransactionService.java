package com.starbuck.moneytracker.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.commands.TransferTransactionCreateCommand;
import com.starbuck.moneytracker.commands.TransferTransactionUpdateCommand;
import com.starbuck.moneytracker.entity.TransferTransactionSide;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.repository.TransferTransactionSidesRepository;

@Service
public class TransferTransactionService {

    private final TransactionService transactionService;
    private final TransferTransactionSidesRepository repository;

    public TransferTransactionService(
            TransactionService transactionService, TransferTransactionSidesRepository repository) {
        this.transactionService = transactionService;
        this.repository = repository;
    }

    /**
     * Átvezetés létrehozása: a forrás walletben kiadás, a cél walletben bevétel
     * keletkezik, és a kettőt összekötjük. Egy tranzakcióban fut, így vagy mindkét
     * oldal és az összekötés is elmentődik, vagy egyik sem
     */
    @Transactional
    public TransferTransactionSide createLinkedTransaction(TransferTransactionCreateCommand command) {
        TransactionSaveCommand srcCommand = new TransactionSaveCommand(
                        command.getSrcTransactionName(),
                        TransactionTypeEnum.OUTCOME.applySign(command.getPriceSum()),
                        command.getTransactionDate(),
                        TransactionTypeEnum.OUTCOME,
                        List.of(),
                        List.of(),
                        command.getSrcWalletId())
                .setTransferSpecialType();

        TransactionSaveCommand dstCommand = new TransactionSaveCommand(
                        command.getDstTransactionName(),
                        TransactionTypeEnum.INCOME.applySign(command.getPriceSum()),
                        command.getTransactionDate(),
                        TransactionTypeEnum.INCOME,
                        List.of(),
                        List.of(),
                        command.getDstWalletId())
                .setTransferSpecialType();

        TransactionPair pair = transactionService.createTransactionPair(srcCommand, dstCommand);
        TransferTransactionSide sideObj = new TransferTransactionSide(pair.srcTransaction(), pair.dstTransaction());
        return repository.save(sideObj);
    }

    public void updateLinkedTransaction(long transferId, TransferTransactionUpdateCommand command) {}
}
