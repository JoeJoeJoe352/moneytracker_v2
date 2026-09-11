import { Component, Input } from '@angular/core';
import { LinkInterface } from '../interfaces';
import { TranslatePipe } from '@ngx-translate/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIcon } from '@angular/material/icon';

@Component({
    selector: 'app-header-link-list',
    template: `
        @for (linkItem of this.linkList; track $index) {
            <li>
                <a
                    class="nav-link"
                    [routerLink]="linkItem.url"
                    routerLinkActive="active"
                    [routerLinkActiveOptions]="{ exact: linkItem.url === '/' }"
                >
                    <mat-icon [fontIcon]="linkItem.icon"></mat-icon>
                    {{ linkItem.langKey | translate }}
                </a>
            </li>
        }
    `,
    imports: [TranslatePipe, RouterLink, RouterLinkActive, MatIcon],
    styles: `
        @use '../variables.scss' as *;

        .nav-link {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 0.65rem 1rem;
            border-radius: $border-radius-lg;
            color: $moneytracker-white;
            text-decoration: none;
            font-weight: 500;
            transition: $transition-base;
            margin-bottom: 6px;

            &:hover:not(.active) {
                background: $moneytracker-darker-green;
            }

            &.active {
                background: $moneytracker-darker-green;
                color: $moneytracker-white;
            }
        }
    `,
})
export class HeaderLinkListComponent {
    /**
     * Linkek listája, amik jelenjenek meg a fejlécben
     */
    @Input({ required: true }) linkList!: LinkInterface[];
}
