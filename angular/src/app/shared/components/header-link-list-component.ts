import { Component, Input } from '@angular/core';
import { LinkInterface } from '../interfaces';
import { TranslatePipe } from '@ngx-translate/core';
import { RouterLink } from '@angular/router';

@Component({
    selector: 'app-header-link-list',
    template: `
        @for (linkItem of this.linkList; track $index) {
            <li class="nav-item">
                <a class="nav-link" [routerLink]="linkItem.url">{{
                    linkItem.langKey | translate
                }}</a>
            </li>
        }
    `,
    imports: [TranslatePipe, RouterLink],
    styles: `
        @use '../variables.scss' as *;
        .nav-link {
            cursor: pointer;
            color: $moneytracker-white;
        }
    `,
})
export class HeaderLinkListComponent {
    /**
     * Linkek listája, amik jelenjenek meg a fejlécben
     */
    @Input({ required: true }) linkList!: LinkInterface[];
}
