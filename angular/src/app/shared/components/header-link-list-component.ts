import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { LinkInterface } from '../interfaces';
import { TranslatePipe } from '@ngx-translate/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIcon } from '@angular/material/icon';

@Component({
    selector: 'app-header-link-list',
    template: `
        <li>
            <a
                class="nav-link"
                [routerLink]="linkData.url"
                routerLinkActive="active"
                [routerLinkActiveOptions]="{ exact: linkData.url === '/' }"
                (click)="clicked.emit()"
            >
                <mat-icon [fontIcon]="linkData.icon"></mat-icon>
                {{ linkData.langKey | translate }}
            </a>
        </li>
    `,
    imports: [TranslatePipe, RouterLink, RouterLinkActive, MatIcon],
    styles: `
        @use '../variables.scss' as *;

        .nav-link {
            cursor: pointer;
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
export class HeaderLinkListComponent implements OnInit {
    /**
     * Linkek listája, amik jelenjenek meg a fejlécben
     */
    @Input({ required: true }) linkData!: LinkInterface;

    @Output() clicked = new EventEmitter<void>();

    ngOnInit(): void {
        if (this.linkData.action && this.linkData.url) {
            throw new Error('Url and action coextists is forbidden');
        } else if (!this.linkData.action && !this.linkData.url) {
            throw new Error('One of url or actions must exists');
        }
    }
}
