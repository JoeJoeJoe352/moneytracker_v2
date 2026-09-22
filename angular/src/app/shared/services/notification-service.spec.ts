import { describe, it, expect, vi, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { NotificationService } from './notification-service';

describe('NotificationService (Vitest)', () => {
    let service: NotificationService;
    let snackBarMock: { open: ReturnType<typeof vi.fn> };

    beforeEach(() => {
        snackBarMock = { open: vi.fn() };

        TestBed.configureTestingModule({
            providers: [
                { provide: MatSnackBar, useValue: snackBarMock },
                // "lefordítva" adjuk vissza a kulcsot, hogy látszódjon, mi ment át a fordítón
                { provide: TranslateService, useValue: { instant: (key: string) => `t:${key}` } },
            ],
        });

        service = TestBed.inject(NotificationService);
    });

    it('should translate the given key and show it with a translated close label', () => {
        service.show('login.success');

        expect(snackBarMock.open).toHaveBeenCalledExactlyOnceWith('t:login.success', 't:etc.close');
    });

    it('should show the given text as it is, without translating it', () => {
        service.showText('Invalid credentials');

        expect(snackBarMock.open).toHaveBeenCalledExactlyOnceWith('Invalid credentials', 't:etc.close');
    });

    it('should show the translated general error message', () => {
        service.showGeneralError();

        expect(snackBarMock.open).toHaveBeenCalledExactlyOnceWith('t:etc.general-error', 't:etc.close');
    });
});
