import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Olyan validátor, mint a Validators.min(), csak szigorúan (egyenlőség nélkül) nagyobbnak kell
 * lennie a mező értékének a megadott határnál
 */
export function greaterThan(min: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
        const value = control.value;
        if (value === null || value === '') {
            return null;
        }

        return value > min ? null : { greaterThan: { min, actual: value } };
    };
}
