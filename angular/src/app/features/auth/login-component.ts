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
    protected isLoading = inject<isLoadingInterface>(MAT_DIALOG_DATA).isloading;
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
