package com.starbuck.moneytracker.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.starbuck.moneytracker.dto.TransferTransactionCreateDto;
import com.starbuck.moneytracker.service.TransferTransactionService;

import jakarta.validation.Valid;

@RequestMapping("/transfer-transaction")
@RestController
public class TransferTransactionController {
    private final TransferTransactionService service;

    public TransferTransactionController(TransferTransactionService service) {
        this.service = service;
    }

    @PostMapping()
    public void createTransferTransaction(@Valid @RequestBody TransferTransactionCreateDto dto) {

        this.service.createLinkedTransaction();
    }
}
