import { AbstractControl, ValidationErrors } from '@angular/forms';

/**
 * A dátum nem lehet az aktuális hónap előtt. Csak a napot nézi, az időpontot nem, így a hónap
 * első napja is érvényes. Üres és érvénytelen dátumnál null-t ad, azt a required és a validDate kezeli
 */
export function notBeforeThisMonth(control: AbstractControl): ValidationErrors | null {
    const value: unknown = control.value;
    if (!(value instanceof Date) || isNaN(value.getTime())) {
        return null;
    }

    // a mai nap hónapja számít, nem a kiválasztott dátumé
    const today = new Date();
    const firstDayOfCurrentMonth = new Date(today.getFullYear(), today.getMonth(), 1);
    const selectedDate = new Date(value.getFullYear(), value.getMonth(), value.getDate());

    return selectedDate < firstDayOfCurrentMonth ? { beforeThisMonth: true } : null;
}
