package com.starbuck.moneytracker.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionDetailCategory;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailCategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.service.domainservice.CostCalculatorDomainService;
import com.starbuck.moneytracker.util.CurrentUserUtil;
import com.starbuck.moneytracker.util.TransactionDetailFactory;

/**
 * A tranzakciók detailjainak és a detailokhoz tartozó kategória kapcsolatoknak
 * a kezelése. A TransactionService tranzakcióján belül fut.
 */
@Service
public class TransactionDetailService {

    private final TransactionDetailRepository transactionDetailRepo;
    private final CategoryRepository categoryRepo;
    private final TransactionDetailCategoryRepository transactionDetailCategoryRepository;
    private final CurrentUserUtil currentUser;
    private final TransactionDetailFactory detailFactory;

    private final CostCalculatorDomainService costCalculator = new CostCalculatorDomainService();

    public TransactionDetailService(TransactionDetailRepository transactionDetailRepo,
            CategoryRepository categoryRepo, TransactionDetailCategoryRepository transactionDetailCategoryRepository,
            CurrentUserUtil currentUser, TransactionDetailFactory detailFactory) {
        this.transactionDetailRepo = transactionDetailRepo;
        this.categoryRepo = categoryRepo;
        this.transactionDetailCategoryRepository = transactionDetailCategoryRepository;
        this.currentUser = currentUser;
        this.detailFactory = detailFactory;
    }

    /**
     * Feltölti és elmenti a tranzakciós részleteket
     *
     * @param savedTransaction
     * @param createCommand
     */
    public void saveDetails(Transaction savedTransaction, TransactionSaveCommand createCommand) {
        List<TransactionDetailSaveCommand> details = detailFactory.resolveDetailCommands(createCommand,
                savedTransaction);

        for (TransactionDetailSaveCommand detailCommand : details) {
            TransactionDetail detail = new TransactionDetail(
                    detailCommand.getName(),
                    costCalculator.calculateCost(detailCommand, savedTransaction.getTransactionType()),
                    detailCommand.getWeight(),
                    detailCommand.getUnitPrice(),
                    savedTransaction);
            var detailAfterSave = this.transactionDetailRepo.save(detail);

            if (detailCommand.getCategories() != null && !detailCommand.getCategories().isEmpty()) {
                this.saveCategoryDetailEntries(detailAfterSave, detailCommand.getCategories());
            }
        }

    }

    /**
     * Törli a tranzakció detailjait.
     * Cascade delete miatt ez törli a detailCategory táblában lévő kapcsolat
     * bejegyzéseket is TODO ez legyen direktben törlés inkább, ne cascade delete
     *
     * @param transaction
     */
    public void deleteDetails(Transaction transaction) {
        transactionDetailRepo.deleteAll(transaction.getTransactionDetails());
    }

    /**
     * Kapcsolatokat létrehozzuk a detail és a hozzá tartozó kategóriák között
     *
     * @param savedDetail
     * @param categoryIds
     */
    private void saveCategoryDetailEntries(TransactionDetail savedDetail,
            List<Long> categoryIds) {
        var foundCategories = categoryRepo.findAllById(categoryIds, currentUser.getUser().getId());
        boolean isAllCategoryAvailableForUser = foundCategories.size() == categoryIds.size();
        if (!isAllCategoryAvailableForUser) {
            throw new IllegalArgumentException("Input contains a category id that doesn't belong to the user");
        }
        categoryIds.forEach((id) -> {
            Category categoryDummyObject = new Category();
            categoryDummyObject.setId(id);
            TransactionDetailCategory detailCategoryModel = new TransactionDetailCategory(categoryDummyObject,
                    savedDetail);
            transactionDetailCategoryRepository.save(detailCategoryModel);
        });
    }
}
