import { AsyncValidatorFn } from '@angular/forms';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth-service';

/**
 * Aszinkron validátor: az email cím foglalt-e már (hibakezelés ugyanúgy, mint a felhasználónévnél)
 *  Ha az ellenőrzés hibára fut (pl. nincs hálózat), nem blokkoljuk a regisztrációt, mert a backend
 * a küldéskor úgyis ellenőrzi az egyediséget
 */
export function uniqueEmailValidator(authService: AuthService): AsyncValidatorFn {
    return (control) =>
        authService.checkEmailUniqueness(control.value).pipe(
            map((isTaken) => (isTaken ? { emailTaken: true } : null)),
            catchError(() => of(null)),
        );
}
