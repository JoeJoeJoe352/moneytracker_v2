package com.starbuck.moneytracker.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import com.starbuck.moneytracker.commands.CreateWalletCommand;
import com.starbuck.moneytracker.commands.UpdateWalletCommand;
import com.starbuck.moneytracker.dto.WalletListResponseDto;
import com.starbuck.moneytracker.dto.WalletSummaryDto;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.GeneralStatusEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.util.CurrentUserUtil;

import jakarta.persistence.EntityNotFoundException;

@Service
public class WalletService {

    private final WalletRepository walletRepo;
    private final MessageSource messageSource;
    private final CurrentUserUtil userUtil;

    public WalletService(WalletRepository walletRepo, MessageSource messageSource, CurrentUserUtil userUtil) {
        this.walletRepo = walletRepo;
        this.messageSource = messageSource;
        this.userUtil = userUtil;
    }

    /**
     * Létrehoz egy walletet a megadott adatokkal.
     *
     * @param command
     * @return a létrehozott wallet
     */
    public Wallet createWallet(CreateWalletCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("createCommand is null");
        }
        Wallet wallet = new Wallet(command.getName(), command.getUser(), command.getCurrency(), command.getType());
        return walletRepo.save(wallet);
    }

    /**
     * Létrehoz egy alapértelmezett walletet az új felhasználónak.
     *
     * @param registeredUser -> regisztráció most történt meg, még nincs
     *                       currentUser-ben senki
     * @return a létrehozott wallet
     */
    public Wallet createDefaultWallet(User registeredUser) {
        String defaultNameLocalised = messageSource.getMessage("defaultWalletName", null,
                LocaleContextHolder.getLocale());

        CreateWalletCommand command = new CreateWalletCommand(defaultNameLocalised, CurrencyEnum.HUF,
                WalletTypeEnum.DEFAULT, registeredUser);
        return createWallet(command);
    }

    /**
     * Wallet frissítése
     * 
     * @param id
     * @param command
     */
    public void updateWallet(long id, UpdateWalletCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("updateCommand is null");
        }
        Wallet walletFromDb = walletRepo.getWalletById(id, userUtil.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("no wallet found"));

        walletFromDb.setName(command.getName());
        walletFromDb.setType(command.getType());

        walletRepo.save(walletFromDb);
    }

    /**
     * Visszatér a user tárcáival
     */
    public List<WalletListResponseDto> listWalletsForUser() {
        return walletRepo.listWalletsWithSumByUserId(userUtil.getUser().getId());
    }

    /**
     * Visszaadja a user egy walletjét id alapján
     *
     * @param id
     * @return
     */
    public Wallet getWalletById(long id) {
        return walletRepo.getWalletById(id, userUtil.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("no wallet found"));
    }

    /**
     * Visszaadja a user egy walletjét id alapján, és a tranzakció végéig zárolja.
     * Csak @Transactional metóduson belülről hívható.
     *
     * @param id
     * @return
     */
    public Wallet getWalletByIdForUpdate(long id) {
        return walletRepo.getWalletByIdForUpdate(id, userUtil.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("no wallet found"));
    }

    /**
     * Soft delete-eli a walletet
     *
     * @param id
     */
    public void softDeleteWallet(long id) {
        // TODO itt miért nem a this.getWalletById van használva?
        Wallet walletFromDb = walletRepo.getWalletById(id, userUtil.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("no wallet found"));

        walletFromDb.setStatus(GeneralStatusEnum.DISABLED);

        walletRepo.save(walletFromDb);
    }

    /**
     * Adott wallethez lekérdezi a rajta lévő balance-ot
     * 
     * @param walletId
     * @return
     */
    public BigDecimal getBalanceForWallet(long walletId) {
        var wallet = this.getWalletById(walletId);

        return walletRepo.getBalanceOfWallet(wallet.getId(), userUtil.getUser().getId());
    }

    /**
     * Kiszámolja a tranzakciók alapján, hogy mennyi a jelenlegi pénze a usernek
     */
    public List<WalletSummaryDto> sumAllMoney() {
        return this.walletRepo.summarizeTotalMoneyForUser(userUtil.getUser().getId());
    }

}
