package com.starbuck.moneytracker.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.commands.BalanceSyncCommand;
import com.starbuck.moneytracker.entity.BalanceSync;
import com.starbuck.moneytracker.repository.BalanceSyncRepository;
import com.starbuck.moneytracker.util.TransactionFactory;

@Service
public class BalanceSyncService {

    private final WalletService walletService;
    private final TransactionService transactionService;
    private final BalanceSyncRepository balanceSyncRepo;
    private final TransactionFactory transactionFactory;

    public BalanceSyncService(WalletService walletService, TransactionService transactionService,
            BalanceSyncRepository balanceSyncRepo, TransactionFactory transactionFactory) {
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.balanceSyncRepo = balanceSyncRepo;
        this.transactionFactory = transactionFactory;
    }

    /**
     * Walletben található összes pénzt összeveti a user által megadott, valós
     * pénzösszeggel, az eltérést rögzíti
     * 
     * @param command
     */
    @Transactional
    public void syncWallet(BalanceSyncCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("BalanceSyncCommand is null");
        }

        // Zárolja a walletet, így egy dupla beküldés nem hozhat létre két korrekciót:
        // (a második kérés megvárja az elsőt, és már a korrigált egyenleget látja)
        var wallet = walletService.getWalletByIdForUpdate(command.getWalletId());
        var balanceSync = new BalanceSync(command.getSyncDate(), wallet, command.getBalanceFromUser());

        var balanceOfWalletInDb = walletService.getBalanceForWallet(command.getWalletId());
        var balancesDiffer = balanceOfWalletInDb.compareTo(command.getBalanceFromUser()) != 0;
        if (balancesDiffer) {
            var transactionSaveCommand = this.transactionFactory.createTransactionEntryFromBalanceChange(command,
                    wallet,
                    balanceOfWalletInDb);
            var transaction = transactionService.createTransaction(transactionSaveCommand);
            balanceSync.setSyncTransaction(transaction);
        }

        balanceSyncRepo.save(balanceSync);
    }
}
