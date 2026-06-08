import { Component, signal, WritableSignal } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { AuthService } from "../services/auth-service";
import { NgClass } from "@angular/common";
import { passwordMismatchValidator } from "../directives/password-match.directive";

const STRICT_EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
@Component({
    selector: "register-component",
    templateUrl: "../pages/register.html",
    imports: [ReactiveFormsModule, NgClass],
    styleUrls: ["../../../shared/components/form-style.scss"],
})
export class RegisterComponent {
    registerForm: FormGroup
    isLoading: WritableSignal<boolean> = signal(false);
    errorMsg: WritableSignal<string> = signal('');
    
    constructor(private fb: FormBuilder, private authService: AuthService) {
        this.registerForm = this.fb.nonNullable.group(
            {
                username: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(20)]],
                email: ['', [Validators.pattern(STRICT_EMAIL_REGEX), Validators.required]],
                password: ['', [Validators.required, Validators.minLength(6)]],
                passwordAgain: ['', [Validators.required]],
            }, {
                validators: passwordMismatchValidator(),
            }
        )
    }

    onSubmit(): void {
        if (this.registerForm.invalid) {
            return;
        }
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