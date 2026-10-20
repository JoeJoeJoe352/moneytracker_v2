import { DOCUMENT, inject, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SwUpdate, VersionReadyEvent } from '@angular/service-worker';
import { _, TranslateService } from '@ngx-translate/core';
import { filter, interval } from 'rxjs';

/**
 * Ilyen gyakran nézzük meg, hogy van-e új verzió (oldalbetöltéskor a service worker magától is ellenőriz)
 */
export const UPDATE_CHECK_INTERVAL_MS = 60 * 60 * 1000;

/**
 * Service worker frissítések kezelése: ha új verzió érhető el, felajánljuk a usernek az oldal újratöltését
 */
@Injectable({
    providedIn: 'root',
})
export class AppUpdateService {
    private readonly swUpdate = inject(SwUpdate);
    private readonly snackBar = inject(MatSnackBar);
    private readonly translateService = inject(TranslateService);
    private readonly document = inject(DOCUMENT);

    public init(): void {
        // dev módban nincs regisztrálva service worker
        if (!this.swUpdate.isEnabled) {
            return;
        }

        this.swUpdate.versionUpdates
            .pipe(filter((event): event is VersionReadyEvent => event.type === 'VERSION_READY'))
            .subscribe(() => this.promptReload(_('app-update.available')));

        // a cache-elt verzió már nem tölthető be teljesen (pl. a szerverről törölt fájlok miatt), csak újratöltés segít
        this.swUpdate.unrecoverable.subscribe(() =>
            this.promptReload(_('app-update.unrecoverable')),
        );

        // a hosszan nyitva tartott fül is értesüljön az új verzióról; offline hibát nem kell jelezni
        interval(UPDATE_CHECK_INTERVAL_MS).subscribe(() => {
            this.swUpdate.checkForUpdate().catch(() => undefined);
        });
    }

    /**
     * Addig marad kint az üzenet, amíg a user nem reagál rá, hogy ne vesszen el egy félig kitöltött űrlap
     */
    private promptReload(translationKey: string): void {
        this.snackBar
            .open(
                this.translateService.instant(translationKey),
                this.translateService.instant(_('app-update.reload')),
                { duration: 0 },
            )
            .onAction()
            .subscribe(() => this.document.location.reload());
    }
}
