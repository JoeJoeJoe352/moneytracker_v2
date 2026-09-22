import { Component } from "@angular/core";
import { TranslatePipe } from "@ngx-translate/core";

@Component({
    selector: "app-error-component",
    template: '<h1>{{ "error.not_found" | translate }}</h1>',
    standalone: true,
    imports: [TranslatePipe],
})
export class ErrorPageComponent {

}