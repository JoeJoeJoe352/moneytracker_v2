package com.starbuck.moneytracker.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.commands.BalanceSyncCommand;
import com.starbuck.moneytracker.entity.BalanceSync;
import com.starbuck.moneytracker.repository.BalanceSyncRepository;
import com.starbuck.moneytracker.util.TransactionFactory;
import com.starbuck.moneytracker.util.UserDateUtil;

@Service
public class BalanceSyncService {

    private final WalletService walletService;
    private final TransactionService transactionService;
    private final BalanceSyncRepository balanceSyncRepo;
    private final TransactionFactory transactionFactory;
    private final UserDateUtil userDateUtil;

    public BalanceSyncService(WalletService walletService, TransactionService transactionService,
            BalanceSyncRepository balanceSyncRepo, TransactionFactory transactionFactory,
            UserDateUtil userDateUtil) {
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.balanceSyncRepo = balanceSyncRepo;
        this.transactionFactory = transactionFactory;
        this.userDateUtil = userDateUtil;
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
        // TODO háromszor kéri le a walletet id alapján. Itt, a getBalanceForWallet-ban
        // és a createtransaction-ben
        var wallet = walletService.getWalletByIdForUpdate(command.getWalletId());

        // User időzónája szerinti dátummal mentjük el a tranzakciót. Mivel az elavultság jelző is frontenden számolódik, ezért 
        // A user mai napja a kliens által küldött időzóna alapján. Szándékosan nem validáljuk:
        // a zóna csak a kliensen ismert, és legfeljebb ±1 nap eltérést okozhat a saját adataiban
        LocalDate date = userDateUtil.today();
        var balanceSync = new BalanceSync(date, wallet, command.getBalanceFromUser());

        var balanceOfWalletInDb = walletService.getBalanceForWallet(command.getWalletId());
        var balancesDiffer = balanceOfWalletInDb.compareTo(command.getBalanceFromUser()) != 0;
        if (balancesDiffer) {
            var transactionSaveCommand = this.transactionFactory.createTransactionEntryFromBalanceChange(wallet,
                    command.getBalanceFromUser(), balanceOfWalletInDb, date);
            var transaction = transactionService.createTransaction(transactionSaveCommand);
            balanceSync.setSyncTransaction(transaction);
        }

        balanceSyncRepo.save(balanceSync);
    }
}
