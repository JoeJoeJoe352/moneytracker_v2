import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

@Component({
    selector: 'app-dialog-close-button',
    template: `
        <button class="close-icon" mat-icon-button class="close-button" [mat-dialog-close]="false">
            <mat-icon>close</mat-icon>
        </button>
    `,
    styles: `
        .close-button {
            right: 5px;
            top: 2px;
        }
    `,
    imports: [MatIconModule, MatDialogModule, MatButtonModule],
})
export class DialogCloseButton {}
