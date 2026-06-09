import { Component, EventEmitter, inject, Output, signal, WritableSignal } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { AuthService } from "./auth-service";
import { NgClass } from "@angular/common";
import { passwordMismatchValidator } from "./password-match.directive";
import { UniqueNameAndEmailDirective } from "./unique-username.directive.";

const STRICT_EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
@Component({
    selector: "app-register-component",
    templateUrl: "./register-component.html",
    imports: [ReactiveFormsModule, NgClass],
    styleUrls: ["../../shared/components/form-style.scss"],
})
export class RegisterComponent {
    @Output() closeModal = new EventEmitter<void>();

    // injektálások 
    private fb = inject(FormBuilder)
    private authService = inject(AuthService)
    private uniqueValidator = inject(UniqueNameAndEmailDirective)

    registerForm: FormGroup
    /**
     * Töltődés alatt van-e a form
     */
    isLoading: WritableSignal<boolean> = signal(false);
    /**
     * Van-e a hiba a backendről
     */
    backendErrorMsg: WritableSignal<string> = signal('');

    constructor() {
        this.registerForm = this.fb.nonNullable.group(
            {
                username: ['', {
                    validators: [Validators.required, Validators.minLength(5), Validators.maxLength(20)],
                    asyncValidators: [this.uniqueValidator.validateUsername.bind(this.uniqueValidator)],
                    updateOn: 'blur'
                }],
                email: ['', {
                    validators: [Validators.pattern(STRICT_EMAIL_REGEX), Validators.required],
                    asyncValidators: [this.uniqueValidator.validateEmail.bind(this.uniqueValidator)],
                    updateOn: 'blur',
                }],
                password: ['', [Validators.required, Validators.minLength(6)]],
                passwordAgain: ['', [Validators.required]],
            }, {
                validators: passwordMismatchValidator(),
            }
        )
    }

    /**
     * Form küldéskor lefutó kódok
     */
    onSubmit(): void {
        if (this.registerForm.invalid) {
            return;
        }

        this.isLoading.set(true)
        this.backendErrorMsg.set('')

        this.authService.register(this.username.value, this.email.value, this.password.value, this.passwordAgain.value).subscribe({
            next: () => {
                this.isLoading.set(false);
                this.closeModal.emit()
            },
            error: (response) => {
                console.error("unknown error during register!", response);
                this.backendErrorMsg.set('Unknown error happened, please try again later');
                this.isLoading.set(false);
            },
        })
        
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