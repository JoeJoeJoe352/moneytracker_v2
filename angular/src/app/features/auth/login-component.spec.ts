import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { provideTranslateService } from '@ngx-translate/core';
import { LoginComponent } from './login-component';
import { LoginRequestData } from './interfaces';

describe('LoginComponent (Vitest)', () => {
    let fixture: ComponentFixture<LoginComponent>;
    let component: LoginComponent;
    let emitted: LoginRequestData[];

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [LoginComponent],
            providers: [
                provideTranslateService(),
                { provide: MAT_DIALOG_DATA, useValue: { isLoading: signal(false) } },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(LoginComponent);
        component = fixture.componentInstance;
        emitted = [];
        component.login.subscribe((params) => emitted.push(params));
        fixture.detectChanges();
    });

    function fill(selector: string, value: string): void {
        const element: HTMLInputElement = fixture.nativeElement.querySelector(selector);
        element.value = value;
        element.dispatchEvent(new Event('input'));
    }

    function submit(): void {
        fixture.nativeElement.querySelector('button[type="submit"]').click();
        fixture.detectChanges();
    }

    function errorTexts(): string[] {
        return Array.from(fixture.nativeElement.querySelectorAll('mat-error')).map((error) =>
            (error as HTMLElement).textContent!.trim(),
        );
    }

    it('should show no errors before the user interacts with the form', () => {
        expect(errorTexts()).toEqual([]);
    });

    it('should not emit "login" and should show the required errors when submitting an empty form', () => {
        submit();

        expect(emitted).toEqual([]);
        expect(errorTexts()).toEqual([
            'login.username.required-error',
            'login.password.required-error',
        ]);
    });

    it('should emit "login" with the username and password when the form is valid', () => {
        fill('#username', 'joe');
        fill('#password', 'secret');

        submit();

        expect(emitted).toEqual([{ username: 'joe', password: 'secret' }]);
    });
});
