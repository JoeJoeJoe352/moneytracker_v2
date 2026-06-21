import { AbstractControl } from "@angular/forms";

/**
 * Datepicker validálása
 */
export function validDate(control: AbstractControl): {invalidDate: boolean} | null {
  const value = control.value;
  if (value === null) return null;
  return value instanceof Date && !isNaN(value.getTime())
    ? null
    : { invalidDate: true };
}