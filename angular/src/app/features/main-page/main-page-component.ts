import { Component, computed, inject, signal } from '@angular/core';
import TransactionListComponent from '../transaction/transaction-list-component';
import { TransactionModalComponent } from '../transaction/transaction-modal';
import { NewTransaction } from '../transaction/interfaces';
import { TransactionService } from '../transaction/transaction-service';
import { DecimalPipe } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { of, switchMap, tap } from 'rxjs';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { TransactionActionService } from '../transaction/transaction-action-service';
import { CategoryService } from '../transaction/category-service';

@Component({
    selector: 'app-main-page-component',
    templateUrl: './main-page-component.html',
    styleUrl: './main-page-component.scss',
    standalone: true,
    imports: [TransactionModalComponent, TransactionListComponent, DecimalPipe, TranslatePipe],
})
export class MainPage {
    private transactionService = inject(TransactionService);
    private transactionActionService = inject(TransactionActionService);
    private categoryService = inject(CategoryService);

    /**
     * Töltődik-e jelenleg a tranzakciós lista
     */
    protected isTransactionListLoading = signal(true);
    /**
     * Összes pénz töltődik-e
     */
    protected isMoneySumLoading = signal(true);
    /**
     * Kiválasztott tranzakció azonosítója.
     * Ha ez változik, akkor le fog futni a tranzakció betöltés is (transactionData)
     */
    protected selectedTransactionIdTrigger = signal<number | null>(null);
    /**
     * Tranzakciós form írható-e
     */
    protected isTransactionFormDisabled = signal(false);
    /**
     * Tranzakció létrehozó modal bezárása
     */
    protected isTransactionModalOpen = signal(false);
    /**
     * Újra kell-e tölteni az adatokat
     * Ha ez változik, akkor újra fogja tölteni a listát
     * Azért számot növelünk és nem boolean érték, mert ha gyorsan, többször hívódik egymás után, akkor kétszer true-ra állítódik az érték és az nem vált ki új letöltés eventet
     */
    protected reloadTransactionMoneyDataTrigger = signal(0);
    /**
     * Kategórialistát újra kell-e tölteni
     */
    protected reloadCategoryDataTrigger = signal(0);
    /**
     * A formban kategória hozzáadása folyamatban van-e?
     */
    protected isAddingCategoryInProgress = signal(false); // TODO ezt átadni a modal és többinek

    /**
     *  Betöltés alatt van-e a tranzakció
     */
    protected isTransactionDataLoading = computed(
        () => this.selectedTransactionIdTrigger() !== null && this.transactionData() === null,
    );

    /**
     * Tranzakciós lista adatok
     */
    protected transactionListData = toSignal(
        toObservable(this.reloadTransactionMoneyDataTrigger).pipe(
            tap(() => this.isTransactionListLoading.set(true)),
            switchMap(() =>
                this.transactionService.getLastTransactions().pipe(
                    tap(() => {
                        this.isTransactionListLoading.set(false);
                    }),
                ),
            ),
        ),
        {
            initialValue: [],
        },
    );
    /**
     * Felhasználó összes pénze.
     */
    protected moneySum = toSignal(
        toObservable(this.reloadTransactionMoneyDataTrigger).pipe(
            tap(() => this.isMoneySumLoading.set(true)),
            switchMap(() =>
                this.transactionService.getMoneySum().pipe(
                    tap(() => {
                        this.isMoneySumLoading.set(false);
                    }),
                ),
            ),
        ),
        { initialValue: null },
    );
    /**
     * Kiválasztott tranzakció adatai
     */
    protected transactionData = toSignal(
        toObservable(this.selectedTransactionIdTrigger).pipe(
            switchMap((id) =>
                id === null ? of(null) : this.transactionService.getTransactionById(id),
            ),
        ),
        { initialValue: null },
    );

    /**
     * Kategóriák listája
     */
    protected categories = toSignal(
        toObservable(this.reloadCategoryDataTrigger).pipe(
            switchMap(() => this.categoryService.listCategories()),
        ),
        { initialValue: [] },
    );

    /**
     * Tranzakció létrehozó modal felnyitása
     */
    protected openTransactionModal(id: number | null): void {
        this.selectedTransactionIdTrigger.set(id);
        this.isTransactionModalOpen.set(true);
    }

    /**
     * Tranzakció létrehozó modal becsukása
     */
    protected closeTransactionModal(): void {
        this.selectedTransactionIdTrigger.set(null);
        this.isTransactionModalOpen.set(false);
    }

    /**
     * Feldob egy confirmot, hogy biztosan törölni szeretné-e a user a tranzakciót, ha igent nyom, törli
     */
    protected popupDeletionConfirm(transactionId: number): void {
        if (this.transactionActionService.confirmDeletion()) {
            this.transactionActionService.deleteTransaction(
                transactionId,
                this.isTransactionFormDisabled,
                () => this.refreshAfterSave(),
            );
        }
    }

    /**
     * Elment egy tranzakció adatait
     */
    protected saveTransaction(payload: NewTransaction): void {
        const transactionData = this.transactionData();
        this.transactionActionService.saveTransaction(
            payload,
            transactionData?.id ?? null,
            this.isTransactionFormDisabled,
            () => this.refreshAfterSave(),
        );
    }

    /**
     * Modal adatváltozás lekezelése
     */
    private refreshAfterSave(): void {
        this.reloadTransactionMoneyDataTrigger.update((value) => value + 1);
        this.closeTransactionModal();
    }

    /**
     * Hozzáad egy új kategóriát és újratölti a listát
     */
    saveCategory(categoryName: string): void {
        this.isAddingCategoryInProgress.set(true);

        this.categoryService.saveCategory({ name: categoryName }).subscribe({
            next: () => {
                this.reloadCategoryDataTrigger.update((value) => value + 1);
                this.isAddingCategoryInProgress.set(false);
            },
            error: (err) => {
                console.error('Problem with the category save' + err);
                this.isAddingCategoryInProgress.set(false);
            },
        });
    }
}
