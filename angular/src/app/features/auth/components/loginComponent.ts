import { Component, signal, WritableSignal } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { NgClass } from "@angular/common";
import { AuthService } from "../services/auth-service";

interface LoginData {
    username: FormControl<string|null>,
    password: FormControl<string|null>,
}

@Component({
    selector: "login-component",
    templateUrl: '../pages/login.html',
    imports: [ReactiveFormsModule, NgClass],
    styleUrls: ["../../../shared/components/form-style.scss"],
    standalone: true,
})
export class LoginComponent {
    loginForm: FormGroup;
    /**
     * Error message from backend when login fails
     */
    errorMsg: WritableSignal<string> = signal('');
    /**
     * Is the form loading?
     */
    isLoading: WritableSignal<boolean> = signal(false);

    constructor(private fb: FormBuilder, private authService: AuthService) {
        // AuthService injektálva van a komponensben, mert @Inject annotációs dekorátorral van ellátva, így a DI konténer tudja, hogy létre kell hoznia egy példányt belőle, és át kell adnia a konstruktorban.
        this.loginForm = this.fb.nonNullable.group<LoginData>({
            username: new FormControl('', [Validators.required]),
            password: new FormControl('', [Validators.required]),
        })
    }

    /**
     * Submit login form data to backend
     */
    onSubmit(): void {
        if (this.loginForm.invalid) {
            return;
        }
        // reactive element in zoneless mode, so we need to manually update the signals
        this.errorMsg.update(() => '');
        this.isLoading.update(() => true);
        const { username, password } = this.loginForm.value;
        this.authService.login(username, password).subscribe({
            next: () => {
                this.isLoading.update(() => false);
                // TODO redirect to home page
            },
            error: (response) => {
                if (response.status === 401) {
                    this.errorMsg.update(() => response.error.message);
                } else {
                    console.error("Ismeretlen hiba történt a bejelentkezés során!", response);
                }
                this.isLoading.update(() => false);
            },
        })
    }

    /////////////
    // GETTERS //
    /////////////
    
    /**
     * Check if there is a problem with the username field after the user interacted with it.
     *
     * @return boolean
     */
    get isUsernameFieldHasError(): boolean {
        return (this.username.touched && this.username.invalid);
    }
    /**
     * Check if there is a problem with the password field after the user interacted with it.
     *
     * @return boolean
     */
    get isPasswordFieldHasError(): boolean {
        return this.password.touched && this.password.invalid;
    }

    /**
     * Get FormControl object of the username form field
     */
    get username(): FormControl<string> {
        return this.loginForm.get('username') as FormControl<string>;
    }

    /**
     * Get FormControl object of the password form field
     */
    get password(): FormControl<string> {
        return this.loginForm.get('password') as FormControl<string>;
    }
    
}