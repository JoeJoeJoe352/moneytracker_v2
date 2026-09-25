import { AbstractControl, ValidatorFn, Validators } from '@angular/forms';
import { validateWhen } from '@shared/utils/form-util';
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

/**
 * Komplex (részletes) tranzakció-e a form, amihez a kontroll tartozik
 */
const isComplexTransaction = (control: AbstractControl): boolean =>
    control.root.get('isComplexTransaction')?.value === true;

/**
 * Komplex ár megadási módban (mennyiség * egységár) van-e a detail sor, amihez a kontroll tartozik
 */
const isDetailComplexPriceMode = (control: AbstractControl): boolean =>
    control.parent?.get('detailIsComplexPriceMode')?.value === true;

/**
 * Globális ár: csak egyszerű tranzakciónál látható
 */
export const GLOBAL_PRICE_VALIDATORS: ValidatorFn[] = [
    validateWhen((control) => !isComplexTransaction(control), POSITIVE_AMOUNT_VALIDATORS),
];

/**
 * Detail név: csak komplex tranzakciónál látható
 */
export const DETAIL_NAME_VALIDATORS: ValidatorFn[] = [
    validateWhen(isComplexTransaction, TRANSACTION_NAME_VALIDATORS),
];

/**
 * Detail ár: komplex tranzakciónál, egyszerű ár megadási módban látható
 */
export const DETAIL_PRICE_VALIDATORS: ValidatorFn[] = [
    validateWhen(
        (control) => isComplexTransaction(control) && !isDetailComplexPriceMode(control),
        POSITIVE_AMOUNT_VALIDATORS,
    ),
];

/**
 * Detail mennyiség és egységár: komplex tranzakciónál, komplex ár megadási módban látható
 */
export const DETAIL_WEIGHT_AND_UNIT_PRICE_VALIDATORS: ValidatorFn[] = [
    validateWhen(
        (control) => isComplexTransaction(control) && isDetailComplexPriceMode(control),
        POSITIVE_AMOUNT_VALIDATORS,
    ),
];
