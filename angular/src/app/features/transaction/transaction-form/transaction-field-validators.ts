import { ValidatorFn, Validators } from '@angular/forms';
import { greaterThan } from './greater-than-validator';
import { validDate } from './valid-date-validator';

/**
 * Tranzakció név mező validátorai
 */
export const TRANSACTION_NAME_VALIDATORS: ValidatorFn[] = [
    Validators.required,
    Validators.minLength(3),
    Validators.maxLength(120),
];

/**
 * Pozitív összeg mezők validátorai (ár, mennyiség, egységár)
 */
export const POSITIVE_AMOUNT_VALIDATORS: ValidatorFn[] = [Validators.required, greaterThan(0)];

/**
 * Tranzakció dátum mező validátorai
 */
export const TRANSACTION_DATE_VALIDATORS: ValidatorFn[] = [Validators.required, validDate];
