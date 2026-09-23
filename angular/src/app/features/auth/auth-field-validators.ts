import { ValidatorFn, Validators } from '@angular/forms';

/**
 * Felhasználónév mező validátorai (login és regisztráció)
 */
export const USERNAME_VALIDATORS: ValidatorFn[] = [
    Validators.required,
    Validators.minLength(3),
    Validators.maxLength(20),
];

/**
 * Jelszó mező validátorai (login és regisztráció)
 */
export const PASSWORD_VALIDATORS: ValidatorFn[] = [
    Validators.required,
    Validators.minLength(6),
    Validators.maxLength(20),
];
