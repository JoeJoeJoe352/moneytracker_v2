import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Observable, of, Subject, switchMap, tap } from 'rxjs';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { TransactionService } from './transaction-service';
import { TransactionActionService } from './transaction-action-service';
import { CategoryService } from './category-service';
import { CategoryResponseInterface, NewTransaction } from './interfaces';
import { TransactionModalComponent, TransactionModalInputInterface } from './transaction-modal';
import { MatSnackBar } from '@angular/material/snack-bar';
import { _, TranslateService } from '@ngx-translate/core';

/**
 * A tranzakció létrehozó/szerkesztő modal állapotát és műveleteit fogja össze
 * (megnyitás/bezárás, tranzakció + kategórialista betöltése, mentés/törlés/kategória hozzáadása).
 *
 * Komponens szinten kell providerelni (providers: [TransactionModalStateService]), hogy minden
 * oldal saját, egymástól független state-tel rendelkezzen.
 */
@Injectable()
export class TransactionModalStateService {
    private transactionService = inject(TransactionService);
    private transactionActionService = inject(TransactionActionService);
    private categoryService = inject(CategoryService);
    private dialog = inject(MatDialog);
    private snackBar = inject(MatSnackBar);
    private translateService = inject(TranslateService);

    /**
     * Kiválasztott tranzakció azonosítója.
     * Ha ez változik, akkor le fog futni a tranzakció betöltés is (transactionData)
     */
    private selectedTransactionIdTrigger = signal<number | null>(null);
    /**
     * Kategórialistát újra kell-e tölteni. Minden új kategória mentéskor
     */
    private reloadCategoryDataTrigger = signal(0);
    /**
     * Kategória lista betöltődött-e már?
     */
    private isCategoriesLoaded = signal(false);
    /**
     * Jelenleg nyitva lévő modal referenciája
     */
    private dialogRef: MatDialogRef<TransactionModalComponent> | null = null;

    /**
     * Tranzakciós form írható-e
     */
    public isTransactionFormDisabled = signal(false);
    /**
     * A formban kategória hozzáadása folyamatban van-e?
     */
    public isAddingCategoryInProgress = signal(false);
    /**
     * Tranzakciós modal függőségi adatai be vannak-e már töltve?
     */
    public isModalDataInitializing = signal(false);
    /**
     * Mentés/törlés után emittál, hogy a hívó oldal újratölthesse a saját listáját
     */
    public changed = new Subject<void>();

    constructor() {
        effect(() => {
            this.isModalDataInitializing.set(!this.areAllModalDependenciesLoaded());
        });
    }

    /**
     * Kiválasztott tranzakció adatai
     */
    public transactionData = toSignal(
        toObservable(this.selectedTransactionIdTrigger).pipe(
            switchMap((id) =>
                id === null ? of(null) : this.transactionService.getTransactionById(id),
            ),
        ),
        { initialValue: null },
    );

    /**
     * Kategóriák listája. Modal megnyitásakor és új kategória mentésekor töltődik újra
     */
    public categories = toSignal(
        toObservable(this.reloadCategoryDataTrigger).pipe(
            tap(() => this.isCategoriesLoaded.set(false)),
            switchMap(() => this.categoryService.listCategories()),
            tap(() => this.isCategoriesLoaded.set(true)),
        ),
        { initialValue: [] },
    );

    /**
     * Tranzakció akkor van betöltött állapotban, ha nincs kiválasztva egy sem, vagy ki van választva és be is vannak töltve az adatai
     */
    private isTransactionDataLoaded = computed(
        () =>
            this.selectedTransactionIdTrigger() === null ||
            (this.selectedTransactionIdTrigger() !== null && this.transactionData() !== null),
    );

    /**
     * Be van-e töltve a modal minden függősége?
     */
    private areAllModalDependenciesLoaded = computed(
        () => this.isTransactionDataLoaded() && this.isCategoriesLoaded(),
    );

    /**
     * Műveletek, amik minden olyan művelet után le kell futtatni, ami tranzakciólista módosulásával járhat (Pl.: új tranzakció felvétele, módosítása, törlése)
     */
    private afterChange(): void {
        this.close();
        this.changed.next();
    }

    /**
     * Tranzakció létrehozó/szerkesztő modal felnyitása
     *
     * @param {number | null} id. Ha null, akkor új tranzakció nyilik fel, ha szám, akkor adott id-jű tranzakció
     */
    public open(id: number | null): void {
        this.selectedTransactionIdTrigger.set(id);

        const dialogRef = this.dialog.open(TransactionModalComponent, {
            width: '600px',
            data: {
                transaction: this.transactionData,
                isDataInitializing: this.isModalDataInitializing,
                categories: this.categories,
                isTransactionFormDisabled: this.isTransactionFormDisabled,
                isCategorySaveInProgress: this.isAddingCategoryInProgress,
                addCategoryCallback: this.saveCategory,
            } as TransactionModalInputInterface,
        });
        this.dialogRef = dialogRef;

        dialogRef.componentInstance.deleteTransactionRequested.subscribe((transactionId) =>
            this.confirmDeletion(transactionId),
        );
        dialogRef.componentInstance.saved.subscribe((payload) => this.save(payload));
        dialogRef.componentInstance.categoryAdded.subscribe((categoryName) =>
            this.saveCategory(categoryName),
        );

        dialogRef.afterClosed().subscribe(() => {
            this.selectedTransactionIdTrigger.set(null);
            this.dialogRef = null;
        });
    }

    /**
     * Modal becsukása
     */
    private close(): void {
        this.dialogRef?.close();
    }

    /**
     * Feldob egy confirmot, hogy biztosan törölni szeretné-e a user a tranzakciót, ha igent nyom, törli
     */
    public confirmDeletion(transactionId: number): void {
        if (this.transactionActionService.confirmDeletion()) {
            this.transactionActionService.deleteTransaction(
                transactionId,
                this.isTransactionFormDisabled,
                () => this.afterChange(),
            );
        }
    }

    /**
     * Elmenti a tranzakció adatait
     */
    public save(payload: NewTransaction): void {
        const transactionId = this.transactionData()?.id ?? null;
        this.transactionActionService.saveTransaction(
            payload,
            transactionId,
            this.isTransactionFormDisabled,
            () => this.afterChange(),
        );
    }

    /**
     * Hozzáad egy új kategóriát és újratölti a kategórialistát.
     * Observable-ből lekérhető az új kategória adatai
     */
    public saveCategory = (categoryName: string): Observable<CategoryResponseInterface> => {
        this.isAddingCategoryInProgress.set(true);

        return this.categoryService.saveCategory({ name: categoryName }).pipe(
            tap({
                next: () => {
                    this.reloadCategoryDataTrigger.update((value) => value + 1);
                    this.isAddingCategoryInProgress.set(false);
                },
                error: (err) => {
                    console.error('Problem with the category save' + err);
                    this.snackBar.open(
                        this.translateService.instant(_('etc.general-error')),
                        this.translateService.instant(_('etc.close')),
                    );
                    this.isAddingCategoryInProgress.set(false);
                },
            }),
        );
    };
}
