package com.starbuck.moneytracker.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import com.starbuck.moneytracker.commands.CreateWalletCommand;
import com.starbuck.moneytracker.commands.UpdateWalletCommand;
import com.starbuck.moneytracker.dto.WalletListResponseDto;
import com.starbuck.moneytracker.dto.WalletListRowResponseDto;
import com.starbuck.moneytracker.dto.WalletSummaryResponseDto;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.CurrencyEnum;
import com.starbuck.moneytracker.entity.enums.GeneralStatusEnum;
import com.starbuck.moneytracker.entity.enums.WalletTypeEnum;
import com.starbuck.moneytracker.exception.EntityNotFoundException;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.util.CurrentUserUtil;
import com.starbuck.moneytracker.util.UserDateUtil;

@Service
public class WalletService {

    private final WalletRepository walletRepo;
    private final MessageSource messageSource;
    private final CurrentUserUtil userUtil;
    private final UserDateUtil userDateUtil;

    public WalletService(
            WalletRepository walletRepo,
            MessageSource messageSource,
            CurrentUserUtil userUtil,
            UserDateUtil userDateUtil) {
        this.walletRepo = walletRepo;
        this.messageSource = messageSource;
        this.userUtil = userUtil;
        this.userDateUtil = userDateUtil;
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
        String defaultNameLocalised =
                messageSource.getMessage("defaultWalletName", null, LocaleContextHolder.getLocale());

        CreateWalletCommand command =
                new CreateWalletCommand(defaultNameLocalised, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT, registeredUser);
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
        Wallet walletFromDb = walletRepo
                .getWalletById(id, userUtil.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));

        walletFromDb.setName(command.getName());
        walletFromDb.setType(command.getType());

        walletRepo.save(walletFromDb);
    }

    /**
     * Visszatér a user tárcáival
     */
    public List<WalletListResponseDto> listWalletsForUser() {
        var wallets = walletRepo.listWalletsWithSumByUserId(userUtil.getUser().getId());
        boolean deletable = isWalletsDeletable(wallets.size());
        return wallets.stream()
                .map(w -> new WalletListResponseDto(
                        w.id(), w.name(), w.currencyCode(), w.type(), w.sum(), getLastSyncDate(w), deletable))
                .toList();
    }

    /**
     * Az utolsó szinkronizálás napja a user időzónája szerint, ha még nem volt, akkor a létrehozásé
     *
     * @param wallet
     * @return LocalDate
     */
    private LocalDate getLastSyncDate(WalletListRowResponseDto wallet) {
        // a @CreationTimestamp a JVM időzónájában menti a LocalDateTime-ot
        Instant lastSyncedAt = wallet.lastSyncedAt() != null
                ? wallet.lastSyncedAt()
                : wallet.createdAt().atZone(ZoneId.systemDefault()).toInstant();
        return userDateUtil.toUserDate(lastSyncedAt);
    }

    /**
     * Törölhetőek-e a walletek
     *
     * @param activeWalletCount
     * @return
     */
    private boolean isWalletsDeletable(int activeWalletCount) {
        return activeWalletCount > 1;
    }

    /**
     * Visszaadja a user egy walletjét id alapján
     *
     * @param id
     * @return
     */
    public Wallet getWalletById(long id) {
        return walletRepo
                .getWalletById(id, userUtil.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
    }

    /**
     * Visszaadja a user egy walletjét id alapján, és a tranzakció végéig zárolja.
     * Csak @Transactional metóduson belülről hívható.
     *
     * @param id
     * @return
     */
    public Wallet getWalletByIdForUpdate(long id) {
        return walletRepo
                .getWalletByIdForUpdate(id, userUtil.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
    }

    /**
     * Soft delete-eli a walletet
     *
     * @param id
     */
    public void softDeleteWallet(long id) {
        Wallet walletFromDb = this.getWalletById(id);

        List<Wallet> wallets = walletRepo.findByUserId(userUtil.getUser().getId());
        if (!isWalletsDeletable(wallets.size())) {
            throw new IllegalStateException("Last wallet cannot be deleted");
        }

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
    public List<WalletSummaryResponseDto> sumAllMoney() {
        return this.walletRepo.summarizeTotalMoneyForUser(userUtil.getUser().getId());
    }
}
