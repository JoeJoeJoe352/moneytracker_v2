import { ValidatorFn, Validators } from '@angular/forms';
import { STRICT_EMAIL_REGEX } from '@app/shared/constants';

/**
 * Felhasználónév mező validátorai
 */
export const USERNAME_VALIDATORS: ValidatorFn[] = [
    Validators.required,
    Validators.minLength(3),
    Validators.maxLength(20),
];

/**
 * Jelszó mező validátorai
 */
export const PASSWORD_VALIDATORS: ValidatorFn[] = [
    Validators.required,
    Validators.minLength(6),
    Validators.maxLength(20),
];

/**
 * Email mező validátorai
 */
export const EMAIL_VALIDATORS: ValidatorFn[] = [
    Validators.pattern(STRICT_EMAIL_REGEX),
    Validators.required,
    Validators.maxLength(30),
];
