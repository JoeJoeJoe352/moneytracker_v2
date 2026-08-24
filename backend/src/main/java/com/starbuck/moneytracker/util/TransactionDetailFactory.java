package com.starbuck.moneytracker.util;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.mapper.TransactionMapper;

@Component
public class TransactionDetailFactory {

    @Autowired
    TransactionMapper mapper;

    /**
     * Létrehoz egy alapértelmezett detailt
     * 
     * @param price
     * @return
     */
    public TransactionDetailSaveCommand createDefaultDetailSaveCommand(BigDecimal price, List<Long> categories,
            TransactionTypeEnum type) {
        return new TransactionDetailSaveCommand(TransactionDetail.DEFAULT_DETAIL_NAME, price, categories, type);
    }
}
