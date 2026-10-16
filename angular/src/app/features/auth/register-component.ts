import { Component, inject, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
    PasswordMismatchErrorStateMatcher,
    passwordMismatchValidator,
} from './password-match-validator';
import { uniqueEmailValidator } from './unique-user-validators';
import {
    EMAIL_VALIDATORS,
    PASSWORD_VALIDATORS,
    USERNAME_VALIDATORS,
} from './auth-field-validators';
import { AuthService } from './auth-service';
import { TranslatePipe } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { AuthDialogData, RegisterRequestData } from './interfaces';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { DialogCloseButton } from '@shared/components/mat-modal-close';

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
     * A jelszó egyezés hibát (ami a form szintjén van) a confirm mezőnél is hibaként jeleníti meg
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
            username: ['', USERNAME_VALIDATORS],
            email: [
                '',
                {
                    validators: EMAIL_VALIDATORS,
                    asyncValidators: [uniqueEmailValidator(this.authService)],
                    updateOn: 'blur',
                },
            ],
            password: ['', PASSWORD_VALIDATORS],
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
        if (this.registerForm.invalid || this.registerForm.pending) {
            this.registerForm.markAllAsTouched();
            return;
        }

        // a passwordAgain csak az ellenőrzéshez kell, a backendnek nem küldjük el
        const { email, username, password } = this.registerForm.getRawValue();
        this.register.emit({ email, username, password });
    }
}
