import { AfterViewInit, Component, ElementRef, inject, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'app-error-component',
    templateUrl: './error-page-component.html',
    styleUrl: './error-page-component.scss',
    imports: [TranslatePipe, MatCardModule, MatButtonModule],
})
export class ErrorPageComponent implements AfterViewInit {
    private readonly router = inject(Router);
    private readonly backToHomepageButton = viewChild.required('mainPageButton', {
        read: ElementRef<HTMLButtonElement>,
    });

    ngAfterViewInit(): void {
        this.backToHomepageButton().nativeElement.focus();
    }

    protected redirectToMainPage() {
        return this.router.navigate(['/']);
    }
}
