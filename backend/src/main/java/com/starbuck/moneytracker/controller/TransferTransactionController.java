package com.starbuck.moneytracker.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.starbuck.moneytracker.dto.TransferTransactionCreateDto;
import com.starbuck.moneytracker.mapper.TransferTransactionMapper;
import com.starbuck.moneytracker.service.TransferTransactionService;

import jakarta.validation.Valid;

@RequestMapping("/transfer-transaction")
@RestController
public class TransferTransactionController {
    private final TransferTransactionService service;
    private final TransferTransactionMapper mapper;

    public TransferTransactionController(TransferTransactionService service, TransferTransactionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping()
    public void createTransferTransaction(@Valid @RequestBody TransferTransactionCreateDto dto) {
        this.service.createLinkedTransaction(mapper.fromCreateRequest(dto));
    }
}
