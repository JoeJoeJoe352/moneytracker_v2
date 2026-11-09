import { Component, inject, output, resource, Signal, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom, Observable, tap } from 'rxjs';
import { DialogCloseButton } from '@shared/components/mat-modal-close';
import { NotificationService } from '@shared/services/notification-service';
import { CategoryService } from '../transaction/category-service';
import { CategoryResponseInterface } from '../transaction/interfaces';
import { RecurringTransactionFormComponent } from './recurring-transaction-form-component';
import {
    RecurringTransactionResponseInterface,
    RecurringTransactionSaveRequestInterface,
} from './interfaces';

export interface RecurringTransactionModalInputInterface {
    /**
     * Szerkesztendő ismétlődő tranzakció, null esetén új létrehozása
     */
    recurringTransaction: RecurringTransactionResponseInterface | null;
    /**
     * Mentés/leállítás/törlés folyamatban van-e (a hívó kezeli), addig a form le van tiltva
     */
    isSaving: Signal<boolean>;
}

/**
 * Ismétlődő tranzakció létrehozó/szerkesztő modal
 */
@Component({
    selector: 'app-recurring-transaction-modal',
    templateUrl: './recurring-transaction-modal.html',
    imports: [
        RecurringTransactionFormComponent,
        TranslatePipe,
        MatDialogModule,
        MatProgressSpinnerModule,
        DialogCloseButton,
    ],
})
export class RecurringTransactionModalComponent {
    private readonly categoryService = inject(CategoryService);
    private readonly notification = inject(NotificationService);

    protected readonly data = inject<RecurringTransactionModalInputInterface>(MAT_DIALOG_DATA);

    /**
     * Mentésre kattintott a user
     */
    public readonly saveRequested = output<RecurringTransactionSaveRequestInterface>();
    /**
     * Leállításra kattintott a user
     */
    public readonly stopRequested = output<void>();
    /**
     * Törlésre kattintott a user (a megerősítést a hívó kéri)
     */
    public readonly deleteRequested = output<void>();

    /**
     * Kategória mentés folyamatban van-e
     */
    protected readonly isAddingCategoryInProgress = signal(false);

    /**
     * Kategóriák listája
     */
    protected readonly categoriesResource = resource({
        defaultValue: [],
        loader: async () => {
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
     * Modal megnyitása. Sikeres mentés/leállítás/törlés esetén true-val tér vissza az afterClosed()
     */
    public static open(dialog: MatDialog, data: RecurringTransactionModalInputInterface) {
        return dialog.open<
            RecurringTransactionModalComponent,
            RecurringTransactionModalInputInterface,
            boolean
        >(RecurringTransactionModalComponent, {
            restoreFocus: true,
            width: '600px',
            data,
        });
    }

    /**
     * Hozzáad egy új kategóriát és beszúrja a kategórialistába
     */
    protected saveCategory = (categoryName: string): Observable<CategoryResponseInterface> => {
        this.isAddingCategoryInProgress.set(true);

        return this.categoryService.saveCategory({ name: categoryName }).pipe(
            tap({
                next: (category) => {
                    this.categoriesResource.update((categories) => [...categories, category]);
                    this.isAddingCategoryInProgress.set(false);
                },
                error: (err) => {
                    console.error('Problem with the category save', err);
                    this.notification.showGeneralError();
                    this.isAddingCategoryInProgress.set(false);
                },
            }),
        );
    };
}
