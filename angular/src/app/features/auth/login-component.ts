import { Component, EventEmitter, inject, Output, WritableSignal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormField, MatLabel, MatError, MatInputModule } from '@angular/material/input';
import { MatButton } from '@angular/material/button';
import { LoginRequestData } from './interfaces';

export interface LoginDialogData {
    isloading: WritableSignal<boolean>;
}

@Component({
    selector: 'app-login-component',
    templateUrl: './login-component.html',
    styleUrl: './login-component.scss',
    imports: [
        ReactiveFormsModule,
        TranslatePipe,
        MatDialogModule,
        MatFormField,
        MatLabel,
        MatError,
        MatButton,
        MatInputModule,
    ],
})
export class LoginComponent {
    protected data = inject<LoginDialogData>(MAT_DIALOG_DATA);
    @Output() login = new EventEmitter<LoginRequestData>();

    private readonly fb = inject(FormBuilder);

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
            return;
        }

        const { username, password } = this.loginForm.getRawValue();
        this.login.emit({ username: username, password: password });
    }

    /**
     * Check if there is a problem with the username field after the user interacted with it.
     */
    get isUsernameFieldHasError(): boolean {
        return (
            this.loginForm.controls['username'].touched &&
            this.loginForm.controls['username'].hasError('required')
        );
    }
    /**
     * Check if there is a problem with the password field after the user interacted with it.
     */
    get isPasswordFieldHasError(): boolean {
        return (
            this.loginForm.controls['password'].touched &&
            this.loginForm.controls['password'].hasError('required')
        );
    }
}
