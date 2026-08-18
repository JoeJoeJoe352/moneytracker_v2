package com.starbuck.moneytracker.unit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.dto.TransactionDetailCreateDto;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.mapper.TransactionMapper;
import com.starbuck.moneytracker.util.TransactionDetailFactory;

@ExtendWith(MockitoExtension.class)
class TransactionDetailFactoryTest {

    @Mock
    private TransactionMapper mapper;

    @InjectMocks
    private TransactionDetailFactory factory;

    /**
     * Ha a kérésben explicit detail lista van, azt alakítja át és adja vissza,
     * nem a globalPrice-ból generál
     */
    @Test
    void factoreDetail_returnsMappedListWhenExplicitDetailsProvided() {
        // GIVEN
        TransactionDetailCreateDto detailRequest = new TransactionDetailCreateDto(new BigDecimal("100.00"),
                "explicitDetail", null, null, List.of());
        TransactionCreateRequest request = new TransactionCreateRequest("test", new BigDecimal("999.00"),
                TransactionTypeEnum.INCOME, LocalDate.now(), List.of(detailRequest), List.of());

        TransactionDetail mappedDetail = new TransactionDetail("explicitDetail", new BigDecimal("100.00"));
        Mockito.when(mapper.fromDetailCreateRequestList(request.transactionDetails()))
                .thenReturn(List.of(mappedDetail));

        // WHEN
        List<TransactionDetail> result = factory.factoreDetail(request);

        // THEN
        assertEquals(List.of(mappedDetail), result);
        Mockito.verify(mapper).fromDetailCreateRequestList(request.transactionDetails());
    }

    /**
     * Ha nincs explicit detail lista, de van globalPrice, abból épít fel egy
     * alapértelmezett nevű detail create dto-t, a globalCategories-al
     */
    @Test
    void factoreDetail_buildsDefaultDetailFromGlobalPriceWhenListIsEmpty() {
        // GIVEN
        List<Long> globalCategories = List.of(1L, 2L);
        TransactionCreateRequest request = new TransactionCreateRequest("test", new BigDecimal("250.00"),
                TransactionTypeEnum.OUTCOME, LocalDate.now(), List.of(), globalCategories);

        TransactionDetail mappedDetail = new TransactionDetail(TransactionDetail.DEFAULT_DETAIL_NAME,
                new BigDecimal("250.00"));
        Mockito.when(mapper.fromDetailCreateRequestList(any())).thenReturn(List.of(mappedDetail));

        // WHEN
        List<TransactionDetail> result = factory.factoreDetail(request);

        // THEN
        assertEquals(List.of(mappedDetail), result);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TransactionDetailCreateDto>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mapper).fromDetailCreateRequestList(captor.capture());

        List<TransactionDetailCreateDto> generatedDetails = captor.getValue();
        assertEquals(1, generatedDetails.size());
        TransactionDetailCreateDto generatedDetail = generatedDetails.get(0);
        assertEquals(new BigDecimal("250.00"), generatedDetail.price());
        assertEquals(TransactionDetail.DEFAULT_DETAIL_NAME, generatedDetail.name());
        assertEquals(globalCategories, generatedDetail.categories());
        assertEquals(null, generatedDetail.weight());
        assertEquals(null, generatedDetail.unitPrice());
    }

    /**
     * Ha se detail lista, se globalPrice nincs megadva, hibát dob és nem hívja
     * meg a mappert
     */
    @Test
    void factoreDetail_throwsWhenNoDetailsAndNoGlobalPrice() {
        TransactionCreateRequest request = new TransactionCreateRequest("test", null,
                TransactionTypeEnum.INCOME, LocalDate.now(), List.of(), List.of());

        assertThrows(IllegalArgumentException.class, () -> {
            factory.factoreDetail(request);
        });

        Mockito.verify(mapper, Mockito.never()).fromDetailCreateRequestList(any());
    }
}
