import { AbstractControl } from "@angular/forms";

export function validDate(control: AbstractControl) {
  const value = control.value;
  if (value === null) return null;
  return value instanceof Date && !isNaN(value.getTime())
    ? null
    : { invalidDate: true };
}