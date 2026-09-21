import {
    AbstractControl,
    FormGroupDirective,
    NgForm,
    ValidationErrors,
    ValidatorFn,
} from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';

/**
 * Jelszó és jelszó konfirm mező egyezőségét vizsgáló validátor
 */
export const passwordMismatchValidator: ValidatorFn = (
    control: AbstractControl,
): ValidationErrors | null => {
    const password = control.get('password')?.value;
    const passwordAgain = control.get('passwordAgain')?.value;

    if (!password || !passwordAgain) {
        return null;
    }

    return password === passwordAgain ? null : { passwordMismatch: true };
};

/**
 * A jelszó egyezés hibát a form (csoport) szintjén jelöli a validátor.
 * A mat-form-field viszont csak akkor jeleníti meg a hibát, ha a mező hibás állapotú, ezért a
 * mezőnek a szülő csoport hibáját is figyelembe kell vennie
 */
export class PasswordMismatchErrorStateMatcher implements ErrorStateMatcher {
    isErrorState(control: AbstractControl | null, form: FormGroupDirective | NgForm | null): boolean {
        const hasError = control?.invalid || control?.parent?.hasError('passwordMismatch');
        return !!(hasError && (control?.touched || form?.submitted));
    }
}
