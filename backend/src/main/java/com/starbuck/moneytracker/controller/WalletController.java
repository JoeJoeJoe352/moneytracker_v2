package com.starbuck.moneytracker.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.starbuck.moneytracker.commands.CreateWalletCommand;
import com.starbuck.moneytracker.commands.BalanceSyncCommand;
import com.starbuck.moneytracker.commands.UpdateWalletCommand;
import com.starbuck.moneytracker.dto.BalanceSyncRequestDto;
import com.starbuck.moneytracker.dto.WalletCreateRequestDto;
import com.starbuck.moneytracker.dto.WalletListResponseDto;
import com.starbuck.moneytracker.dto.WalletUpdateRequestDto;
import com.starbuck.moneytracker.service.BalanceSyncService;
import com.starbuck.moneytracker.service.WalletService;
import com.starbuck.moneytracker.util.CurrentUserUtil;

import jakarta.validation.Valid;

@RequestMapping("/wallet")
@RestController
public class WalletController {

    private final WalletService walletService;
    private final CurrentUserUtil userUtil;
    private final BalanceSyncService syncService;

    public WalletController(WalletService walletService, CurrentUserUtil userUtil, BalanceSyncService syncService) {
        this.walletService = walletService;
        this.userUtil = userUtil;
        this.syncService = syncService;
    }

    /**
     * Visszatér a felhasználó walletjaival
     * 
     * @return
     */
    @GetMapping
    public List<WalletListResponseDto> listWallets() {
        return walletService.listWalletsForUser();
    }

    /**
     * Elment egy walletet
     * 
     * @param dto
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createWallet(@Valid @RequestBody WalletCreateRequestDto dto) {
        var command = new CreateWalletCommand(dto.name(), dto.currencyCode(), dto.walletType(), userUtil.getUser());
        this.walletService.createWallet(command);
    }

    /**
     * Szinkronizálja a wallet egyenlegét a user által megadott valós
     * egyenleggel, az eltérést korrekciós tranzakcióként rögzíti
     *
     * @param dto
     * @param id
     */
    @PostMapping(path = "/{id}/sync")
    @ResponseStatus(HttpStatus.CREATED)
    public void syncWallet(@Valid @RequestBody BalanceSyncRequestDto dto, @PathVariable long id) {
        var command = new BalanceSyncCommand(id, dto.currentBalance());
        syncService.syncWallet(command);
    }

    /**
     * Update-el egy walletet
     *
     * @param dto
     * @param id
     */
    @PutMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void updateWallet(@Valid @RequestBody WalletUpdateRequestDto dto, @PathVariable long id) {
        var command = new UpdateWalletCommand(dto.name(), dto.walletType());
        this.walletService.updateWallet(id, command);
    }

    /**
     * Soft delete-eli a walletet
     *
     * @param id
     */
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDeleteWallet(@PathVariable long id) {
        walletService.softDeleteWallet(id);
    }

}
