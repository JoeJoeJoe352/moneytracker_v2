package com.starbuck.moneytracker.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.commands.AbstractTransferTransactionSaveCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.commands.TransferTransactionCreateCommand;
import com.starbuck.moneytracker.commands.TransferTransactionUpdateCommand;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransferTransactionSide;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.exception.EntityNotFoundException;
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
        TransactionSaveCommand srcCommand = createSideCommand(
                command.getSrcTransactionName(), TransactionTypeEnum.OUTCOME, command.getSrcWalletId(), command);
        TransactionSaveCommand dstCommand = createSideCommand(
                command.getDstTransactionName(), TransactionTypeEnum.INCOME, command.getDstWalletId(), command);

        TransactionPair pair = transactionService.createTransactionPair(srcCommand, dstCommand);
        TransferTransactionSide sideObj = new TransferTransactionSide(pair.srcTransaction(), pair.dstTransaction());
        return repository.save(sideObj);
    }

    /**
     * Átvezetés módosítása: a két oldal neve, dátuma és összege együtt változik. A
     * walletek nem módosíthatók. Egy tranzakcióban fut, így vagy mindkét oldal
     * módosul, vagy egyik sem
     *
     * @param transferId az átvezetés (TransferTransactionSide) id-ja
     * @throws EntityNotFoundException ha nincs ilyen átvezetés, vagy nem a
     *                                 bejelentkezett useré
     */
    @Transactional
    public void updateLinkedTransaction(long transferId, TransferTransactionUpdateCommand command) {
        TransferTransactionSide side = getActiveTransfer(transferId);

        Transaction src = side.getSrcTransaction();
        Transaction dst = side.getDstTransaction();

        TransactionSaveCommand srcCommand = createSideCommand(
                command.getSrcTransactionName(),
                TransactionTypeEnum.OUTCOME,
                src.getWallet().getId(),
                command);
        TransactionSaveCommand dstCommand = createSideCommand(
                command.getDstTransactionName(),
                TransactionTypeEnum.INCOME,
                dst.getWallet().getId(),
                command);

        // a user jogosultságát a TransactionService ellenőrzi mindkét oldalra
        transactionService.updateTransactionPair(src.getId(), srcCommand, dst.getId(), dstCommand);
    }

    /**
     * Átvezetés törlése: mindkét oldal törlődik (soft delete). Az összekötés
     * megmarad, de a törölt átvezetést a lekérdezések már nem találják meg.
     *
     * @param transferId az átvezetés (TransferTransactionSide) id-ja
     * @throws EntityNotFoundException ha nincs ilyen (aktív) átvezetés, vagy nem a
     *                                 bejelentkezett useré
     */
    @Transactional
    public void deleteLinkedTransaction(long transferId) {
        TransferTransactionSide side = getActiveTransfer(transferId);

        // a user jogosultságát a TransactionService ellenőrzi mindkét oldalra
        transactionService.deleteTransactionPair(
                side.getSrcTransaction().getId(), side.getDstTransaction().getId());
    }

    /**
     * Az átvezetés, aminek a megadott tranzakció az egyik oldala (a listában a user egy oldalra
     * kattint, nem az átvezetésre)
     *
     * @param transactionId az átvezetés forrás vagy cél oldali tranzakciójának id-ja
     * @throws EntityNotFoundException ha nincs ilyen tranzakció, nem a bejelentkezett useré, vagy
     *                                 nem tartozik aktív átvezetéshez
     */
    @Transactional(readOnly = true)
    public TransferTransactionSide getLinkedTransactionByTransactionId(long transactionId) {
        // a user jogosultságát a TransactionService ellenőrzi (más user tranzakciója nem található)
        transactionService.getTransactionByIdForActualUser(transactionId);

        return repository
                .findActiveBySideTransactionId(transactionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transfer transaction not found for transaction: " + transactionId));
    }

    /**
     * Az adott id-jú átvezetés, ha egyik oldala sincs törölve
     *
     * @throws EntityNotFoundException ha nincs ilyen aktív átvezetés
     */
    private TransferTransactionSide getActiveTransfer(long transferId) {
        return repository
                .findActiveById(transferId)
                .orElseThrow(() -> new EntityNotFoundException("Transfer transaction not found: " + transferId));
    }

    /**
     * Az átvezetés egyik oldalának mentési parancsa. Az összeg a típusnak megfelelő
     * előjelet kapja (forrás: negatív, cél: pozitív)
     */
    private TransactionSaveCommand createSideCommand(
            String name, TransactionTypeEnum type, Long walletId, AbstractTransferTransactionSaveCommand command) {
        return new TransactionSaveCommand(
                        name,
                        type.applySign(command.getPriceSum()),
                        command.getTransactionDate(),
                        type,
                        List.of(),
                        List.of(),
                        walletId)
                .setTransferSpecialType();
    }
}
