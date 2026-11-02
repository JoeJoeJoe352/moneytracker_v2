package com.starbuck.moneytracker.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.starbuck.moneytracker.dto.RecurringTransactionResponseDto;
import com.starbuck.moneytracker.dto.RecurringTransactionSaveRequestDto;
import com.starbuck.moneytracker.mapper.RecurringTransactionMapper;
import com.starbuck.moneytracker.service.RecurringTransactionService;

import jakarta.validation.Valid;

@RequestMapping("/recurring-transaction")
@RestController
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;
    private final RecurringTransactionMapper recurringTransactionMapper;

    public RecurringTransactionController(
            RecurringTransactionService recurringTransactionService,
            RecurringTransactionMapper recurringTransactionMapper) {
        this.recurringTransactionService = recurringTransactionService;
        this.recurringTransactionMapper = recurringTransactionMapper;
    }

    /**
     * Létrehoz egy ismétlődő tranzakciót
     *
     * @param request
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createRecurringTransaction(@Valid @RequestBody RecurringTransactionSaveRequestDto request) {
        var command = recurringTransactionMapper.fromSaveRequest(request);
        recurringTransactionService.createRecurringTransaction(command);
    }

    /**
     * Visszatér a user ismétlődő tranzakcióival
     *
     * @return
     */
    @GetMapping
    public List<RecurringTransactionResponseDto> listRecurringTransactions() {
        return recurringTransactionMapper.toDtoList(recurringTransactionService.listRecurringTransactions());
    }

    /**
     * Visszaadja a user megadott id-jú ismétlődő tranzakcióját
     *
     * @param id
     * @return
     */
    @GetMapping(path = "/{id}")
    public RecurringTransactionResponseDto getRecurringTransactionById(@PathVariable long id) {
        return recurringTransactionMapper.toDto(
                recurringTransactionService.getRecurringTransactionByIdForActualUser(id));
    }

    /**
     * Frissíti a user megadott id-jú ismétlődő tranzakcióját
     *
     * @param request
     * @param id
     */
    @PutMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateRecurringTransaction(
            @Valid @RequestBody RecurringTransactionSaveRequestDto request, @PathVariable long id) {
        var command = recurringTransactionMapper.fromSaveRequest(request);
        recurringTransactionService.updateRecurringTransaction(id, command);
    }

    /**
     * Leállítja a user megadott id-jú ismétlődő tranzakcióját
     *
     * @param id
     */
    @PutMapping(path = "/{id}/stop")
    @ResponseStatus(HttpStatus.OK)
    public void stopRecurringTransaction(@PathVariable long id) {
        recurringTransactionService.stopRecurringTransaction(id);
    }

    /**
     * Törli (soft delete) a user megadott id-jú ismétlődő tranzakcióját
     *
     * @param id
     */
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteRecurringTransaction(@PathVariable long id) {
        recurringTransactionService.deleteRecurringTransaction(id);
    }
}
