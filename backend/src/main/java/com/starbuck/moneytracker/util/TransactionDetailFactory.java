package com.starbuck.moneytracker.util;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.entity.TransactionDetail;
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
    public TransactionDetail createDefauldDetail(BigDecimal price) {
        return new TransactionDetail(TransactionDetail.DEFAULT_DETAIL_NAME, price);
    }
}
