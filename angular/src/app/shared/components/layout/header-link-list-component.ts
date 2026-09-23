import { Component, input, OnInit, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { LinkInterface } from './interfaces';

@Component({
    selector: 'app-header-link-list',
    templateUrl: './header-link-list-component.html',
    styleUrl: './header-link-list-component.scss',
    imports: [TranslatePipe, RouterLink, RouterLinkActive, MatIcon],
})
export class HeaderLinkListComponent implements OnInit {
    /**
     * Linkek listája, amik jelenjenek meg a fejlécben
     */
    linkData = input.required<LinkInterface>();

    /**
     * rákattintott a user az egyik elemre
     */
    clicked = output<void>();

    ngOnInit(): void {
        if (this.linkData().action && this.linkData().url) {
            throw new Error('Url and action coextists is forbidden');
        } else if (!this.linkData().action && !this.linkData().url) {
            throw new Error('One of url or actions must exists');
        }
    }
}
