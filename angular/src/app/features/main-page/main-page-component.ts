import { Component, computed, effect, inject, signal } from '@angular/core';
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
     * Újra kell-e tölteni az adatokat? Ha ez változik, akkor újra fogja tölteni a listát.
     * Azért számot növelünk és nem boolean értéket, mert ha gyorsan hívódik egymás után,
     * akkor többször true-ra állítódik az érték és az nem vált ki új letöltés eventet
     */
    private reloadTransactionListTrigger = signal(0);
    /**
     * Kategórialistát újra kell-e tölteni. Minden modal megnyitáskor + új kategória mentéskor
     */
    private reloadCategoryDataTrigger = signal(0);
    /**
     * Kiválasztott tranzakció azonosítója.
     * Ha ez változik, akkor le fog futni a tranzakció betöltés is (transactionData)
     */
    private selectedTransactionIdTrigger = signal<number | null>(null);

    /**
     * Töltődik-e jelenleg a tranzakciós lista
     */
    protected isTransactionListLoading = signal(true);
    /**
     * Összes pénz töltődik-e
     */
    protected isMoneySumLoading = signal(true);
    /**
     * Tranzakciós form írható-e
     */
    protected isTransactionFormDisabled = signal(false);
    /**
     * Tranzakció létrehozó modal bezárása
     */
    protected isTransactionModalOpen = signal(false);

    /**
     * A formban kategória hozzáadása folyamatban van-e?
     */
    protected isAddingCategoryInProgress = signal(false);
    /**
     * Kategória lista betöltődött-e már?
     */
    protected isCategoriesLoaded = signal(false);
    /**
     * Tranzakciós modal függőségi adatai be vannak-e már töltve?
     */
    protected isModalDataInitializing = signal(false);
    /**
     * Volt-e modal felnyitására kérés?
     */
    protected isModalOpenRequested = signal(false);

    constructor() {
        effect(() => {
            // Ha most nyitjuk fel a modalt, akkor lehet még nincs betöltve minden függősége, akkor a modal komponens fog egy loading ikont kirakni
            // azért itt nyitom fel a modalt, mert lehetőség van beállítani a modal töltöttségi állapotát
            if (this.isModalOpenRequested() && !this.isTransactionModalOpen()) {
                this.isTransactionModalOpen.set(true);
                if (!this.areAllModalDependenciesLoaded()) {
                    this.isModalDataInitializing.set(true);
                } else {
                    this.isModalDataInitializing.set(false); // ez lehet nem kell ide, de jobb a biztonság
                }
            } else {
                // Ha már fel van nyitva a modal, akkor ha betöltődött az adat, akkor levesszük a loadert
                if (this.areAllModalDependenciesLoaded()) {
                    this.isModalDataInitializing.set(false);
                }
            }
        });
    }

    /**
     * Betöltés alatt van-e a tranzakció
     */
    protected isTransactionDataLoaded = computed(
        () => this.selectedTransactionIdTrigger() !== null && this.transactionData() !== null,
    );

    /**
     * Modal megnyitásához szükséges dolgok le vannak-e töltve (kategória lista + tranzakciós adatok)
     */
    protected areAllModalDependenciesLoaded = computed(
        () => this.isTransactionDataLoaded() && this.isCategoriesLoaded(),
    );

    /**
     * Tranzakciós lista adatok
     */
    protected transactionListData = toSignal(
        toObservable(this.reloadTransactionListTrigger).pipe(
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
        toObservable(this.reloadTransactionListTrigger).pipe(
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
     * Kategóriák listája. Oldal betöltődésekor beállítódik és Újratöltődik, ha mentünk egy kategóriát
     */
    protected categories = toSignal(
        toObservable(this.reloadCategoryDataTrigger)
            .pipe(
                tap(() => this.isCategoriesLoaded.set(false)),
                switchMap(() => this.categoryService.listCategories()),
            )
            .pipe(tap(() => this.isCategoriesLoaded.set(true))),
        { initialValue: [] },
    );

    /**
     * Tranzakció létrehozó modal felnyitása
     */
    protected openTransactionModal(id: number | null): void {
        this.selectedTransactionIdTrigger.set(id);
        this.isModalOpenRequested.set(true);
        // modal felnyitása a constructor effect-ben van, ha minden api hívás lefutott
    }

    /**
     * Tranzakció létrehozó modal becsukása
     */
    protected closeTransactionModal(): void {
        this.selectedTransactionIdTrigger.set(null);
        this.isTransactionModalOpen.set(false);
        this.isModalOpenRequested.set(false);
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
        this.reloadTransactionListTrigger.update((value) => value + 1);
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
