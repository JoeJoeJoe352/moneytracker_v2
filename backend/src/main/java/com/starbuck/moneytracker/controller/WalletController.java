package com.starbuck.moneytracker.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.starbuck.moneytracker.commands.CreateWalletCommand;
import com.starbuck.moneytracker.commands.UpdateWalletCommand;
import com.starbuck.moneytracker.dto.WalletCreateDto;
import com.starbuck.moneytracker.dto.WalletResponseDto;
import com.starbuck.moneytracker.dto.WalletUpdateDto;
import com.starbuck.moneytracker.mapper.WalletMapper;
import com.starbuck.moneytracker.service.WalletService;
import com.starbuck.moneytracker.util.CurrentUserUtil;

@RestController
public class WalletController {

    private final WalletService walletService;
    private final WalletMapper walletMapper;
    private final CurrentUserUtil userUtil;

    public WalletController(WalletService walletService, WalletMapper walletMapper, CurrentUserUtil userUtil) {
        this.walletService = walletService;
        this.walletMapper = walletMapper;
        this.userUtil = userUtil;
    }

    /**
     * Visszatér a felhasználó walletjaival
     * 
     * @return
     */
    @GetMapping(path = "/wallet")
    public List<WalletResponseDto> listWallets() {
        var wallets = walletService.listWalletsForUser();

        return walletMapper.toDtoList(wallets);
    }

    /**
     * Elment egy walletet
     * 
     * @param dto
     */
    @PostMapping(path = "wallet")
    @ResponseStatus(HttpStatus.CREATED)
    public void createWallet(@RequestBody WalletCreateDto dto) {
        var command = new CreateWalletCommand(dto.name(), dto.currencyCode(), dto.walletType(), userUtil.getUser());
        this.walletService.createWallet(command);
    }

    /**
     * Update-el egy walletet
     *
     * @param dto
     * @param id
     */
    @PutMapping(path = "wallet/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void updateWallet(@RequestBody WalletUpdateDto dto, @PathVariable long id) {
        var command = new UpdateWalletCommand(dto.name(), dto.walletType());
        this.walletService.updateWallet(id, command);
    }

    /**
     * Visszaadja a user egy walletjét id alapján
     *
     * @param id
     * @return
     */
    @GetMapping(path = "wallet/{id}")
    public WalletResponseDto getWalletById(@PathVariable long id) {
        var wallet = walletService.getWalletById(id);

        return walletMapper.toDto(wallet);
    }

    /**
     * Soft delete-eli a walletet
     *
     * @param id
     */
    @DeleteMapping(path = "wallet/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDeleteWallet(@PathVariable long id) {
        walletService.softDeleteWallet(id);
    }

}
