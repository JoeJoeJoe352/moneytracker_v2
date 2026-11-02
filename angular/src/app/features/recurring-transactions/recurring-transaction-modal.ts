import { Component, inject, resource, signal } from '@angular/core';
import {
    MAT_DIALOG_DATA,
    MatDialog,
    MatDialogModule,
    MatDialogRef,
} from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { _, TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom, Observable, tap } from 'rxjs';
import { DialogCloseButton } from '@shared/components/mat-modal-close';
import { NotificationService } from '@shared/services/notification-service';
import { CategoryService } from '../transaction/category-service';
import { CategoryResponseInterface } from '../transaction/interfaces';
import { RecurringTransactionFormComponent } from './recurring-transaction-form-component';
import { RecurringTransactionService } from './recurring-transactions-service';
import {
    RecurringTransactionResponseInterface,
    RecurringTransactionSaveRequestInterface,
} from './interfaces';

export interface RecurringTransactionModalInputInterface {
    /**
     * Szerkesztendő ismétlődő tranzakció, null esetén új létrehozása
     */
    recurringTransaction: RecurringTransactionResponseInterface | null;
}

/**
 * Ismétlődő tranzakció létrehozó/szerkesztő modal. Sikeres mentés után true értékkel zárul be
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
    private readonly dialogRef =
        inject<MatDialogRef<RecurringTransactionModalComponent, boolean>>(MatDialogRef);
    private readonly service = inject(RecurringTransactionService);
    private readonly categoryService = inject(CategoryService);
    private readonly notification = inject(NotificationService);

    protected readonly data = inject<RecurringTransactionModalInputInterface>(MAT_DIALOG_DATA);

    /**
     * Mentés folyamatban van-e
     */
    protected readonly isSaving = signal(false);
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
     * Modal megnyitása. Sikeres mentés esetén true-val tér vissza az afterClosed()
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
     * Elmenti (létrehozza vagy módosítja) az ismétlődő tranzakciót
     */
    protected save(payload: RecurringTransactionSaveRequestInterface): void {
        const id = this.data.recurringTransaction?.id ?? null;
        const request =
            id === null
                ? this.service.createRecurringTransaction(payload)
                : this.service.updateRecurringTransaction(id, payload);

        this.isSaving.set(true);
        request.subscribe({
            next: () => {
                this.isSaving.set(false);
                this.notification.show(_('etc.save-success'));
                this.dialogRef.close(true);
            },
            error: () => this.isSaving.set(false),
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
