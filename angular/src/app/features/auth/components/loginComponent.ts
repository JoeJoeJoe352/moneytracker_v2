import { Component, NgZone, signal, WritableSignal } from "@angular/core";
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { NgClass } from "@angular/common";
import { AuthService } from "../services/auth-service";

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
     * Hibaüzenet a backendtől
     */
    errorMsg: WritableSignal<string> = signal('');
    /**
     * Töltődik-e a form
     */
    isLoading: WritableSignal<boolean> = signal(false);

    constructor(private fb: FormBuilder, private authService: AuthService, private ngZone: NgZone) {
        // AuthService injektálva van a komponensben, mert @Inject annotációs dekorátorral van ellátva, így a DI konténer tudja, hogy létre kell hoznia egy példányt belőle, és át kell adnia a konstruktorban.
        this.loginForm = this.fb.group({
            username: ["", Validators.required],
            password: ["", Validators.required]
        })
    }

    /**
     * Login form elküldése
     */
    onSubmit(): void {
        if (this.loginForm.invalid) {
            return;
        }
        // így lehet zoneless módban reaktív elemet berakni
        this.errorMsg.update(() => '');
        this.isLoading.update(() => true);
        const { username, password } = this.loginForm.value;
        this.authService.login(username, password).subscribe({
            next: () => {
                this.isLoading.update(() => false);
                // TODO redirect to home page
            },
            error: (response) => {
                console.log('123', NgZone.isInAngularZone())
                if (response.status === 401) {
                    this.ngZone.run(() => {
                        this.errorMsg.update(() => response.error.message);
                    });
                } else {
                    console.error("Ismeretlen hiba történt a bejelentkezés során!", response);
                }
                this.isLoading.update(() => false);
            },
        })
    }

    /**
     * Megnézi, hogy valid-e a felhasználónév input. Csak azután mond hibát, ha a felhasználó hozzáért az inputhoz valaha
     *
     * @return boolean
     */
    get isUsernameFieldHasError(): boolean {
        const usernameInputContent = this.loginForm.get('username');
        if (!usernameInputContent) {
            throw new Error("Nincs ilyen nevű input a formban!");
        }
        return usernameInputContent.touched && usernameInputContent.invalid;
    }
    /**
     * Megnézi, hogy valid-e a jelszó input. Csak azután mond hibát, ha a felhasználó hozzáért az inputhoz valaha
     * 
     * @return boolean
     */
    get isPasswordFieldHasError(): boolean {
        const passwordInputContent = this.loginForm.get('password');
        if (!passwordInputContent) {
            throw new Error("Nincs ilyen nevű input a formban!");
        }
        return passwordInputContent.touched && passwordInputContent.invalid;
    }

}