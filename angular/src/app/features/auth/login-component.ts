import { Component, inject, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatButton } from '@angular/material/button';
import { AuthDialogData, LoginRequestData } from './interfaces';
import { MatFormFieldModule } from '@angular/material/form-field';

@Component({
    selector: 'app-login-component',
    templateUrl: './login-component.html',
    styleUrl: './login-component.scss',
    imports: [
        ReactiveFormsModule,
        TranslatePipe,
        MatDialogModule,
        MatFormFieldModule,
        MatButton,
        MatInputModule,
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
        username: ['', Validators.required],
        password: ['', Validators.required],
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
