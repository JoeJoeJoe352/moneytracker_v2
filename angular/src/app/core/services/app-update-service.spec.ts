import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { DOCUMENT } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SwUpdate, VersionEvent } from '@angular/service-worker';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { AppUpdateService, UPDATE_CHECK_INTERVAL_MS } from './app-update-service';

describe('AppUpdateService (Vitest)', () => {
    let service: AppUpdateService;
    let versionUpdates: Subject<VersionEvent>;
    let unrecoverable: Subject<unknown>;
    let snackBarAction: Subject<void>;
    let swUpdateMock: {
        isEnabled: boolean;
        versionUpdates: Subject<VersionEvent>;
        unrecoverable: Subject<unknown>;
        checkForUpdate: ReturnType<typeof vi.fn>;
    };
    let snackBarMock: { open: ReturnType<typeof vi.fn> };
    let reloadMock: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        vi.useFakeTimers();

        versionUpdates = new Subject();
        unrecoverable = new Subject();
        snackBarAction = new Subject();
        swUpdateMock = {
            isEnabled: true,
            versionUpdates,
            unrecoverable,
            checkForUpdate: vi.fn().mockResolvedValue(false),
        };
        snackBarMock = { open: vi.fn().mockReturnValue({ onAction: () => snackBarAction }) };
        reloadMock = vi.fn();

        TestBed.configureTestingModule({
            providers: [
                { provide: SwUpdate, useValue: swUpdateMock },
                { provide: MatSnackBar, useValue: snackBarMock },
                // "lefordítva" adjuk vissza a kulcsot, hogy látszódjon, mi ment át a fordítón
                { provide: TranslateService, useValue: { instant: (key: string) => `t:${key}` } },
                { provide: DOCUMENT, useValue: { location: { reload: reloadMock } } },
            ],
        });

        service = TestBed.inject(AppUpdateService);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('should do nothing when the service worker is disabled', () => {
        swUpdateMock.isEnabled = false;
        service.init();

        versionUpdates.next({ type: 'VERSION_READY' } as VersionEvent);
        vi.advanceTimersByTime(UPDATE_CHECK_INTERVAL_MS);

        expect(snackBarMock.open).not.toHaveBeenCalled();
        expect(swUpdateMock.checkForUpdate).not.toHaveBeenCalled();
    });

    it('should offer a reload when a new version is ready', () => {
        service.init();

        versionUpdates.next({ type: 'VERSION_READY' } as VersionEvent);

        expect(snackBarMock.open).toHaveBeenCalledExactlyOnceWith(
            't:app-update.available',
            't:app-update.reload',
            { duration: 0 },
        );
    });

    it('should ignore the other version events', () => {
        service.init();

        versionUpdates.next({ type: 'VERSION_DETECTED' } as VersionEvent);
        versionUpdates.next({ type: 'NO_NEW_VERSION_DETECTED' } as VersionEvent);

        expect(snackBarMock.open).not.toHaveBeenCalled();
    });

    it('should reload the page when the user clicks the reload button', () => {
        service.init();
        versionUpdates.next({ type: 'VERSION_READY' } as VersionEvent);

        expect(reloadMock).not.toHaveBeenCalled();
        snackBarAction.next();

        expect(reloadMock).toHaveBeenCalledOnce();
    });

    it('should offer a reload when the app state is unrecoverable', () => {
        service.init();

        unrecoverable.next({ type: 'UNRECOVERABLE_STATE', reason: 'test' });

        expect(snackBarMock.open).toHaveBeenCalledExactlyOnceWith(
            't:app-update.unrecoverable',
            't:app-update.reload',
            { duration: 0 },
        );
    });

    it('should check for updates periodically', () => {
        service.init();

        expect(swUpdateMock.checkForUpdate).not.toHaveBeenCalled();
        vi.advanceTimersByTime(UPDATE_CHECK_INTERVAL_MS);
        expect(swUpdateMock.checkForUpdate).toHaveBeenCalledTimes(1);
        vi.advanceTimersByTime(UPDATE_CHECK_INTERVAL_MS);
        expect(swUpdateMock.checkForUpdate).toHaveBeenCalledTimes(2);
    });

    it('should swallow the error of a failed update check', async () => {
        swUpdateMock.checkForUpdate.mockRejectedValue(new Error('offline'));
        service.init();

        await vi.advanceTimersByTimeAsync(UPDATE_CHECK_INTERVAL_MS);

        expect(swUpdateMock.checkForUpdate).toHaveBeenCalledOnce();
    });
});
