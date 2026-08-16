import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { of, Subject, switchMap, tap } from 'rxjs';
import { TransactionService } from './transaction-service';
import { TransactionActionService } from './transaction-action-service';
import { CategoryService } from './category-service';
import { NewTransaction } from './interfaces';

/**
 * A tranzakció létrehozó/szerkesztő modal állapotát és műveleteit fogja össze
 * (megnyitás/bezárás, tranzakció + kategórialista betöltése, mentés/törlés/kategória hozzáadása).
 *
 * Komponens szinten kell providerelni (providers: [TransactionModalStateService]), hogy minden
 * oldal saját, egymástól független state-tel rendelkezzen.
 *
 * Mivel a mentés/törlés utáni teendő (pl. lista újratöltése) oldalanként eltérő, ezt a `changed`
 * observable-ön keresztül a hívó oldal maga iratkozik fel rá.
 */
@Injectable()
export class TransactionModalStateService {
    private transactionService = inject(TransactionService);
    private transactionActionService = inject(TransactionActionService);
    private categoryService = inject(CategoryService);

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
     * Volt-e modal felnyitására kérés?
     */
    private isModalOpenRequested = signal(false);
    /**
     * Kategória lista betöltődött-e már?
     */
    private isCategoriesLoaded = signal(false);

    /**
     * Tranzakció létrehozó/szerkesztő modal nyitva van-e
     */
    public isTransactionModalOpen = signal(false);
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
        // todo átnézni, lehet-e egyszerűsíteni
        effect(() => {
            // Ha felnyitjuk a modalt, akkor lehet még nincs betöltve minden függősége. Ekkor a modal komponens fog egy loading ikont kirakni
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
     * @param {number | null} id -> ha null, akkor új tranzakció nyilik fel, ha szám, akkor adott id-jű tranzakció
     */
    public open(id: number | null): void {
        this.selectedTransactionIdTrigger.set(id);
        this.isModalOpenRequested.set(true);
        // modal felnyitása a constructor effect-ben van, ha minden api hívás lefutott
    }

    /**
     * Modal becsukása
     */
    public close(): void {
        this.selectedTransactionIdTrigger.set(null);
        this.isTransactionModalOpen.set(false);
        this.isModalOpenRequested.set(false);
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
     * Hozzáad egy új kategóriát és újratölti a kategórialistát
     */
    public saveCategory(categoryName: string): void {
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
