import { Component, inject, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
    PasswordMismatchErrorStateMatcher,
    passwordMismatchValidator,
} from './password-match-validator';
import { uniqueEmailValidator, uniqueUsernameValidator } from './unique-user-validators';
import { AuthService } from './auth-service';
import { TranslatePipe } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { AuthDialogData, RegisterRequestData } from './interfaces';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { DialogCloseButton } from '../../shared/components/mat-modal-close';
import { STRICT_EMAIL_REGEX } from '../../shared/constants';

@Component({
    selector: 'app-register-component',
    templateUrl: './register-component.html',
    styleUrl: './register-component.scss',
    imports: [
        ReactiveFormsModule,
        TranslatePipe,
        MatFormFieldModule,
        MatButtonModule,
        MatDialogModule,
        MatInputModule,
        DialogCloseButton,
    ],
})
export class RegisterComponent {
    private readonly fb = inject(FormBuilder);
    private readonly authService = inject(AuthService);

    /**
     * A jelszó egyezés hibát (ami a form szintjén van) a konfirm mezőnél is hibaként jeleníti meg
     */
    protected readonly passwordMismatchMatcher = new PasswordMismatchErrorStateMatcher();

    /**
     * Regisztrációs gombra rákattintott-e a user
     */
    public register = output<RegisterRequestData>();

    /**
     * Töltődés alatt van-e a form
     */
    protected isLoading = inject<AuthDialogData>(MAT_DIALOG_DATA).isLoading;
    /**
     * Regisztrációs form beállításai
     */
    protected readonly registerForm = this.fb.nonNullable.group(
        {
            username: [
                '',
                {
                    validators: [
                        Validators.required,
                        Validators.minLength(3),
                        Validators.maxLength(20),
                    ],
                    asyncValidators: [uniqueUsernameValidator(this.authService)],
                    updateOn: 'blur',
                },
            ],
            email: [
                '',
                {
                    validators: [
                        Validators.pattern(STRICT_EMAIL_REGEX),
                        Validators.required,
                        Validators.maxLength(30),
                    ],
                    asyncValidators: [uniqueEmailValidator(this.authService)],
                    updateOn: 'blur',
                },
            ],
            password: [
                '',
                [Validators.required, Validators.minLength(6), Validators.maxLength(20)],
            ],
            passwordAgain: ['', [Validators.required]],
        },
        {
            validators: passwordMismatchValidator,
        },
    );

    /**
     * Form küldéskor lefutó kódok
     */
    onSubmit(): void {
        if (this.registerForm.invalid) {
            this.registerForm.markAllAsTouched();
            return;
        }

        // a passwordAgain csak az ellenőrzéshez kell, a backendnek nem küldjük el
        const { email, username, password } = this.registerForm.getRawValue();
        this.register.emit({ email, username, password });
    }

    // Getters
    get username() {
        return this.registerForm.controls.username;
    }

    get email() {
        return this.registerForm.controls.email;
    }

    get password() {
        return this.registerForm.controls.password;
    }

    get passwordAgain() {
        return this.registerForm.controls.passwordAgain;
    }
}
