import { Component, EventEmitter, inject, Output } from '@angular/core';
import {
    FormBuilder,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators,
} from '@angular/forms';
import { passwordMismatchValidator } from './password-match.directive';
import { UniqueNameAndEmailDirective } from './unique-username.directive.';
import { TranslatePipe } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { isLoadingInterface, RegisterRequestData } from './interfaces';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';

const STRICT_EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
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
    ],
})
export class RegisterComponent {
    private readonly fb = inject(FormBuilder);
    private readonly uniqueValidator = inject(UniqueNameAndEmailDirective);

    /**
     * Töltődés alatt van-e a form
     */
    protected isLoading = inject<isLoadingInterface>(MAT_DIALOG_DATA).isloading;
    /**
     * Regisztrációs gombra rákattintott-e a user
     */
    @Output() register = new EventEmitter<RegisterRequestData>();

    registerForm: FormGroup;

    constructor() {
        this.registerForm = this.fb.nonNullable.group(
            {
                username: [
                    '',
                    {
                        validators: [
                            Validators.required,
                            Validators.minLength(3),
                            Validators.maxLength(20),
                        ],
                        asyncValidators: [
                            this.uniqueValidator.validateUsername.bind(this.uniqueValidator),
                        ],
                        updateOn: 'blur',
                    },
                ],
                email: [
                    '',
                    {
                        validators: [Validators.pattern(STRICT_EMAIL_REGEX), Validators.required],
                        asyncValidators: [
                            this.uniqueValidator.validateEmail.bind(this.uniqueValidator),
                        ],
                        updateOn: 'blur',
                    },
                ],
                password: ['', [Validators.required, Validators.minLength(6)]],
                passwordAgain: ['', [Validators.required]],
            },
            {
                validators: passwordMismatchValidator(),
            },
        );
    }

    /**
     * Form küldéskor lefutó kódok
     */
    onSubmit(): void {
        if (this.registerForm.invalid) {
            return;
        }

        const params = {
            email: this.email.value,
            username: this.username.value,
            password: this.password.value,
        };
        this.register.emit(params);
    }

    // Getters
    get username(): FormControl<string> {
        return this.registerForm.get('username') as FormControl<string>;
    }

    get email(): FormControl<string> {
        return this.registerForm.get('email') as FormControl<string>;
    }

    get password(): FormControl<string> {
        return this.registerForm.get('password') as FormControl<string>;
    }

    get passwordAgain(): FormControl<string> {
        return this.registerForm.get('passwordAgain') as FormControl<string>;
    }

    get hasPasswordMismatchError(): boolean {
        return this.registerForm.hasError('passwordMismatch') && this.registerForm.touched;
    }
}
