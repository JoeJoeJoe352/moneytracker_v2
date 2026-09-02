package com.starbuck.moneytracker.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.dto.WalletResponseDto;
import com.starbuck.moneytracker.entity.Wallet;

@Component
public class WalletMapper {

    public WalletResponseDto toDto(Wallet wallet) {
        return new WalletResponseDto(wallet.getId(), wallet.getName(), wallet.getCurrencyCode(), wallet.getType());
    }

    public List<WalletResponseDto> toDtoList(List<Wallet> wallets) {
        return wallets.stream()
                .map((wallet) -> this.toDto(wallet))
                .collect(Collectors.toList());
    }

}
