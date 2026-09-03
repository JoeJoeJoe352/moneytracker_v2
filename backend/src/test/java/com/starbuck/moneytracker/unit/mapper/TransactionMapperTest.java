package com.starbuck.moneytracker.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.dto.TransactionDetailCreateDto;
import com.starbuck.moneytracker.dto.TransactionDetailResponseDto;
import com.starbuck.moneytracker.dto.TransactionEditResponseDto;
import com.starbuck.moneytracker.dto.TransactionResponseDto;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionDetailCategory;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;
import com.starbuck.moneytracker.mapper.TransactionMapper;
import com.starbuck.moneytracker.mapper.WalletMapper;

@ExtendWith(MockitoExtension.class)
class TransactionMapperTest {

    @InjectMocks
    private WalletMapper walletMapper;

    private TransactionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TransactionMapper(walletMapper);
    }

    private Wallet defaultWallet;

    public TransactionMapperTest() {
        User user = new User("teszt", "qweqweqwe", "teszt@mail.com");
        this.defaultWallet = new Wallet("Default Wallet", user, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT);
        this.defaultWallet.setId(1L);
    }

    /**
     * Null entitásra null-lal tér vissza
     */
    @Test
    void toDto_returnsNullForNullEntity() {
        assertNull(mapper.toDto(null));
    }

    /**
     * Egy egyszerű, egy detailos tranzakciót helyesen alakít át DTO-vá
     */
    @Test
    void toDto_mapsSimpleTransactionWithSingleDetail() {
        Transaction transaction = new Transaction(1L, "simple", LocalDate.of(2026, 1, 15),
                TransactionTypeEnum.INCOME, new BigDecimal("100.00"), 0, defaultWallet);

        TransactionDetail detail = new TransactionDetail(1L, TransactionDetail.DEFAULT_DETAIL_NAME,
                new BigDecimal("100.00"), null, null, transaction);
        detail.setCategoryLinks(List.of());
        transaction.setTransactionDetails(List.of(detail));

        TransactionResponseDto dto = mapper.toDto(transaction);

        assertEquals(1L, dto.id());
        assertEquals("simple", dto.name());
        assertEquals(new BigDecimal("100.00"), dto.priceSum());
        assertEquals(LocalDate.of(2026, 1, 15), dto.transactionDate());
        assertEquals(TransactionTypeEnum.INCOME, dto.transactionType());
        // egy detail van, alapértelmezett névvel, ezért nem komplex
        assertFalse(dto.isComplexTransaction());
        assertEquals(1, dto.transactionDetails().size());

        TransactionDetailResponseDto detailDto = dto.transactionDetails().iterator().next();
        assertEquals(TransactionDetail.DEFAULT_DETAIL_NAME, detailDto.name());
        assertEquals(new BigDecimal("100.00"), detailDto.price());
        assertFalse(detailDto.isComplexPriceMode());
        assertTrue(detailDto.categories().isEmpty());

        var walletDto = dto.wallet();
        assertEquals("Default Wallet", walletDto.name());
        assertEquals(CurrencyEnum.HUF, walletDto.currencyCode());
        assertEquals(WalletTypeEnum.DEFAULT, walletDto.type());
    }

    /**
     * Súllyal és egységárral rendelkező (komplex árazású) detailt is helyesen
     * alakít át
     */
    @Test
    void toDto_mapsComplexPriceModeDetail() {
        Transaction transaction = new Transaction(1L, "weighted", LocalDate.now(), TransactionTypeEnum.OUTCOME,
                new BigDecimal("-150.00"), 0, defaultWallet);

        TransactionDetail detail = new TransactionDetail(1L, "meat", new BigDecimal("-150.00"),
                new BigDecimal("0.5"), new BigDecimal("300"), transaction);
        detail.setCategoryLinks(List.of());
        transaction.setTransactionDetails(List.of(detail));

        TransactionResponseDto dto = mapper.toDto(transaction);

        TransactionDetailResponseDto detailDto = dto.transactionDetails().iterator().next();
        assertTrue(detailDto.isComplexPriceMode());
        assertEquals(new BigDecimal("0.5"), detailDto.weight());
        assertEquals(new BigDecimal("300"), detailDto.unitPrice());
    }

    /**
     * Több detail esetén a tranzakció komplexnek számít, és a category id-k is
     * átkerülnek a DTO-ba
     */
    @Test
    void toDto_mapsMultipleDetailsAsComplexTransactionWithCategoryIds() {
        Transaction transaction = new Transaction(1L, "multi", LocalDate.now(), TransactionTypeEnum.INCOME,
                new BigDecimal("300.00"), 0, defaultWallet);

        TransactionDetail detail1 = new TransactionDetail(1L, "detail1", new BigDecimal("100.00"), null, null,
                transaction);
        TransactionDetail detail2 = new TransactionDetail(2L, "detail2", new BigDecimal("200.00"), null, null,
                transaction);

        var category = new com.starbuck.moneytracker.entity.Category();
        category.setId(7L);
        TransactionDetailCategory categoryLink = new TransactionDetailCategory(category, detail1);
        detail1.setCategoryLinks(List.of(categoryLink));
        detail2.setCategoryLinks(List.of());

        transaction.setTransactionDetails(List.of(detail1, detail2));

        TransactionEditResponseDto dto = mapper.toEditDto(transaction);

        assertTrue(dto.isComplexTransaction());
        assertEquals(2, dto.transactionDetails().size());

        var detail1Dto = dto.transactionDetails().stream()
                .filter(d -> d.name().equals("detail1")).findFirst().orElseThrow();
        assertEquals(List.of(7L), detail1Dto.categories());
    }

    @Test
    void toDto_mapsMultipleDetailsAsComplexTransactionWithCategoryIds_list() {
        Category category = new Category(7L, "teszt!", null, null);
        Transaction transaction = new Transaction(1L, "multi", LocalDate.now(), TransactionTypeEnum.INCOME,
                new BigDecimal("300.00"), 0, defaultWallet);

        TransactionDetail detail1 = new TransactionDetail(1L, "detail1", new BigDecimal("100.00"), null, null,
                transaction);
        TransactionDetail detail2 = new TransactionDetail(2L, "detail2", new BigDecimal("200.00"), null, null,
                transaction);

        TransactionDetailCategory categoryLink = new TransactionDetailCategory(category, detail1);
        detail1.setCategoryLinks(List.of(categoryLink));
        detail2.setCategoryLinks(List.of());

        transaction.setTransactionDetails(List.of(detail1, detail2));

        TransactionResponseDto dto = mapper.toDto(transaction);

        assertTrue(dto.isComplexTransaction());
        assertEquals(2, dto.transactionDetails().size());

        var detail1Dto = dto.transactionDetails().stream()
                .filter(d -> d.name().equals("detail1")).findFirst().orElseThrow();
        assertEquals(List.of("teszt!"), detail1Dto.categories());
    }

    /**
     * Üres listára üres listával tér vissza
     */
    @Test
    void toDtoList_returnsEmptyListForEmptyInput() {
        assertTrue(mapper.toDtoList(List.of()).isEmpty());
    }

    /**
     * Minden elemet egyenként átalakít
     */
    @Test
    void toDtoList_mapsEachTransaction() {
        Transaction transaction1 = new Transaction(1L, "first", LocalDate.now(), TransactionTypeEnum.INCOME,
                new BigDecimal("10.00"), 0, defaultWallet);
        TransactionDetail detail1 = new TransactionDetail(1L, TransactionDetail.DEFAULT_DETAIL_NAME,
                new BigDecimal("10.00"), null, null, transaction1);
        detail1.setCategoryLinks(List.of());
        transaction1.setTransactionDetails(List.of(detail1));

        Transaction transaction2 = new Transaction(2L, "second", LocalDate.now(), TransactionTypeEnum.OUTCOME,
                new BigDecimal("-20.00"), 0, defaultWallet);
        TransactionDetail detail2 = new TransactionDetail(2L, TransactionDetail.DEFAULT_DETAIL_NAME,
                new BigDecimal("-20.00"), null, null, transaction2);
        detail2.setCategoryLinks(List.of());
        transaction2.setTransactionDetails(List.of(detail2));

        List<TransactionResponseDto> dtos = mapper.toDtoList(List.of(transaction1, transaction2));

        assertEquals(2, dtos.size());
        assertTrue(dtos.stream().anyMatch(d -> d.name().equals("first")));
        assertTrue(dtos.stream().anyMatch(d -> d.name().equals("second")));
    }

    /**
     * A create requestből a nevet, dátumot és típust másolja át, a
     * priceSum/detailok nélkül
     */
    @Test
    void fromTransactionCreateRequest_mapsBasicFields() {
        TransactionCreateRequest request = new TransactionCreateRequest("groceries", new BigDecimal("-50.00"),
                TransactionTypeEnum.OUTCOME, LocalDate.of(2026, 3, 1), List.of(), List.of(), 1L);

        TransactionSaveCommand result = mapper.fromTransactionSaveRequest(request);

        assertEquals("groceries", result.getTransactionName());
        assertEquals(LocalDate.of(2026, 3, 1), result.getTransactionDate());
        assertEquals(TransactionTypeEnum.OUTCOME, result.getTransactionType());
        assertEquals(1, result.getWalletId());
    }

    /**
     * Minden detail create dto-t átalakít a listában
     */
    @Test
    void fromDetailCreateRequest_mapsEachRequest() {
        TransactionDetailCreateDto request1 = new TransactionDetailCreateDto(new BigDecimal("10.00"), "d1",
                null,
                null, List.of());
        TransactionDetailCreateDto request2 = new TransactionDetailCreateDto(new BigDecimal("20.00"), "d2",
                null,
                null, List.of());

        List<TransactionDetailSaveCommand> result = mapper.fromDetailCreateRequest(
                List.of(request1, request2), TransactionTypeEnum.INCOME);

        assertEquals(2, result.size());
        assertEquals("d1", result.get(0).getName());
        assertEquals("d2", result.get(1).getName());
    }

    /**
     * Üres listára üres listával tér vissza
     */
    @Test
    void fromDetailCreateRequest_returnsEmptyListForEmptyInput() {
        assertTrue(mapper.fromDetailCreateRequest(List.of(), TransactionTypeEnum.INCOME).isEmpty());
    }

    @Test
    void fromEntityRequest() {
        var date = LocalDate.now();
        Transaction transaction = new Transaction(1L, "test", date, TransactionTypeEnum.INCOME,
                new BigDecimal("100.00"), 0, defaultWallet);

        TransactionDetail detail1 = new TransactionDetail(1L, "detail1", new BigDecimal("100.00"), null, null,
                transaction);
        TransactionDetail detail2 = new TransactionDetail(1L, "detail2", new BigDecimal("100.00"),
                new BigDecimal("50.00"), new BigDecimal("200.00"), transaction);
        transaction.setTransactionDetails(List.of(detail1, detail2));

        var command = mapper.entityToCommand(transaction);

        assertEquals("test", command.getTransactionName());
        assertNull(command.getGlobalPrice());
        assertEquals(TransactionTypeEnum.INCOME, command.getTransactionType());
        assertEquals(2, command.getDetailCommands().size());
        assertEquals(date, command.getTransactionDate());

        assertEquals("detail1", command.getDetailCommands().get(0).getName());
        assertEquals("detail2", command.getDetailCommands().get(1).getName());
        assertEquals(new BigDecimal("100.00"), command.getDetailCommands().get(0).getPrice());
        assertNull(command.getDetailCommands().get(1).getPrice());
        assertEquals(new BigDecimal("200.00"), command.getDetailCommands().get(1).getUnitPrice());
        assertEquals(new BigDecimal("50.00"), command.getDetailCommands().get(1).getWeight());
    }
}
