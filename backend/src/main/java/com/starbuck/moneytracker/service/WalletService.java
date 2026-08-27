package com.starbuck.moneytracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import com.starbuck.moneytracker.commands.CreateWalletCommand;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;
import com.starbuck.moneytracker.repository.WalletRepository;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepo;

    @Autowired
    private MessageSource messageSource;

    /**
     * Létrehoz egy walletet a megadott adatokkal
     * 
     * @param command
     */
    public void createWallet(CreateWalletCommand command) {
        Wallet wallet = new Wallet(command.getName(), command.getUser(), command.getCurrency(), command.getType());
        walletRepo.save(wallet);
    }

    /**
     * Létrehoz egy alapértelmezett walletet az új felhasználónak.
     * 
     * @param registeredUser -> regisztráció most történt meg, még nincs
     *                       currentUser-ben senki
     */
    public void createDefaultWallet(User registeredUser) {
        String defaultNameLocalised = messageSource.getMessage("defaultWalletName", null,
                LocaleContextHolder.getLocale());

        CreateWalletCommand command = new CreateWalletCommand(defaultNameLocalised, CurrencyEnum.HUF,
                WalletTypeEnum.DEFAULT, registeredUser);
        createWallet(command);
    }

}
