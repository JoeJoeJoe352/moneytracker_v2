import { Component, EventEmitter, inject, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatButton } from '@angular/material/button';
import { isLoadingInterface, LoginRequestData } from './interfaces';
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

    @Output() login = new EventEmitter<LoginRequestData>();

    /**
     * Töltődés alatt van-e a form
     */
    protected isLoading = inject<isLoadingInterface>(MAT_DIALOG_DATA).isloading;
    /**
     * login form adatai
     */
    protected loginForm: FormGroup;

    constructor() {
        this.loginForm = this.fb.nonNullable.group({
            username: ['', Validators.required],
            password: ['', Validators.required],
        });
    }

    /**
     * Login adatok küldése
     */
    onSubmit(): void {
        if (this.loginForm.invalid) {
            this.loginForm.markAllAsTouched();
            return;
        }

        const { username, password } = this.loginForm.getRawValue();
        this.login.emit({ username: username, password: password });
    }

    /**
     * Megnézi van-e valami hiba a felhasználónév mezőben
     */
    get isUsernameFieldHasError(): boolean {
        return (
            this.loginForm.controls['username'].touched &&
            this.loginForm.controls['username'].hasError('required')
        );
    }
    /**
     * Ellenőrzi a jelszó mezőt, van-e valami hiba
     */
    get isPasswordFieldHasError(): boolean {
        return (
            this.loginForm.controls['password'].touched &&
            this.loginForm.controls['password'].hasError('required')
        );
    }
}
