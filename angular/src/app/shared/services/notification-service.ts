import { inject, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { _, TranslateService } from '@ngx-translate/core';

/**
 * Snackbar üzenetek megjelenítése a usernek, a "bezár" gombbal együtt
 */
@Injectable({
    providedIn: 'root',
})
export class NotificationService {
    private readonly snackBar = inject(MatSnackBar);
    private readonly translateService = inject(TranslateService);

    /**
     * Megjeleníti a megadott fordítási kulcshoz tartozó üzenetet.
     * a kulcs kerüljön _() függvénybe, hogy automatikus kulcs extractorok megtalálhassák
     */
    public show(translationKey: string): void {
        this.showText(this.translateService.instant(translationKey));
    }

    /**
     * Megjeleníti az üzenetet úgy, ahogy kapjuk (pl. a backendtől már lefordítva érkezett szöveg)
     */
    public showText(message: string): void {
        this.snackBar.open(message, this.translateService.instant(_('etc.close')));
    }

    /**
     * Általános hibaüzenet, amikor nincs a usernek mutatható konkrét ok
     */
    public showGeneralError(): void {
        this.show(_('etc.general-error'));
    }
}
