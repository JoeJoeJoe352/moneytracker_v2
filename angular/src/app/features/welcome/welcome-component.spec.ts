import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { WelcomeComponent } from './welcome-component';
import { AuthDialogService } from '../auth/auth-dialog-service';
import { UserDataStore } from '@app/shared/stores/user-data-store';

describe('WelcomeComponent (Vitest)', () => {
    let fixture: ComponentFixture<WelcomeComponent>;
    let authDialogMock: { openLogin: ReturnType<typeof vi.fn>; openRegister: ReturnType<typeof vi.fn> };
    let isUserLogged: WritableSignal<boolean>;

    beforeEach(async () => {
        authDialogMock = { openLogin: vi.fn(), openRegister: vi.fn() };
        isUserLogged = signal(false);

        await TestBed.configureTestingModule({
            imports: [WelcomeComponent],
            providers: [
                provideTranslateService(),
                provideRouter([]),
                { provide: AuthDialogService, useValue: authDialogMock },
                { provide: UserDataStore, useValue: { isUserLogged } },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(WelcomeComponent);
        fixture.detectChanges();
    });

    function buttonTexts(): string[] {
        return Array.from(
            fixture.nativeElement.querySelectorAll('.cta-buttons button, .cta-buttons a'),
        ).map((element) => (element as HTMLElement).textContent.trim());
    }

    function clickButton(text: string): void {
        const button = Array.from(
            fixture.nativeElement.querySelectorAll('.cta-buttons button'),
        ).find((element) => (element as HTMLElement).textContent.trim() === text);
        (button as HTMLButtonElement).click();
    }

    it('should render a card for every benefit', () => {
        const cardTexts = Array.from(fixture.nativeElement.querySelectorAll('.benefit-card p')).map(
            (element) => (element as HTMLElement).textContent.trim(),
        );

        expect(cardTexts).toEqual([
            'welcome.liquidityTitle',
            'welcome.easier',
            'welcome.decisions',
            'welcome.financial-stress',
            'welcome.faster',
            'welcome.security',
            'welcome.goals',
        ]);
    });

    it('should show the register and login buttons in the hero and the closing section when the user is logged out', () => {
        expect(buttonTexts()).toEqual([
            'welcome.register',
            'welcome.login',
            'welcome.register',
            'welcome.login',
        ]);
    });

    it('should open the register dialog when the register button is clicked', () => {
        clickButton('welcome.register');

        expect(authDialogMock.openRegister).toHaveBeenCalledOnce();
        expect(authDialogMock.openLogin).not.toHaveBeenCalled();
    });

    it('should open the login dialog when the login button is clicked', () => {
        clickButton('welcome.login');

        expect(authDialogMock.openLogin).toHaveBeenCalledOnce();
        expect(authDialogMock.openRegister).not.toHaveBeenCalled();
    });

    it('should show only the dashboard link pointing to the main page when the user is logged in', () => {
        isUserLogged.set(true);
        fixture.detectChanges();

        expect(buttonTexts()).toEqual(['welcome.toDashboard', 'welcome.toDashboard']);
        const links = Array.from(fixture.nativeElement.querySelectorAll('.cta-buttons a'));
        expect(links.map((link) => (link as HTMLAnchorElement).getAttribute('href'))).toEqual([
            '/',
            '/',
        ]);
    });
});
