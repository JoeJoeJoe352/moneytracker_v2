package com.starbuck.moneytracker.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.starbuck.moneytracker.commands.TransferTransactionCreateCommand;
import com.starbuck.moneytracker.commands.TransferTransactionUpdateCommand;
import com.starbuck.moneytracker.dto.TransferTransactionCreateDto;
import com.starbuck.moneytracker.dto.TransferTransactionResponseDto;
import com.starbuck.moneytracker.dto.TransferTransactionUpdateDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransferTransactionSide;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.CurrencyEnum;
import com.starbuck.moneytracker.entity.enums.GeneralStatusEnum;
import com.starbuck.moneytracker.entity.enums.TransactionSpecialTypeEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.enums.WalletTypeEnum;
import com.starbuck.moneytracker.mapper.TransferTransactionMapper;

class TransferTransactionMapperTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 16);
    private static final BigDecimal PRICE = new BigDecimal("100.00");

    private final TransferTransactionMapper mapper = new TransferTransactionMapper();

    @Test
    void fromCreateRequest_mapsAllFields() {
        var request = new TransferTransactionCreateDto("Kimenő", 1L, "Bejövő", 2L, DATE, PRICE);

        TransferTransactionCreateCommand command = mapper.fromCreateRequest(request);

        assertEquals("Kimenő", command.getSrcTransactionName());
        assertEquals(1L, command.getSrcWalletId());
        assertEquals("Bejövő", command.getDstTransactionName());
        assertEquals(2L, command.getDstWalletId());
        assertEquals(DATE, command.getTransactionDate());
        assertEquals(PRICE, command.getPriceSum());
    }

    /**
     * A command validációja a mapperen keresztül is érvényesül
     */
    @Test
    void fromCreateRequest_throwsForSameWallets() {
        var request = new TransferTransactionCreateDto("Kimenő", 1L, "Bejövő", 1L, DATE, PRICE);

        assertThrowsExactly(IllegalArgumentException.class, () -> mapper.fromCreateRequest(request));
    }

    @Test
    void fromUpdateRequest_mapsAllFields() {
        var request = new TransferTransactionUpdateDto("Kimenő", "Bejövő", DATE, PRICE);

        TransferTransactionUpdateCommand command = mapper.fromUpdateRequest(request);

        assertEquals("Kimenő", command.getSrcTransactionName());
        assertEquals("Bejövő", command.getDstTransactionName());
        assertEquals(DATE, command.getTransactionDate());
        assertEquals(PRICE, command.getPriceSum());
    }

    /**
     * A válaszban az összeg a cél oldal (pozitív) összege
     */
    @Test
    void toResponseDto_mapsBothSidesWithPositivePrice() {
        User user = new User(1L, "name", "password", "email");
        Wallet srcWallet = new Wallet(1L, "src", user, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT);
        Wallet dstWallet = new Wallet(2L, "dst", user, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT);
        Transaction src = new Transaction(
                10L,
                "Kimenő",
                DATE,
                TransactionTypeEnum.OUTCOME,
                TransactionSpecialTypeEnum.TRANSFER,
                PRICE.negate(),
                GeneralStatusEnum.ACTIVE,
                srcWallet);
        Transaction dst = new Transaction(
                11L,
                "Bejövő",
                DATE,
                TransactionTypeEnum.INCOME,
                TransactionSpecialTypeEnum.TRANSFER,
                PRICE,
                GeneralStatusEnum.ACTIVE,
                dstWallet);
        TransferTransactionSide side = new TransferTransactionSide(src, dst);
        side.setId(7L);

        TransferTransactionResponseDto dto = mapper.toResponseDto(side);

        assertEquals(new TransferTransactionResponseDto(7L, "Kimenő", 1L, "Bejövő", 2L, DATE, PRICE), dto);
    }
}
