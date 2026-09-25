import { AbstractControl, FormArray, FormGroup, ValidatorFn, Validators } from '@angular/forms';

/**
 * A megadott validátorokat csak akkor futtatja, ha a feltétel teljesül (pl. a mező az aktuális
 * módban látható). Így a nem látható mezőket nem kell letiltani, hogy ne tegyék invaliddá a formot.
 * A feltétel változásakor a mezőt újra kell validálni (lásd updateTreeValidity)
 */
export function validateWhen(
    condition: (control: AbstractControl) => boolean,
    validators: ValidatorFn[],
): ValidatorFn {
    const composedValidator = Validators.compose(validators);
    return (control) =>
        composedValidator && condition(control) ? composedValidator(control) : null;
}

/**
 * Újrafuttatja a validátorokat a kontrollon és az összes leszármazottján (előbb a gyerekeken,
 * hogy a szülő státusza már a frissített gyerek státuszokból számolódjon).
 */
export function updateTreeValidity(control: AbstractControl): void {
    if (control instanceof FormGroup || control instanceof FormArray) {
        Object.values(control.controls).forEach((child: AbstractControl) =>
            updateTreeValidity(child),
        );
    }
    control.updateValueAndValidity({ onlySelf: true, emitEvent: false });
}
