import { AbstractControl, ValidationErrors } from '@angular/forms';

/**
 * A dátum nem lehet a mai napnál korábbi. Csak a napot nézi, az időpontot nem, így a mai nap is
 * érvényes. Üres és érvénytelen dátumnál null-t ad, azt a required és a validDate kezeli
 */
export function notBeforeToday(control: AbstractControl): ValidationErrors | null {
    const value: unknown = control.value;
    if (!(value instanceof Date) || isNaN(value.getTime())) {
        return null;
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const day = new Date(value.getFullYear(), value.getMonth(), value.getDate());

    return day < today ? { beforeToday: true } : null;
}
