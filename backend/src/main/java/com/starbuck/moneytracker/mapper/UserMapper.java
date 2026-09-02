package com.starbuck.moneytracker.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.dto.UserDataResponseDto;
import com.starbuck.moneytracker.dto.WalletResponseDto;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;

@Component
public class UserMapper {

    private final WalletMapper walletMapper;

    public UserMapper(WalletMapper walletMapper) {
        this.walletMapper = walletMapper;
    }

    /**
     * Átalakítja a usert dto-vá. Walletek azért jönnek paraméterből, hogy a hívó
     * felelőssége legyen olyan query-t lekérni, ahol biztos létezik
     * 
     * @param user
     * @param wallets
     * @return
     */
    public UserDataResponseDto toDto(User user, List<Wallet> wallets) {
        List<WalletResponseDto> walletDtos = walletMapper.toDtoList(wallets);

        return new UserDataResponseDto(user.getUsername(), walletDtos);
    }
}
