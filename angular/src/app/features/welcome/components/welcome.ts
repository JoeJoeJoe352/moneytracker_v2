import { Component } from "@angular/core";
import { LoginComponent } from "../../auth/components/loginComponent";
import { RegisterComponent } from "../../auth/components/RegisterComponent";

@Component({
    selector: "app-welcome",
    templateUrl: "../pages/welcome.html",
    imports: [LoginComponent, RegisterComponent],
    standalone: true,
})
export class Welcome {}