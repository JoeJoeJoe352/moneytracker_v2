import { Component, signal, WritableSignal } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { AuthService } from "../services/auth-service";

interface RegistrationValues {
    username: FormControl<string>
    email: FormControl<string>
    password: FormControl<string>
    passwordAgain: FormControl<string>
}

@Component({
    selector: "register-component",
    templateUrl: "../pages/register.html",
    imports: [ReactiveFormsModule],
})
export class RegisterComponent {
    registerForm: FormGroup
    isLoading: WritableSignal<boolean> = signal(false);
    errorMsg: WritableSignal<string> = signal('');
    
    constructor(private fb: FormBuilder, private authService: AuthService) {
        this.registerForm = this.fb.nonNullable.group({
            username: new FormControl('', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]),
            email: new FormControl('', [Validators.email, Validators.required]),
            password: new FormControl('', [Validators.required, Validators.minLength(6)]),
            passwordAgain: new FormControl('', [Validators.required]),
        })
    }

    onSubmit(): void {
        if (this.registerForm.invalid) {
            return;
        }
    }
}