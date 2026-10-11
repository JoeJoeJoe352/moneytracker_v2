import { Component, inject, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthDialogData, LoginRequestData } from './interfaces';
import { MatFormFieldModule } from '@angular/material/form-field';
import { DialogCloseButton } from '@app/shared/components/mat-modal-close';
import { PASSWORD_VALIDATORS, USERNAME_VALIDATORS } from './auth-field-validators';

@Component({
    selector: 'app-login-component',
    templateUrl: './login-component.html',
    styleUrl: './login-component.scss',
    imports: [
        ReactiveFormsModule,
        TranslatePipe,
        MatDialogModule,
        MatFormFieldModule,
        MatButtonModule,
        MatInputModule,
        DialogCloseButton,
    ],
})
export class LoginComponent {
    private readonly fb = inject(FormBuilder);

    public login = output<LoginRequestData>();

    /**
     * Töltődés alatt van-e a form
     */
    protected isLoading = inject<AuthDialogData>(MAT_DIALOG_DATA).isLoading;
    /**
     * login form adatai
     */
    protected readonly loginForm = this.fb.nonNullable.group({
        username: ['', { validators: USERNAME_VALIDATORS }],
        password: ['', { validators: PASSWORD_VALIDATORS }],
    });

    /**
     * Login adatok küldése
     */
    onSubmit(): void {
        if (this.loginForm.invalid) {
            this.loginForm.markAllAsTouched();
            return;
        }

        this.login.emit(this.loginForm.getRawValue());
    }
}
