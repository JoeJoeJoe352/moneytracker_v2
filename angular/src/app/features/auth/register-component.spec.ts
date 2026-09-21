import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { RegisterComponent } from './register-component';
import { AuthService } from './auth-service';
import { RegisterRequestData } from './interfaces';

describe('RegisterComponent (Vitest)', () => {
    let fixture: ComponentFixture<RegisterComponent>;
    let component: RegisterComponent;
    let emitted: RegisterRequestData[];

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [RegisterComponent],
            providers: [
                provideTranslateService(),
                { provide: MAT_DIALOG_DATA, useValue: { isLoading: signal(false) } },
                {
                    provide: AuthService,
                    useValue: {
                        // a foglaltság-ellenőrzés itt mindig szabadot ad vissza
                        checkNameUniqueness: () => of(false),
                        checkEmailUniqueness: () => of(false),
                    },
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(RegisterComponent);
        component = fixture.componentInstance;
        emitted = [];
        component.register.subscribe((params) => emitted.push(params));
        fixture.detectChanges();
    });

    function input(selector: string): HTMLInputElement {
        return fixture.nativeElement.querySelector(selector);
    }

    /**
     * Beírja az értéket, és blur-t küld: a username és email mező updateOn: 'blur', a többinél a touched miatt kell
     */
    function fill(selector: string, value: string): void {
        const element = input(selector);
        element.value = value;
        element.dispatchEvent(new Event('input'));
        element.dispatchEvent(new Event('blur'));
    }

    function fillValidForm(): void {
        fill('#reg-username', 'joe');
        fill('#reg-email', 'joe@example.com');
        fill('#reg-password', 'secret1');
        fill('#reg-password-again', 'secret1');
        fixture.detectChanges();
    }

    function errorTexts(): string[] {
        return Array.from(fixture.nativeElement.querySelectorAll('mat-error')).map((error) =>
            (error as HTMLElement).textContent!.trim(),
        );
    }

    it('should show the password mismatch error under the confirm field when the passwords differ', () => {
        fill('#reg-password', 'secret1');
        fill('#reg-password-again', 'secret2');
        fixture.detectChanges();

        expect(errorTexts()).toContain('register.password_again.mismatch-error');
    });

    it('should not show the password mismatch error when the passwords are equal', () => {
        fill('#reg-password', 'secret1');
        fill('#reg-password-again', 'secret1');
        fixture.detectChanges();

        expect(errorTexts()).not.toContain('register.password_again.mismatch-error');
    });

    it('should not emit "register" and should mark the controls touched when the form is invalid', () => {
        input('button[type="submit"]').click();
        fixture.detectChanges();

        expect(emitted).toEqual([]);
        expect(component.username.touched).toBe(true);
        expect(errorTexts()).toContain('register.username.required-error');
    });

    it('should emit "register" with the username, email and password (without the confirm field) when valid', () => {
        fillValidForm();

        input('button[type="submit"]').click();

        expect(emitted).toEqual([
            { username: 'joe', email: 'joe@example.com', password: 'secret1' },
        ]);
    });
});
