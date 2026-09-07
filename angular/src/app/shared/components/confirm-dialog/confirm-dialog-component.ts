import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';

export interface ConfirmDialogData {
    /**
     * A megerősítendő üzenet szövege
     */
    message: string;
    /**
     * Opcionális cím a dialog tetején
     */
    title?: string;
    /**
     * Megerősítő gomb felirata (alapértelmezett: "Confirm" - nyelvesítve)
     */
    confirmLabel?: string;
    /**
     * Elutasító gomb felirata (alapértelmezett: "Cancel" - nyelvesítve)
     */
    cancelLabel?: string;
    /**
     * Veszélyes (pl. törlés) művelet-e, ekkor a megerősítő gomb piros lesz
     */
    danger?: boolean;
}

@Component({
    selector: 'app-confirm-dialog',
    templateUrl: './confirm-dialog-component.html',
    standalone: true,
    imports: [MatDialogModule, MatButtonModule, TranslatePipe],
})
export class ConfirmDialogComponent {
    protected data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}
