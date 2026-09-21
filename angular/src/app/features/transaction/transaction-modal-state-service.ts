import { computed, inject, Injectable, resource, ResourceRef, signal } from '@angular/core';
import { firstValueFrom, Observable, Subject, tap } from 'rxjs';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { TransactionService } from './transaction-service';
import { TransactionActionService } from './transaction-action-service';
import { CategoryService } from './category-service';
import {
    CategoryResponseInterface,
    NewTransaction,
    TransactionDataFromBackend,
} from './interfaces';
import { TransactionModalComponent, TransactionModalInputInterface } from './transaction-modal';
import { NotificationService } from '../../shared/services/notification-service';

interface ModalParams {
    isOpen: boolean;
    editId: number | null;
}

/**
 * A tranzakció létrehozó/szerkesztő modal állapotát és műveleteit fogja össze
 * (megnyitás/bezárás, tranzakció + kategórialista betöltése, mentés/törlés/kategória hozzáadása).
 *
 * Komponens szinten kell providerelni (providers: [TransactionModalStateService]), mert van state-je.
 */
@Injectable()
export class TransactionModalStateService {
    private transactionService = inject(TransactionService);
    private transactionActionService = inject(TransactionActionService);
    private categoryService = inject(CategoryService);
    private dialog = inject(MatDialog);
    private notification = inject(NotificationService);

    /**
     * Nyitva van-e a modal
     */
    private isOpen = signal(false);
    /**
     * Szerkesztendő tranzakció azonosítója. Ha null, akkor új tranzakció felvétele van folyamatban
     */
    private editId = signal<number | null>(null);
    /**
     * A transactionData és a categoriesResource közös reaktív paramétere.
     */
    private modalParams = computed<ModalParams>(() => ({
        isOpen: this.isOpen(),
        editId: this.editId(),
    }));
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
     * Mentés/törlés után emittál, hogy a hívó oldal újratölthesse a saját listáját
     */
    public changed = new Subject<void>();

    /**
     * Tranzakciós modal függőségi adatai töltés alatt vannak-e?
     */
    private isModalDataInitializing = computed(() => {
        return !this.areAllModalDependenciesLoaded();
    });

    /**
     * Kiválasztott tranzakció adatai
     */
    public transactionData: ResourceRef<TransactionDataFromBackend | null> = resource({
        defaultValue: null,
        params: this.modalParams,
        loader: async ({ params }) => {
            if (params.editId === null) {
                return null;
            }
            return firstValueFrom(this.transactionService.getTransactionById(params.editId));
        },
    });

    /**
     * Kategóriák listája. Ugyanarra a paraméterre (modalParams) épül, mint a transactionData,
     * így minden alkalommal újratöltődik, amikor a modal megnyílik
     */
    private categoriesResource: ResourceRef<CategoryResponseInterface[]> = resource({
        defaultValue: [],
        params: this.modalParams,
        loader: async ({ params }) => {
            if (!params.isOpen) {
                return this.categoriesResource.value();
            }
            try {
                return await firstValueFrom(this.categoryService.listCategories());
            } catch (err) {
                console.error('Problem with loading the categories', err);
                this.notification.showGeneralError();
                return [];
            }
        },
    });

    /**
     * Kategóriák listája
     */
    public categories = this.categoriesResource.value;

    /**
     * Tranzakció akkor van betöltött állapotban, ha nincs kiválasztva egy sem, vagy ki van választva és be is vannak töltve az adatai
     */
    private isTransactionDataLoaded = computed(
        () => this.editId() === null || this.transactionData.value() !== null,
    );

    /**
     * Be van-e töltve a modal minden függősége?
     */
    private areAllModalDependenciesLoaded = computed(
        () => this.isTransactionDataLoaded() && !this.categoriesResource.isLoading(),
    );

    /**
     * Műveletek, amik minden olyan művelet után le kell futtatni, ami tranzakciólista módosulásával járhat (Pl.: új tranzakció felvétele, módosítása, törlése)
     */
    private afterChange(): void {
        this.dialogRef?.close();
        this.changed.next();
    }

    /**
     * Tranzakció létrehozó/szerkesztő modal felnyitása
     *
     * @param {number | null} id. Ha null, akkor új tranzakció nyilik fel, ha szám, akkor adott id-jű tranzakció
     */
    public open(id: number | null = null): void {
        this.editId.set(id);
        this.isOpen.set(true);

        const dialogRef = this.dialog.open(TransactionModalComponent, {
            restoreFocus: true,
            width: '600px',
            data: {
                transaction: this.transactionData.value,
                isDataInitializing: this.isModalDataInitializing,
                categories: this.categories,
                isTransactionFormDisabled: this.isTransactionFormDisabled,
                isCategorySaveInProgress: this.isAddingCategoryInProgress,
                addCategoryCallback: this.saveCategory,
            } satisfies TransactionModalInputInterface,
        });
        this.dialogRef = dialogRef;

        dialogRef.componentInstance.deleteTransactionRequested.subscribe((transactionId) =>
            this.confirmDeletion(transactionId),
        );

        dialogRef.componentInstance.saved.subscribe((payload) => this.save(payload));

        dialogRef.afterClosed().subscribe(() => {
            this.isOpen.set(false);
            this.editId.set(null);
            this.dialogRef = null;
        });
    }

    /**
     * Feldob egy confirmot, hogy biztosan törölni szeretné-e a user a tranzakciót, ha igent nyom, törli
     */
    private confirmDeletion(transactionId: number): void {
        this.transactionActionService.confirmDeletion().subscribe((confirmed) => {
            if (confirmed) {
                this.transactionActionService.deleteTransaction(
                    transactionId,
                    this.isTransactionFormDisabled,
                    () => this.afterChange(),
                );
            }
        });
    }

    /**
     * Elmenti a tranzakció adatait
     */
    private save(payload: NewTransaction): void {
        const transactionId = this.transactionData.value()?.id ?? null;
        this.transactionActionService.saveTransaction(
            payload,
            transactionId,
            this.isTransactionFormDisabled,
            () => this.afterChange(),
        );
    }

    /**
     * Hozzáad egy új kategóriát és beszúrja a kategórialistába
     * Observable-ből lekérhető az új kategória adatai
     */
    public saveCategory = (categoryName: string): Observable<CategoryResponseInterface> => {
        this.isAddingCategoryInProgress.set(true);

        return this.categoryService.saveCategory({ name: categoryName }).pipe(
            tap({
                next: (category) => {
                    this.categoriesResource.update((categories) => [...categories, category]);
                    this.isAddingCategoryInProgress.set(false);
                },
                error: (err) => {
                    console.error('Problem with the category save' + err);
                    this.notification.showGeneralError();
                    this.isAddingCategoryInProgress.set(false);
                },
            }),
        );
    };
}
