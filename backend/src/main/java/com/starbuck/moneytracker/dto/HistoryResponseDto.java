package com.starbuck.moneytracker.dto;

import java.util.List;

public record HistoryResponseDto(List<TransactionResponseDto> transactions, PageDataResponseDto pageData) {}
