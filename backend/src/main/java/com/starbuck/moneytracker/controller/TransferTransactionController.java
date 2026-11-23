package com.starbuck.moneytracker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.starbuck.moneytracker.dto.TransferTransactionCreateDto;
import com.starbuck.moneytracker.dto.TransferTransactionUpdateDto;
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

    /**
     * Módosítja a user adott id-jú átvezetését (mindkét oldalát)
     */
    @PutMapping(path = "/{transferId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateTransferTransaction(
            @Valid @RequestBody TransferTransactionUpdateDto dto, @PathVariable Long transferId) {
        this.service.updateLinkedTransaction(transferId, mapper.fromUpdateRequest(dto));
    }
}
