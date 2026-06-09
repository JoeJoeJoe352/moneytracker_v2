import { Component, inject, signal, WritableSignal } from "@angular/core";
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { NgClass } from "@angular/common";
import { AuthService } from "./auth-service";

const ERROR_LEVEL_NONE = 0
const ERROR_LEVEL_USER_ERROR = 1
const ERROR_LEVEL_SYSTEM_ERROR = 2

@Component({
    selector: "app-login-component",
    templateUrl: './login-component.html',
    imports: [ReactiveFormsModule, NgClass],
    styleUrls: ["../../shared/components/form-style.scss"],
})
export class LoginComponent {
    private fb = inject(FormBuilder)
    private authService = inject(AuthService)

    loginForm: FormGroup;
    /**
     * Error message from backend when login fails
     */
    backendErrorMsg: WritableSignal<string> = signal('');
    /**
     * Is the form loading?
     */
    isLoading: WritableSignal<boolean> = signal(false);
    /**
     * Error level
     */
    errorLevel: WritableSignal<number> = signal(ERROR_LEVEL_NONE)


    constructor() {
        // AuthService injektálva van a komponensben, mert @Inject annotációs dekorátorral van ellátva, így a DI konténer tudja, hogy létre kell hoznia egy példányt belőle, és át kell adnia a konstruktorban.
        this.loginForm = this.fb.nonNullable.group({
            username: ['', Validators.required],
            password: ['', Validators.required],
        })
    }

    /**
     * Submit login form data to backend
     */
    onSubmit(): void {
        if (this.loginForm.invalid) {
            return;
        }
        // reactive element in zoneless mode, so we need to manually set the signals
        this.backendErrorMsg.set('');
        this.isLoading.set(true);
        this.errorLevel.set(ERROR_LEVEL_NONE);
        const { username, password } = this.loginForm.getRawValue();
        this.authService.login(username, password).subscribe({
            next: () => {
                this.isLoading.set(false);
                // TODO redirect to home page
            },
            error: (response) => {
                if (response.status === 401) {
                    this.errorLevel.set(ERROR_LEVEL_USER_ERROR)
                    this.backendErrorMsg.set(response.error.message);
                } else {
                    this.errorLevel.set(ERROR_LEVEL_SYSTEM_ERROR)
                    console.error("Ismeretlen hiba történt a bejelentkezés során!", response);
                    this.backendErrorMsg.set('Unknown error happened, please try again later');
                }
                this.isLoading.set(false);
            },
        })
    }

    /**
     * Check if there is a problem with the username field after the user interacted with it.
     */
    get isUsernameFieldHasError(): boolean {
        return this.loginForm.controls['username'].touched && 
            this.loginForm.controls['username'].hasError('required');
    }
    /**
     * Check if there is a problem with the password field after the user interacted with it.
     */
    get isPasswordFieldHasError(): boolean {
        return this.loginForm.controls['password'].touched 
            && this.loginForm.controls['password'].hasError('required');
    }

    /**
     * Check if there is an user error from backend. 
     * It modify the apperance of form elements in the html
     */
    get hasUserLoginError(): boolean {
        return this.errorLevel() === ERROR_LEVEL_USER_ERROR
    }
}