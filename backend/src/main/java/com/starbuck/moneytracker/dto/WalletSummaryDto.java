package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;

import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;

public class WalletSummaryDto {
    private CurrencyEnum currencyCode;
    private BigDecimal total;

    public WalletSummaryDto(CurrencyEnum currencyCode, BigDecimal total) {
        this.currencyCode = currencyCode;
        this.total = total;
    }

    public CurrencyEnum getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyEnum currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

}
