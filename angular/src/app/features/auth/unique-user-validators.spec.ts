import { describe, it, expect, vi } from 'vitest';
import { AsyncValidatorFn, FormControl, ValidationErrors } from '@angular/forms';
import { firstValueFrom, Observable, of, throwError } from 'rxjs';
import { AuthService } from './auth-service';
import { uniqueEmailValidator } from './unique-user-validators';

describe('unique user validators (Vitest)', () => {
    function validate(
        validator: AsyncValidatorFn,
        value: string,
    ): Promise<ValidationErrors | null> {
        return firstValueFrom(
            validator(new FormControl(value)) as Observable<ValidationErrors | null>,
        );
    }

    function authServiceWith(
        checkEmailUniqueness: () => Observable<boolean> = () => of(false),
    ): AuthService {
        return { checkEmailUniqueness } as unknown as AuthService;
    }

    it('should report emailTaken when the email already exists', async () => {
        const checkEmailUniqueness = vi.fn(() => of(true));
        const validator = uniqueEmailValidator(authServiceWith(checkEmailUniqueness));

        expect(await validate(validator, 'joe@example.com')).toEqual({ emailTaken: true });
        expect(checkEmailUniqueness).toHaveBeenCalledWith('joe@example.com');
    });

    it('should accept a free email', async () => {
        const validator = uniqueEmailValidator(authServiceWith(() => of(false)));

        expect(await validate(validator, 'joe@example.com')).toBeNull();
    });

    it('should not block the registration when the email check fails', async () => {
        const validator = uniqueEmailValidator(
            authServiceWith(() => throwError(() => new Error('offline'))),
        );

        expect(await validate(validator, 'joe@example.com')).toBeNull();
    });
});
