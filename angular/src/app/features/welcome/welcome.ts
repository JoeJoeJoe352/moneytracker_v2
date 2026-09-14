import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';

@Component({
    selector: 'app-welcome',
    templateUrl: './welcome.html',
    styles: `
        .logo {
            max-height: 450px;
            max-width: stretch;
        }
    `,
    imports: [TranslatePipe, MatCardModule, MatButtonModule],
    standalone: true,
})
export class Welcome {}
