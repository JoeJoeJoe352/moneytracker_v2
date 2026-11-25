import { Component, inject } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { _, TranslatePipe } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { AuthDialogService } from '../auth/auth-dialog-service';
import { UserDataStore } from '@app/shared/stores/user-data-store';

/**
 * Egy előny kártya adatai a landing oldalon
 */
interface BenefitInterface {
    icon: string;
    langKey: string;
}

@Component({
    selector: 'app-welcome',
    templateUrl: './welcome-component.html',
    styleUrl: './welcome-component.scss',
    imports: [TranslatePipe, MatButtonModule, MatIcon, RouterLink, NgTemplateOutlet],
})
export class WelcomeComponent {
    private readonly authDialog = inject(AuthDialogService);

    protected readonly userData = inject(UserDataStore);

    /**
     * Előnyök listája ikonokkal
     */
    protected readonly benefits: BenefitInterface[] = [
        { icon: 'visibility', langKey: _('welcome.liquidityTitle') },
        { icon: 'search', langKey: _('welcome.easier') },
        { icon: 'insights', langKey: _('welcome.decisions') },
        { icon: 'self_improvement', langKey: _('welcome.financial-stress') },
        { icon: 'savings', langKey: _('welcome.faster') },
        { icon: 'shield', langKey: _('welcome.security') },
        { icon: 'flag', langKey: _('welcome.goals') },
    ];

    protected openLogin(): void {
        this.authDialog.openLogin();
    }

    protected openRegister(): void {
        this.authDialog.openRegister();
    }
}
