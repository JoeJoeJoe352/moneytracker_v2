import { Component, Input } from '@angular/core';
import { LinkInterface } from '../interfaces';
import { TranslatePipe } from '@ngx-translate/core';
import { RouterLink } from '@angular/router';
import { MatButton } from '@angular/material/button';

@Component({
    selector: 'app-header-link-list',
    template: `
        @for (linkItem of this.linkList; track $index) {
            <li>
                <a matButton="text" class="header-link" [routerLink]="linkItem.url">{{
                    linkItem.langKey | translate
                }}</a>
            </li>
        }
    `,
    imports: [TranslatePipe, RouterLink, MatButton],
    styles: `
        @use '../variables.scss' as *;
        .header-link {
            --mat-button-text-label-text-color: #{$moneytracker-white};
            --mat-button-text-state-layer-color: #{$moneytracker-white};
        }
    `,
})
export class HeaderLinkListComponent {
    /**
     * Linkek listája, amik jelenjenek meg a fejlécben
     */
    @Input({ required: true }) linkList!: LinkInterface[];
}
